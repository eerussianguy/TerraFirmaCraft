package net.dries007.tfc.world.stream;

import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;

import net.dries007.tfc.util.Helpers;


public abstract class AbstractStreamGenerator
{
    public static final int RIVER_CHUNK_RADIUS = 16; // The maximum bounding box of a river generation
    public static final float RIVER_START_WEIGHT = 1f;
    public static final float BRANCH_CHANCE = 0.7f;
    public static final int DRAIN_RIVER_WIDTH = 20; // The width of each river at the drain element (plus or minus a range)
    public static final int SOURCE_CUTOFF_WIDTH = 8; // The width at which a branch must end with a source element
    public static final int SOURCE_MAX_WIDTH = 12; // The width at which a branch *may* end with a source element
    public static final int BRANCH_DECREASE_WIDTH = 2; // How much branch with decreases relative to the main branch

    private static final Logger LOGGER = LogManager.getLogger();

    protected final Map<ChunkPos, IStreamStructure> generatedRiverStructures; // Positions and rivers that have been generated, cached here for efficiency
    protected final Random random;
    protected final long seed;

    public AbstractStreamGenerator(long seed)
    {
        this.seed = seed;
        this.generatedRiverStructures = new HashMap<>();
        this.random = new Random();
    }

    /**
     * Generates rivers starting at a given chunk
     * This method is not completely deterministic - it can be influenced by the order chunks are explored, since
     * it takes into account existing generated rivers and avoids intersections
     *
     * @param posAt The position of the origin of the river.
     * @return the river that was generated, or null if not
     */
    @Nullable
    public IStreamStructure generateRiverAtChunk(ChunkPos posAt)
    {
        random.setSeed(Helpers.hash(seed, posAt.x, 0, posAt.z));

        // todo weighting
        // noinspection ConstantConditions
        if (!(random.nextFloat() > RIVER_START_WEIGHT))
        {
            // River structure
            StreamStructure river = new StreamStructure(posAt.getMinBlockX(), posAt.getMinBlockZ());
            List<StreamBranch> branches = new ArrayList<>(); // track possible branches
            List<StreamBranch> potentialBranches = new ArrayList<>(); // potential branches off an uncommitted river branch

            // Drain piece
            StreamPiece drainPiece = generateDrainPiece(posAt);
            if (drainPiece != null)
            {
                // Start with the main branch, iterate through adding all branches
                branches.add(new StreamBranch(drainPiece));
                boolean validBranchesAdded = false;
                while (!branches.isEmpty())
                {
                    // Generate a branch to completion, or to end
                    StreamBranch branch = branches.remove(0);
                    potentialBranches.clear();
                    if (generateBranch(river, branch, potentialBranches))
                    {
                        river.addBranch(branch);
                        branches.addAll(potentialBranches);
                        validBranchesAdded = true;
                    }
                }

                if (validBranchesAdded)
                {
                    LOGGER.info("Generated river at {}", posAt);
                    river.markGenerated();
                    generatedRiverStructures.put(posAt, river);
                    return river;
                }
            }
        }
        return null;
    }

    /**
     * Tries to generate a valid river starting point
     * @param posAt The chunk pos of the river start
     * @return A template for the river start if valid, null if not
     */
    @Nullable
    protected StreamPiece generateDrainPiece(ChunkPos posAt)
    {
        BlockPos pos = findValidDrainPos(posAt);
        if (pos != null)
        {
            List<StreamTemplate> drainTemplates = StreamTemplate.DRAIN_TEMPLATES.get(Direction.Plane.HORIZONTAL.getRandomDirection(random));
            StreamTemplate drainTemplate = drainTemplates.get(random.nextInt(drainTemplates.size()));
            StreamPiece drainPiece = new StreamPiece(drainTemplate, pos.getX() - drainTemplate.getDownstreamX(), pos.getZ() - drainTemplate.getDownstreamZ(), DRAIN_RIVER_WIDTH, getSeaLevel());

            // Check that drain does not intersect other rivers
            for (IStreamStructure otherRiver : generatedRiverStructures.values())
            {
                if (otherRiver.intersectsBox(drainPiece.getBox()))
                {
                    return null;
                }
            }
            if (isValidPiece(drainPiece))
            {
                return drainPiece;
            }
        }
        return null;
    }

    @Nullable
    protected abstract BlockPos findValidDrainPos(ChunkPos pos);

