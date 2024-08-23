package net.dries007.tfc.client.model;

import java.util.List;
import java.util.function.Function;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.client.ClimateRenderCache;
import net.dries007.tfc.client.RenderHelpers;
import net.dries007.tfc.util.Helpers;

public class GrassBlockModel implements IDynamicBakedModel, IUnbakedGeometry<GrassBlockModel>
{
    private final BlockModel grassModel;
    private final BlockModel snowModel;
    @Nullable private BakedModel snowBakedModel = null;
    @Nullable private BakedModel grassBakedModel = null;

    public GrassBlockModel(BlockModel grassModel, BlockModel snowModel)
    {
        this.grassModel = grassModel;
        this.snowModel = snowModel;
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData)
    {
        final boolean snow = level.getBrightness(LightLayer.SKY, pos.above()) > 3 &&
                             !level.getBlockState(pos.above()).canOcclude() &&
                             ClimateRenderCache.INSTANCE.getTemperature() < 0;
        assert snowBakedModel != null;
        assert grassBakedModel != null;
        return modelData.derive().with(BakedModelData.PROPERTY, new BakedModelData(snow ? snowBakedModel : grassBakedModel)).build();
    }


    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> atlas, ModelState modelState, ItemOverrides overrides)
    {
        snowBakedModel = snowModel.bake(baker, atlas, modelState);
        grassBakedModel = grassModel.bake(baker, atlas, modelState);
        return this;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random, ModelData modelData, @Nullable RenderType renderType)
    {
        final BakedModelData bakedData = modelData.get(BakedModelData.PROPERTY);
        if (bakedData != null)
        {
            return bakedData.toRender.getQuads(state, direction, random, modelData, renderType);
        }
        assert grassBakedModel != null;
        return grassBakedModel.getQuads(state, direction, random, modelData, renderType);
    }

    @Override
    public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context)
    {
        grassModel.resolveParents(modelGetter);
        snowModel.resolveParents(modelGetter);
    }

    @Override
    public boolean useAmbientOcclusion()
    {
        return true;
    }

    @Override
    public boolean isGui3d()
    {
        return false;
    }

    @Override
    public boolean usesBlockLight()
    {
        return true;
    }

    @Override
    public boolean isCustomRenderer()
    {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon()
    {
        return RenderHelpers.missingTexture();
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data)
    {
        final BakedModelData bakedData = data.get(BakedModelData.PROPERTY);
        return bakedData != null ? bakedData.toRender.getParticleIcon(data) : RenderHelpers.missingTexture();
    }

    @Override
    public ItemOverrides getOverrides()
    {
        return ItemOverrides.EMPTY;
    }

    record BakedModelData(BakedModel toRender)
    {
        public static final ModelProperty<BakedModelData> PROPERTY = new ModelProperty<>();
    }

    public static class Loader implements IGeometryLoader<GrassBlockModel>
    {
        @Override
        public GrassBlockModel read(JsonObject json, JsonDeserializationContext context) throws JsonParseException
        {
            return new GrassBlockModel(
                context.deserialize(json.get("grass"), BlockModel.class),
                context.deserialize(json.get("snow"), BlockModel.class)
            );
        }

    }
}
