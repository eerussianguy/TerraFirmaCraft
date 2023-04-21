/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.client.model.block;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import com.google.common.collect.Maps;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.IModelConfiguration;
import net.minecraftforge.client.model.IModelLoader;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.geometry.IModelGeometry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.client.RenderHelpers;
import net.dries007.tfc.common.blocks.ThinSpikeBlock;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.JsonHelpers;

public class SodaStrawsBakedModel implements BasicBakedModel
{
    public static final List<SodaStrawsBakedModel> INSTANCES = new ArrayList<>();

    private final List<BakedModel> shortStraws = new ArrayList<>(256);
    private final List<BakedModel> longStraws = new ArrayList<>(256);

    @Nullable
    private TextureAtlasSprite texture = null;

    private final BlockModel blockModel;
    private final ResourceLocation modelLocation;
    private final ResourceLocation textureLocation;

    public SodaStrawsBakedModel(ResourceLocation modelLocation, ResourceLocation textureLocation)
    {
        this.blockModel = new BlockModel(null, new ArrayList<>(), new HashMap<>(), false, BlockModel.GuiLight.FRONT, ItemTransforms.NO_TRANSFORMS, new ArrayList<>());

        this.modelLocation = modelLocation;
        this.textureLocation = textureLocation;
        INSTANCES.add(this);
    }

    public void init()
    {
        texture = Minecraft.getInstance().getTextureAtlas(RenderHelpers.BLOCKS_ATLAS).apply(textureLocation);
        final Random random = new Random(42L);
        buildModels(random);
    }

    private void buildModels(Random random)
    {
        for (int x = 0; x < 16; x++)
        {
            for (int z = 0; z < 16; z++)
            {
                shortStraws.add(buildStraw(x, Mth.nextInt(random, 5, 15), z));
                longStraws.add(buildStraw(x, 0, z));
            }
        }
    }

    private BakedModel buildStraw(int x, int y, int z)
    {
        assert texture != null;
        final Map<Direction, BlockElementFace> faces = Maps.newEnumMap(Direction.class);
        for (Direction d : Helpers.DIRECTIONS)
        {
            faces.put(d, RenderHelpers.makeFace(RenderHelpers.UV_DEFAULT));
        }
        final BlockElement part = new BlockElement(new Vector3f(x, y, z), new Vector3f(x + 1, 0, z + 1), faces, null, true);
        final var builder = new SimpleBakedModel.Builder(blockModel, ItemOverrides.EMPTY, false).particle(texture);

        for (var entry : part.faces.entrySet())
        {
            final Direction d = entry.getKey();
            builder.addCulledFace(d, RenderHelpers.makeBakedQuad(part, entry.getValue(), texture, d, BlockModelRotation.X0_Y0, modelLocation));
        }

        return builder.build();
    }

    @NotNull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand, IModelData extraData)
    {
        if (extraData instanceof ModelData data)
        {
            final List<BakedQuad> list = new ArrayList<>(64 * 6);
            for (BakedModel model : data.models)
            {
                list.addAll(model.getQuads(state, side, rand, extraData));
            }
            return list;
        }
        return List.of();
    }

    @NotNull
    @Override
    public IModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, IModelData modelData)
    {
        return new ModelData(state, pos, shortStraws, longStraws);
    }

    @Override
    public TextureAtlasSprite getParticleIcon()
    {
        return Objects.requireNonNull(texture);
    }

    public static class ModelData implements BlankModelData
    {
        private final List<BakedModel> models;

        public ModelData(BlockState state, BlockPos pos, List<BakedModel> shortStraws, List<BakedModel> longStraws)
        {
            final Random rand = new Random();
            pos = new BlockPos(pos.getX(), 0, pos.getY());
            rand.setSeed(pos.asLong() * 654321L);
            if (state.getValue(ThinSpikeBlock.TIP))
            {
                models = uniqueRandomSample(shortStraws, longStraws, 64, rand).list1;
            }
            else
            {
                models = uniqueRandomSample(shortStraws, longStraws, 64, rand).list2;
            }
        }

        private static <T> Result<T> uniqueRandomSample(List<T> list, List<T> list2, int n, Random r)
        {
            final int length = list.size();
            if (length < n)
            {
                throw new IllegalArgumentException("Cannot select n=" + n + " from a list of size = " + length);
            }
            if (list.size() != list2.size())
            {
                throw new IllegalArgumentException("List sizes n=" + n + "and n=" + list2.size() + " unequal");
            }
            for (int i = length - 1; i >= length - n; --i)
            {
                final int swap = r.nextInt(i + 1);
                Collections.swap(list, i, swap);
                Collections.swap(list2, i, swap);
            }
            return new Result<>(list.subList(length - n, length), list2.subList(length - n, length));
        }

        private record Result<T>(List<T> list1, List<T> list2) {}
    }

    public static class Loader implements IModelLoader<ModelGeometry>
    {
        @Override
        public ModelGeometry read(JsonDeserializationContext context, JsonObject modelContents)
        {
            final JsonObject textures = JsonHelpers.getAsJsonObject(modelContents, "textures");
            return new ModelGeometry(new ResourceLocation(JsonHelpers.getAsString(textures, "all")));
        }

        @Override
        public void onResourceManagerReload(ResourceManager manager) { }
    }

    public record ModelGeometry(ResourceLocation texture) implements IModelGeometry<ModelGeometry>
    {
        @Override
        public BakedModel bake(IModelConfiguration owner, ModelBakery bakery, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelTransform, ItemOverrides overrides, ResourceLocation modelLocation)
        {
            return new SodaStrawsBakedModel(modelLocation, texture);
        }

        @Override
        public Collection<Material> getTextures(IModelConfiguration owner, Function<ResourceLocation, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors)
        {
            return RenderHelpers.makeMaterials(texture);
        }
    }

}
