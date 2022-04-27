/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.world.stream;

import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkStatus;

import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.world.TFCChunkGenerator;
import net.dries007.tfc.world.chunkdata.ChunkData;


public abstract class AbstractStreamGenerator
{
    public static final int STREAM_CHUNK_RADIUS = ChunkStatus.MAX_STRUCTURE_DISTANCE; // The maximum bounding box of a stream generation
    public static final float STREAM_START_WEIGHT = 0.5f;
    public static final float BRANCH_CHANCE = 0.7f;
    public static final int DRAIN_STREAM_WIDTH = 20; // The width of each stream at the drain element (plus or minus a range)
    public static final int SOURCE_CUTOFF_WIDTH = 8; // The width at which a branch must end with a source element
    public static final int SOURCE_MAX_WIDTH = 12; // The width at which a branch *may* end with a source element
    public static final int BRANCH_DECREASE_WIDTH = 2; // How much branch with decreases relative to the main branch

    private static final Logger LOGGER = LogManager.getLogger();

    protected final Map<ChunkPos, StreamStructure> generatedStreamStructures; // Positions and streams that have been generated, cached here for efficiency
    protected final Set<ChunkPos> failedStreamStructures; // Positions and streams that have been generated, cached here for efficiency
    protected final Random random;
    protected final long seed;

    public AbstractStreamGenerator(long seed)
    {
        this.seed = seed;
        this.generatedStreamStructures = new HashMap<>();
        this.failedStreamStructures = new HashSet<>();
        this.random = new Random();
    }

