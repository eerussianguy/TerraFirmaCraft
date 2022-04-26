package net.dries007.tfc.world.stream;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public class RiverStructure implements IRiverStructure
{
    public static final int RADIUS = (AbstractRiverGenerator.RIVER_CHUNK_RADIUS - 1) * 16;

    final List<RiverPiece> pieces;
    private final XZRange box;
    private final List<XZRange> piecesBoundingBoxes;

    public RiverStructure(int x, int z)
    {
        this.pieces = new ArrayList<>();
        this.piecesBoundingBoxes = new ArrayList<>();

        this.box = new XZRange(x - RADIUS, z - RADIUS, 2 * RADIUS, 2 * RADIUS);
    }

    public RiverStructure(CompoundTag nbt)
    {
        this.pieces = new ArrayList<>();
        this.piecesBoundingBoxes = new ArrayList<>();

        ListTag list = nbt.getList("pieces", Tag.TAG_COMPOUND);
        RiverPiece downstream = null;
        for (int i = 0; i < list.size(); i++)
        {
            RiverPiece current = new RiverPiece(list.getCompound(i), downstream);
            add(current);
            downstream = current;
        }

        int x = nbt.getInt("x");
        int z = nbt.getInt("z");
        this.box = new XZRange(x - RADIUS, z - RADIUS, 2 * RADIUS, 2 * RADIUS);
    }

    public void add(RiverPiece piece)
    {
        pieces.add(piece);
        piecesBoundingBoxes.add(piece.getBox());
    }

    public void addBranch(RiverBranch branch)
    {
        // Add all pieces from the branch
        for (RiverPiece piece : branch.getPieces())
        {
            add(piece);
        }
    }

    public XZRange getBoundingBox()
    {
        return box;
    }

    @Override
    public List<RiverPiece> getPieces()
    {
        return pieces;
    }

    @Override
    public boolean intersectsBox(XZRange box)
    {
        // Don't directly query the pieces, query the boxes instead as after the river is generated they are larger
        for (XZRange pieceBox : piecesBoundingBoxes)
        {
            if (pieceBox.intersects(box))
            {
                return true;
            }
        }
        return false;
    }

    public void markGenerated()
    {
        // This tells the river that it, in its entirety, has now been "generated".
        // In order to avoid conflicts with other rivers, we expand the bounding box by 16 blocks in all directions
        piecesBoundingBoxes.clear();
        for (RiverPiece piece : pieces)
        {
            piecesBoundingBoxes.add(piece.getBox().expand(16));
        }
    }

    public CompoundTag serializeNBT()
    {
        CompoundTag nbt = new CompoundTag();
        ListTag listNbt = new ListTag();
        for (RiverPiece piece : pieces)
        {
            listNbt.add(piece.serializeNBT());
        }
        nbt.put("pieces", listNbt);
        return nbt;
    }
}
