package net.dries007.tfc.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.resources.ResourceLocation;

import net.dries007.tfc.client.RenderHelpers;
import net.dries007.tfc.client.render.blockentity.BowlBlockEntityRenderer;

public class DustParticle extends TextureSheetParticle
{
    public DustParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed)
    {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.gravity = 0.5f;
        this.lifetime = 20 + level.random.nextInt(20);
        scale(0.5f);
    }

    @Override
    public ParticleRenderType getRenderType()
    {
        return ParticleRenderType.TERRAIN_SHEET;
    }

    public record Provider(SpriteSet sprite) implements ParticleProvider<ItemParticleOption>
    {
        @Override
        public Particle createParticle(ItemParticleOption option, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed)
        {
            final DustParticle particle = new DustParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
            final ResourceLocation texture = BowlBlockEntityRenderer.getTexture(option.getItem().getItem());
            final TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(RenderHelpers.BLOCKS_ATLAS).apply(texture);
            particle.setSprite(sprite);
            return particle;
        }
    }
}
