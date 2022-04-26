package net.dries007.tfc.world.stream;

import java.util.List;
import java.util.Stack;

/**
 * This is a temporary holding object for uncommitted sections of river structures
 * In order to be committed, a branch must have a valid source piece at it's end
 * If such a piece is unable to be found, the branch is rejected, discarding all potential pieces it has generated
 */
public class StreamBranch implements IStreamStructure
{
    private final StreamPiece joinPiece;
    private final Stack<StreamPiece> pieces;

    public StreamBranch(StreamPiece joinPiece)
    {
        this.joinPiece = joinPiece;
        this.pieces = new Stack<>();
        this.pieces.push(joinPiece);
    }

    public void push(StreamPiece piece)
    {
        this.pieces.push(piece);
    }

    public void pop()
    {
        this.pieces.pop();
    }

    public StreamPiece getJoinPiece()
    {
        return joinPiece;
    }

    @Override
    public List<StreamPiece> getPieces()
    {
        return pieces;
    }
}
