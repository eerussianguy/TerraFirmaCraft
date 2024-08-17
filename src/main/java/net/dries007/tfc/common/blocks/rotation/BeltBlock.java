package net.dries007.tfc.common.blocks.rotation;

import java.util.function.Supplier;

import net.dries007.tfc.common.blocks.ExtendedProperties;

public class BeltBlock extends AbstractShaftAxleBlock
{
    private final Supplier<? extends AxleBlock> axle;

    public BeltBlock(ExtendedProperties properties, Supplier<? extends AxleBlock> axle)
    {
        super(properties);
        this.axle = axle;
    }

    @Override
    public AxleBlock getAxle()
    {
        return axle.get();
    }
}
