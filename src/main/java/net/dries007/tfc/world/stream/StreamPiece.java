/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.world.stream;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.world.TFCChunkGenerator;
import net.dries007.tfc.world.river.Flow;
import org.jetbrains.annotations.Nullable;

public class StreamPiece
{
    public final float upstreamX;
    public final float upstreamZ;
    public final float downstreamX;
    public final float downstreamZ;
    private final StreamTemplate template;
    private final int width;
    private final int height;
    private final Flow[] flows;
    private final Direction upstreamDirection;
    private final Direction downstreamDirection;
    private final XZRange box;
    private final StreamPiece downstreamPiece;
    private final float x;
    private final float z;
    private final int[] surfaceHeight;
    private final int surfaceMaxHeight;

    public StreamPiece(StreamTemplate template, float x, float z, int width, int height, int[] surfaceHeight)
    {
        this(template, null, width, height, x, z, surfaceHeight);
    }

    public StreamPiece(StreamTemplate template, StreamPiece downstreamPiece, int width, int height, int[] surfaceHeight)
    {
        this(template, downstreamPiece, width, height, 0, 0, surfaceHeight);
    }

    public StreamPiece(CompoundTag nbt, @Nullable StreamPiece downstreamPiece)
    {
        this(StreamTemplate.get(nbt.getInt("template")), downstreamPiece, nbt.getInt("width"), nbt.getInt("height"), nbt.getFloat("x"), nbt.getFloat("z"), nbt.getIntArray("surfaceHeights"));
    }

    private StreamPiece(StreamTemplate template, @Nullable StreamPiece downstreamPiece, int width, int height, float xIn, float zIn, int[] surfaceHeight)
    {
        this.template = template;
        this.flows = new Flow[width * width];
        this.width = width;
        this.height = height;
        this.surfaceHeight = surfaceHeight;
        this.surfaceMaxHeight = Helpers.max(surfaceHeight);

        if (width == StreamTemplate.SIZE)
        {
            // skip the complex flow copy, just use the existing array
            System.arraycopy(template.getFlows(), 0, flows, 0, flows.length);
        }
        else
        {
            // Create the flow plan for this piece by scaling the template outwards
            // This scale factor is so the template flows are stretched to match at the edges
            float flowScaleFactor = (float) (StreamTemplate.SIZE - 1) / (width - 1);
            for (int ix = 0; ix < width; ix++)
            {
                for (int iz = 0; iz < width; iz++)
                {
                    float px = ix * flowScaleFactor;
                    float pz = iz * flowScaleFactor;
                    // Check edge cases
                    if (ix == 0 || iz == 0 || ix == width - 1 || iz == width - 1)
                    {
                        flows[ix + width * iz] = template.getFlow(Math.round(px), Math.round(pz));
                    }
                    else
                    {
                        // Merge flows from four directions
                        Flow flowNW = template.getFlow((int) px, (int) pz);
                        Flow flowNE = template.getFlow((int) px + 1, (int) pz);
                        Flow flowSW = template.getFlow((int) px, (int) pz + 1);
                        Flow flowSE = template.getFlow((int) px + 1, (int) pz + 1);
                        Flow mixed = Flow.lerp(flowNE, flowSE, flowNW, flowSW, px % 1f, 1 - (pz % 1f));
                        Flow.lerp(flowNE, flowSE, flowNW, flowSW, px % 1f, 1 - (pz % 1f));
                        flows[ix + width * iz] = mixed;
                    }
                }
            }
        }

        float sizeScaleFactor = (float) width / StreamTemplate.SIZE;
        this.upstreamX = template.getUpstreamX() * sizeScaleFactor;
        this.upstreamZ = template.getUpstreamZ() * sizeScaleFactor;
        this.downstreamX = template.getDownstreamX() * sizeScaleFactor;
        this.downstreamZ = template.getDownstreamZ() * sizeScaleFactor;
        this.upstreamDirection = template.getUpstreamDirection();
        this.downstreamDirection = template.getDownstreamDirection();

        // Align piece to previous
        if (downstreamPiece != null)
        {
            if (downstreamPiece.upstreamDirection != downstreamDirection)
            {
                throw new IllegalStateException("Can't adjust piece to fit as their upstream and downstream don't align");
            }
            // Adjust x/z position such that previous downstream == current upstream
            this.x = downstreamPiece.getUpstreamX() - downstreamX;
            this.z = downstreamPiece.getUpstreamZ() - downstreamZ;
        }
        else
        {
            this.x = xIn;
            this.z = zIn;
        }
        this.downstreamPiece = downstreamPiece;
        this.box = new XZRange(x, z, width, width);
    }

    public StreamTemplate getTemplate()
    {
        return template;
    }

    public XZRange getBox()
    {
        return box;
    }

    public float getX()
    {
        return x;
    }

    public float getZ()
    {
        return z;
    }

    public int getWidth()
    {
        return width;
    }

    public int getHeight()
    {
        return height;
    }

    @Nullable
    public StreamPiece getDownstream()
    {
        return downstreamPiece;
    }

    public float getUpstreamX()
    {
        return x + upstreamX;
    }

    public float getUpstreamZ()
    {
        return z + upstreamZ;
    }

    public Direction getUpstreamDirection()
    {
        return upstreamDirection;
    }

    public int[] getHeightInfo()
    {
        return surfaceHeight;
    }

    public int getSurfaceMaxHeight()
    {
        return surfaceMaxHeight;
    }

    public Flow getFlow(int x, int z)
    {
        if (x < 0 || z < 0 || x >= width || z >= width)
        {
            throw new IllegalStateException("Tried to get flow with illegal index: x = " + x + ", z = " + z + ", index = " + (x + width * z));
        }
        return flows[x + width * z];
    }

    /**
     * Only for internal use!
     */
    public Flow[] getFlows()
    {
        return flows;
    }

    public CompoundTag serializeNBT()
    {
        // todo
        CompoundTag nbt = new CompoundTag();
        return nbt;
    }
}