    /**
     * Generates a single river branch
     * Returns if the branch is valid once generated
     */
    protected boolean generateBranch(StreamStructure river, StreamBranch branch, List<StreamBranch> otherBranches)
    {
        StreamPiece downstreamPiece = branch.getJoinPiece();
        while (downstreamPiece.getWidth() > SOURCE_CUTOFF_WIDTH)
        {
            int nextWidth = downstreamPiece.getWidth() - 1;
            if (nextWidth == SOURCE_CUTOFF_WIDTH)
            {
                // Must generate a source piece
                StreamPiece sourcePiece = generateSourcePiece(river, branch, downstreamPiece, SOURCE_CUTOFF_WIDTH);
                if (sourcePiece != null && !branch.intersectsBoxIgnoringPiece(sourcePiece.getBox(), downstreamPiece))
                {
                    // Valid source piece
                    branch.push(sourcePiece);
                    return true;
                }
                // Can't generate a source piece, so instead try and replace the previous piece with a source instead
                return replaceLastPieceWithSource(river, branch, downstreamPiece);
            }
            else
            {
                // Generate possible straight and branch pieces
                StreamPiece straightPiece = generateStraightPiece(river, branch, downstreamPiece, nextWidth);
                if (straightPiece != null)
                {

                    // Optionally, generate a tributary
                    if (random.nextFloat() < BRANCH_CHANCE)
                    {
                        StreamPiece branchPiece = generateBranchPiece(river, branch, downstreamPiece, nextWidth - BRANCH_DECREASE_WIDTH, straightPiece.getUpstreamDirection());
                        if (branchPiece != null && branchPiece.getTemplate() != straightPiece.getTemplate() && branchPiece.getUpstreamDirection() != straightPiece.getUpstreamDirection())
                        {
                            // Branch is valid: can't be the same piece as the normal, or have the same direction upstream
                            StreamBranch newBranch = new StreamBranch(branchPiece);
                            otherBranches.add(newBranch);
                        }
                    }

                    branch.push(straightPiece);
                    downstreamPiece = straightPiece;
                }
                else
                {
                    // this piece doesn't fit for some reason - try and turn this branch into a source by replacing the last piece (which was valid)
                    return replaceLastPieceWithSource(river, branch, downstreamPiece);
                }
            }
        }
        return false;
    }

    protected abstract int getSeaLevel();

    /**
     * When a river branch fails to generate next pieces, it tries to replace the last added piece with a source piece
     * Returns true if it succeeds and the river has a valid source piece
     */
    protected boolean replaceLastPieceWithSource(StreamStructure river, StreamBranch branch, StreamPiece downstreamPiece)
    {
        // piece to be replaced must be at most the max width for a source piece
        if (downstreamPiece.getWidth() <= SOURCE_MAX_WIDTH && downstreamPiece.getDownstream() != null && branch.getPieces().size() > 1)
        {
            StreamPiece sourcePiece = generateSourcePiece(river, branch, downstreamPiece.getDownstream(), downstreamPiece.getWidth());
            if (sourcePiece != null)
            {
                // Source is valid, so replace the previous piece
                branch.pop();
                branch.push(sourcePiece);
                return true;
            }
        }
        return false;
    }

    @Nullable
    protected StreamPiece generateBranchPiece(StreamStructure river, StreamBranch branch, StreamPiece previousPiece, int riverWidth, Direction excludedDirection)
    {
        return generatePiece(river, branch, previousPiece, riverWidth, StreamTemplate.CONNECTOR_TEMPLATES.get(previousPiece.getUpstreamDirection()).stream().filter(template -> template.getUpstreamDirection() != excludedDirection).collect(Collectors.toList()));
    }

    @Nullable
    protected StreamPiece generateStraightPiece(StreamStructure river, StreamBranch branch, StreamPiece previousPiece, int riverWidth)
    {
        return generatePiece(river, branch, previousPiece, riverWidth, StreamTemplate.CONNECTOR_TEMPLATES.get(previousPiece.getUpstreamDirection()));
    }

    @Nullable
    protected StreamPiece generateSourcePiece(StreamStructure river, StreamBranch branch, StreamPiece previousPiece, int riverWidth)
    {
        return generatePiece(river, branch, previousPiece, riverWidth, StreamTemplate.SOURCE_TEMPLATES.get(previousPiece.getUpstreamDirection()));
    }

    @Nullable
    protected StreamPiece generatePiece(StreamStructure river, StreamBranch branch, StreamPiece previousPiece, int riverWidth, List<StreamTemplate> templates)
    {
        StreamTemplate template = templates.get(random.nextInt(templates.size()));
        StreamPiece piece = new StreamPiece(template, previousPiece, riverWidth, previousPiece.getHeight() + 1);

        // check bounding box doesn't intersect this river, and the branch doesn't intersect itself, and that the piece is withing the generation range for the river
        if (!river.intersectsBoxIgnoringPiece(piece.getBox(), previousPiece) && !branch.intersectsBoxIgnoringPiece(piece.getBox(), previousPiece) && piece.getBox().containedIn(river.getBoundingBox()))
        {
            // Check all other possible river intersections that have been generated
            for (IStreamStructure otherRiver : generatedRiverStructures.values())
            {
                if (otherRiver.intersectsBox(piece.getBox()))
                {
                    return null;
                }
            }

            if (isValidPiece(piece))
            {
                return piece;
            }
        }
        // Unable to find a matching piece
        return null;
    }

    protected abstract boolean isValidPiece(StreamPiece piece);
}