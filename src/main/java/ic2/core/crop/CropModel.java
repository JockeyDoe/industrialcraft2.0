package ic2.core.crop;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import ic2.api.crops.CropCard;
import ic2.api.crops.Crops;
import ic2.core.block.state.Ic2BlockState;
import ic2.core.model.AbstractModel;
import ic2.core.model.BasicBakedBlockModel;
import ic2.core.model.Ic2Geometry;
import ic2.core.model.ModelRegistry;
import ic2.core.model.VdUtil;
import ic2.core.util.DirectionOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class CropModel extends AbstractModel {
   private static final Identifier STICK = Identifier.fromNamespaceAndPath("ic2", "blocks/crop/stick");
   private static final Identifier UPGRADED_STICK = Identifier.fromNamespaceAndPath("ic2", "blocks/crop/stick_upgraded");
   private static final Function<Identifier, TextureAtlasSprite> MISSING = location -> ModelRegistry.missingSprite();
   static final Map<Identifier, TextureAtlasSprite> textures = new HashMap<>();
   private final LoadingCache<TileEntityCrop.CropRenderState, Ic2Geometry> modelCache = CacheBuilder.newBuilder()
      .maximumSize(256L)
      .expireAfterAccess(5L, TimeUnit.MINUTES)
      .build(new CacheLoader<TileEntityCrop.CropRenderState, Ic2Geometry>() {
         {
            Objects.requireNonNull(CropModel.this);
         }

         public Ic2Geometry load(TileEntityCrop.CropRenderState key) throws Exception {
            return key.crop != null && key.size > 0 ? CropModel.this.generateModel(key) : CropModel.this.generateStickModel(key.crosscrop);
         }
      });

   @Override
   public Collection<Identifier> getTextures() {
      if (textures.isEmpty()) {
         IC2Crops.needsToPost = false;
         NeoForge.EVENT_BUS.post(new Crops.CropRegisterEvent());

         for (CropCard crop : Crops.instance.getCrops()) {
            for (Identifier aux : crop.getTexturesLocation()) {
               textures.put(aux, null);
            }
         }

         textures.put(STICK, null);
         textures.put(UPGRADED_STICK, null);
      }

      return textures.keySet();
   }

   @Override
   public Ic2Geometry bake(Matrix4f state, VertexFormat format, Function<Identifier, TextureAtlasSprite> bakedTextureGetter) {
      for (Entry<Identifier, TextureAtlasSprite> entry : textures.entrySet()) {
         entry.setValue(bakedTextureGetter.apply(entry.getKey()));
      }

      return this;
   }

   private static Identifier getTextureLocation(CropCard crop, int currentSize) {
      return crop.getTexturesLocation().get(currentSize - 1);
   }

   @Override
   public List<BakedQuad> func_188616_a(BlockState rawState, Direction side, long rand) {
      Ic2BlockState.Ic2BlockStateInstance state;
      TileEntityCrop.CropRenderState prop;
      if (rawState instanceof Ic2BlockState.Ic2BlockStateInstance
         && (state = (Ic2BlockState.Ic2BlockStateInstance)rawState).hasValue(TileEntityCrop.renderStateProperty)) {
         prop = state.getValue(TileEntityCrop.renderStateProperty);
      } else {
         prop = new TileEntityCrop.CropRenderState(null, 0, false);
      }

      try {
         return ((Ic2Geometry)this.modelCache.get(prop)).func_188616_a(rawState, side, rand);
      } catch (Exception var8) {
         throw new RuntimeException(var8);
      }
   }

   Ic2Geometry generateModel(TileEntityCrop.CropRenderState prop) {
      List<BakedQuad>[] faceQuads = new List[DirectionOrder.HORIZONTAL.length];

      for (int index = 0; index < faceQuads.length; index++) {
         faceQuads[index] = new ArrayList<>();
      }

      List<BakedQuad> generalQuads = new ArrayList<>();
      TextureAtlasSprite cropSprite = textures.computeIfAbsent(getTextureLocation(prop.crop, prop.size), MISSING);

      for (Direction facing : DirectionOrder.HORIZONTAL) {
         int offsetX = facing.getStepX();
         int offsetZ = facing.getStepZ();
         float x = Math.abs(offsetX) * (0.5F + offsetX * 0.25F);
         float z = Math.abs(offsetZ) * (0.5F + offsetZ * 0.25F);
         float xS = offsetX == 0 ? 0.0F : x;
         float xE = offsetX == 0 ? 1.0F : x;
         float zS = offsetZ == 0 ? 0.0F : z;
         float zE = offsetZ == 0 ? 1.0F : z;
         VdUtil.addFlippedCuboidWithYOffset(xS, 0.001F, zS, xE, 1.0F, zE, -1, EnumSet.of(facing), cropSprite, faceQuads, generalQuads, -0.0625F);
         VdUtil.addFlippedCuboidWithYOffset(xS, 0.001F, zS, xE, 1.0F, zE, -1, EnumSet.of(facing.getOpposite()), cropSprite, faceQuads, generalQuads, -0.0625F);
      }

      int used = 0;

      for (int index = 0; index < faceQuads.length; index++) {
         if (faceQuads[index].isEmpty()) {
            faceQuads[index] = Collections.emptyList();
         } else {
            used++;
         }
      }

      if (used == 0) {
         faceQuads = null;
      }

      if (generalQuads.isEmpty()) {
         generalQuads = Collections.emptyList();
      }

      return new BasicBakedBlockModel(faceQuads, generalQuads, cropSprite);
   }

   Ic2Geometry generateStickModel(boolean crosscrop) {
      List<BakedQuad>[] faceQuads = new List[DirectionOrder.HORIZONTAL.length];

      for (int index = 0; index < faceQuads.length; index++) {
         faceQuads[index] = new ArrayList<>();
      }

      List<BakedQuad> generalQuads = new ArrayList<>();
      TextureAtlasSprite stickSprite = textures.get(STICK);
      TextureAtlasSprite upgradedStickSprite = textures.get(UPGRADED_STICK);

      for (Direction facing : DirectionOrder.HORIZONTAL) {
         int offsetX = facing.getStepX();
         int offsetZ = facing.getStepZ();
         float x = Math.abs(offsetX) * (0.5F + offsetX * 0.25F);
         float z = Math.abs(offsetZ) * (0.5F + offsetZ * 0.25F);
         float xS = offsetX == 0 ? 0.0F : x;
         float xE = offsetX == 0 ? 1.0F : x;
         float zS = offsetZ == 0 ? 0.0F : z;
         float zE = offsetZ == 0 ? 1.0F : z;
         if (!crosscrop) {
            VdUtil.addFlippedCuboidWithYOffset(xS, 0.001F, zS, xE, 1.0F, zE, -1, EnumSet.of(facing), stickSprite, faceQuads, generalQuads, -0.0625F);
            VdUtil.addFlippedCuboidWithYOffset(
               xS, 0.001F, zS, xE, 1.0F, zE, -1, EnumSet.of(facing.getOpposite()), stickSprite, faceQuads, generalQuads, -0.0625F
            );
         } else {
            VdUtil.addFlippedCuboidWithYOffset(xS, 0.001F, zS, xE, 1.0F, zE, -1, EnumSet.of(facing), upgradedStickSprite, faceQuads, generalQuads, -0.0625F);
            VdUtil.addFlippedCuboidWithYOffset(
               xS, 0.001F, zS, xE, 1.0F, zE, -1, EnumSet.of(facing.getOpposite()), upgradedStickSprite, faceQuads, generalQuads, -0.0625F
            );
         }
      }

      int used = 0;

      for (int index = 0; index < faceQuads.length; index++) {
         if (faceQuads[index].isEmpty()) {
            faceQuads[index] = Collections.emptyList();
         } else {
            used++;
         }
      }

      if (used == 0) {
         faceQuads = null;
      }

      if (generalQuads.isEmpty()) {
         generalQuads = Collections.emptyList();
      }

      return new BasicBakedBlockModel(faceQuads, generalQuads, stickSprite);
   }

   @Override
   public TextureAtlasSprite func_177554_e() {
      return textures.get(STICK);
   }

   @Override
   public void onReload() {
      this.modelCache.invalidateAll();
   }
}
