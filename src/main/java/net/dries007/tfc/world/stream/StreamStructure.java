/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.world.stream;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public class StreamStructure implements IStreamStructure
{
    public static final int RADIUS = (AbstractStreamGenerator.STREAM_CHUNK_RADIUS - 1) * 16;

    final List<StreamPiece> pieces;
    private final XZRange box;
    private final List<XZRange> piecesBoundingBoxes;

    public StreamStructure(int x, int z)
    {
        this.pieces = new ArrayList<>();
        this.piecesBoundingBoxes = new ArrayList<>();

        this.box = new XZRange(x - RADIUS, z - RADIUS, 2 * RADIUS, 2 * RADIUS);
    }

    public StreamStructure(CompoundTag nbt)
    {
        this.pieces = new ArrayList<>();
        this.piecesBoundingBoxes = new ArrayList<>();

        ListTag list = nbt.getList("pieces", Tag.TAG_COMPOUND);
        StreamPiece downstream = null;
        for (int i = 0; i < list.size(); i++)
        {
            StreamPiece current = new StreamPiece(list.getCompound(i), downstream);
            add(current);
            downstream = current;
        }

        int x = nbt.getInt("x");
        int z = nbt.getInt("z");
        this.box = new XZRange(x - RADIUS, z - RADIUS, 2 * RADIUS, 2 * RADIUS);
    }

    public void add(StreamPiece piece)
    {
        pieces.add(piece);
        piecesBoundingBoxes.add(piece.getBox());
    }

    public void addBranch(StreamBranch branch)
    {
        // Add all pieces from the branch
        for (StreamPiece piece : branch.getPieces())
        {
            add(piece);
        }
    }

    public XZRange getBoundingBox()
    {
        return box;
    }

    @Override
    public List<StreamPiece> getPieces()
    {
        return pieces;
    }

    @Override
    public boolean intersectsBox(XZRange box)
    {
        // Don't directly query the pieces, query the boxes instead as after the stream is generated they are larger
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
        // This tells the stream that it, in its entirety, has now been "generated".
        // In order to avoid conflicts with other streams, we expand the bounding box by 16 blocks in all directions
        piecesBoundingBoxes.clear();
        for (StreamPiece piece : pieces)
        {
            piecesBoundingBoxes.add(piece.getBox().expand(16));
        }
    }

    public CompoundTag serializeNBT()
    {
        CompoundTag nbt = new CompoundTag();
        ListTag listNbt = new ListTag();
        for (StreamPiece piece : pieces)
        {
            listNbt.add(piece.serializeNBT());
        }
        nbt.put("pieces", listNbt);
        return nbt;
    }
}