    /**
     * Generates streams starting at a given chunk
     * This method is not completely deterministic - it can be influenced by the order chunks are explored, since
     * it takes into account existing generated streams and avoids intersections
     *
     * @param level the level
     * @param posAt The position of the origin of the stream.
     * @param biomes the local biome array, 16x16
     * @return the stream that was generated, or null if not
     */
    @Nullable
    public StreamStructure generateStreamAtChunk(LevelAccessor level, ChunkPos posAt, Biome[] biomes, int[] heights)
    {
        random.setSeed(Helpers.hash(seed, posAt.x, 0, posAt.z));

        if (!(random.nextFloat() > STREAM_START_WEIGHT))
        {
            // stream structure
            StreamStructure stream = new StreamStructure(posAt.getMinBlockX(), posAt.getMinBlockZ());
            List<StreamBranch> branches = new ArrayList<>(); // track possible branches
            List<StreamBranch> potentialBranches = new ArrayList<>(); // potential branches off an uncommitted stream branch

            // Drain piece
            StreamPiece drainPiece = generateDrainPiece(level, posAt, biomes, heights);
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
                    if (generateBranch(stream, branch, potentialBranches, heights))
                    {
                        stream.addBranch(branch);
                        branches.addAll(potentialBranches);
                        validBranchesAdded = true;
                    }
                }

                if (validBranchesAdded)
                {
                    LOGGER.info("Generated stream at {}", posAt);
                    stream.markGenerated();
                    generatedStreamStructures.put(posAt, stream);
                    return stream;
                }
            }
        }
        return null;
    }

    public List<StreamStructure> generateStreamsAroundChunk(LevelAccessor level, ChunkPos chunkPos, Biome[] biomes, ChunkData data)
    {
        List<StreamStructure> streams = new ArrayList<>();
        final int range = STREAM_CHUNK_RADIUS;
        for (int x = chunkPos.x - range; x <= chunkPos.x + range; x++)
        {
            for (int z = chunkPos.z - range; z <= chunkPos.z + range; z++)
            {
                ChunkPos posAt = new ChunkPos(x, z);
                if (generatedStreamStructures.containsKey(posAt))
                {
                    streams.add(generatedStreamStructures.get(posAt));
                }
                else if (!failedStreamStructures.contains(posAt))
                {
                    StreamStructure stream = generateStreamAtChunk(level, posAt, biomes, data.getTerrainSurfaceHeight()[(x - chunkPos.x + range) + 17 * (z - chunkPos.z + range)]);
                    if (stream != null)
                    {
                        streams.add(stream);
                    }
                    else
                    {
                        failedStreamStructures.add(posAt);
                    }
                }
            }
        }
        return streams;
    }

    /**
     * Tries to generate a valid stream starting point
     * @param level the level
     * @param posAt The chunk pos of the stream start
     * @param biomes The local biome array, 16x16
     * @param heights the local height map of the area
     * @return A template for the stream start if valid, null if not
     */
    @Nullable
    protected StreamPiece generateDrainPiece(LevelAccessor level, ChunkPos posAt, Biome[] biomes, int[] heights)
    {
        BlockPos pos = findValidDrainPos(level, posAt, biomes);
        if (pos != null)
        {
            List<StreamTemplate> drainTemplates = StreamTemplate.DRAIN_TEMPLATES.get(Direction.Plane.HORIZONTAL.getRandomDirection(random));
            StreamTemplate drainTemplate = drainTemplates.get(random.nextInt(drainTemplates.size()));
            StreamPiece drainPiece = new StreamPiece(drainTemplate, pos.getX() - drainTemplate.getDownstreamX(), pos.getZ() - drainTemplate.getDownstreamZ(), DRAIN_STREAM_WIDTH, getSeaLevel(), TFCChunkGenerator.fuzzyMax(heights));

            // Check that drain does not intersect other streams
            for (StreamStructure other : generatedStreamStructures.values())
            {
                if (other.intersectsBox(drainPiece.getBox()))
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
    protected abstract BlockPos findValidDrainPos(LevelAccessor level, ChunkPos pos, Biome[] biomes);

    /**
     * Generates a single stream branch
     * Returns if the branch is valid once generated
     */
    protected boolean generateBranch(StreamStructure stream, StreamBranch branch, List<StreamBranch> otherBranches, int[] heights)
    {
        StreamPiece downstreamPiece = branch.getJoinPiece();
        while (downstreamPiece.getWidth() > SOURCE_CUTOFF_WIDTH)
        {
            int nextWidth = downstreamPiece.getWidth() - 1;
            if (nextWidth == SOURCE_CUTOFF_WIDTH)
            {
                // Must generate a source piece
                StreamPiece sourcePiece = generateSourcePiece(stream, branch, downstreamPiece, SOURCE_CUTOFF_WIDTH, TFCChunkGenerator.fuzzyMax(heights));
                if (sourcePiece != null && !branch.intersectsBoxIgnoringPiece(sourcePiece.getBox(), downstreamPiece) && branch.getMaxSurfaceHeight() <= sourcePiece.getSurfaceHeight())
                {
                    // Valid source piece
                    branch.push(sourcePiece);
                    return true;
                }
                // Can't generate a source piece, so instead try and replace the previous piece with a source instead
                return replaceLastPieceWithSource(stream, branch, downstreamPiece);
            }
            else
            {
                // Generate possible straight and branch pieces
                StreamPiece straightPiece = generateStraightPiece(stream, branch, downstreamPiece, nextWidth, TFCChunkGenerator.fuzzyMax(heights));
                if (straightPiece != null && branch.getMaxSurfaceHeight() <= straightPiece.getSurfaceHeight())
                {

                    // Optionally, generate a tributary
                    if (random.nextFloat() < BRANCH_CHANCE)
                    {
                        StreamPiece branchPiece = generateBranchPiece(stream, branch, downstreamPiece, nextWidth - BRANCH_DECREASE_WIDTH, straightPiece.getUpstreamDirection(), TFCChunkGenerator.fuzzyMax(heights));
                        if (branchPiece != null && branchPiece.getTemplate() != straightPiece.getTemplate() && branchPiece.getUpstreamDirection() != straightPiece.getUpstreamDirection() && straightPiece.getSurfaceHeight() <= branchPiece.getSurfaceHeight())
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
                    return replaceLastPieceWithSource(stream, branch, downstreamPiece);
                }
            }
        }
        return false;
    }

    protected abstract int getSeaLevel();

    /**
     * When a stream branch fails to generate next pieces, it tries to replace the last added piece with a source piece
     * Returns true if it succeeds and the stream has a valid source piece
     */
    protected boolean replaceLastPieceWithSource(StreamStructure stream, StreamBranch branch, StreamPiece downstreamPiece)
    {
        // piece to be replaced must be at most the max width for a source piece
        if (downstreamPiece.getWidth() <= SOURCE_MAX_WIDTH && downstreamPiece.getDownstream() != null && branch.getPieces().size() > 1)
        {
            StreamPiece sourcePiece = generateSourcePiece(stream, branch, downstreamPiece.getDownstream(), downstreamPiece.getWidth(), downstreamPiece.getSurfaceHeight());
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
    protected StreamPiece generateBranchPiece(StreamStructure stream, StreamBranch branch, StreamPiece previousPiece, int streamWidth, Direction excludedDirection, int surfaceHeight)
    {
        return generatePiece(stream, branch, previousPiece, streamWidth, StreamTemplate.CONNECTOR_TEMPLATES.get(previousPiece.getUpstreamDirection()).stream().filter(template -> template.getUpstreamDirection() != excludedDirection).collect(Collectors.toList()), surfaceHeight);
    }

    @Nullable
    protected StreamPiece generateStraightPiece(StreamStructure stream, StreamBranch branch, StreamPiece previousPiece, int streamWidth, int surfaceHeight)
    {
        return generatePiece(stream, branch, previousPiece, streamWidth, StreamTemplate.CONNECTOR_TEMPLATES.get(previousPiece.getUpstreamDirection()), surfaceHeight);
    }

    @Nullable
    protected StreamPiece generateSourcePiece(StreamStructure stream, StreamBranch branch, StreamPiece previousPiece, int streamWidth, int surfaceHeight)
    {
        return generatePiece(stream, branch, previousPiece, streamWidth, StreamTemplate.SOURCE_TEMPLATES.get(previousPiece.getUpstreamDirection()), surfaceHeight);
    }

    @Nullable
    protected StreamPiece generatePiece(StreamStructure stream, StreamBranch branch, StreamPiece previousPiece, int streamWidth, List<StreamTemplate> templates, int surfaceHeight)
    {
        StreamTemplate template = templates.get(random.nextInt(templates.size()));
        StreamPiece piece = new StreamPiece(template, previousPiece, streamWidth, previousPiece.getHeight() + 1, surfaceHeight);

        // check bounding box doesn't intersect this stream, and the branch doesn't intersect itself, and that the piece is withing the generation range for the stream
        if (!stream.intersectsBoxIgnoringPiece(piece.getBox(), previousPiece) && !branch.intersectsBoxIgnoringPiece(piece.getBox(), previousPiece) && piece.getBox().containedIn(stream.getBoundingBox()))
        {
            // Check all other possible stream intersections that have been generated
            for (StreamStructure otherStream : generatedStreamStructures.values())
            {
                if (otherStream.intersectsBox(piece.getBox()))
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