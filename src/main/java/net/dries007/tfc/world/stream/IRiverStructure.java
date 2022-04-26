package net.dries007.tfc.world.stream;

import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * Anything that holds multiple river pieces at a time
 */
public interface IRiverStructure
{
    List<RiverPiece> getPieces();

    default boolean intersectsBox(XZRange box)
    {
        return intersectsBoxIgnoringPiece(box, null);
    }

    default boolean intersectsBoxIgnoringPiece(XZRange box, @Nullable RiverPiece pieceToIgnore)
    {
        for (RiverPiece piece : getPieces())
        {
            if (piece != pieceToIgnore && piece.getBox().intersects(box))
            {
                return true;
            }
        }
        return false;
    }
}
