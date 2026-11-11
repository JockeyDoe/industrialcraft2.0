package ic2.core.block.wiring;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import ic2.core.block.BlockFoam;
import ic2.core.block.TileEntityWall;
import ic2.core.block.comp.Obscuration;
import ic2.core.block.state.Ic2BlockState;
import ic2.core.model.AbstractModel;
import ic2.core.model.BasicBakedBlockModel;
import ic2.core.model.ISpecialParticleModel;
import ic2.core.model.Ic2Geometry;
import ic2.core.model.ModelUtil;
import ic2.core.model.VdUtil;
import ic2.core.ref.BlockName;
import ic2.core.ref.TeBlock;
import ic2.core.util.Ic2Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class CableModel extends AbstractModel implements ISpecialParticleModel {
   private final Map<Identifier, TextureAtlasSprite> textures = generateTextureLocations();
   private final LoadingCache<TileEntityCable.CableRenderState, Ic2Geometry> modelCache = CacheBuilder.newBuilder()
      .maximumSize(256L)
      .expireAfterAccess(5L, TimeUnit.MINUTES)
      .build(new CacheLoader<TileEntityCable.CableRenderState, Ic2Geometry>() {
         {
            Objects.requireNonNull(CableModel.this);
         }

         public Ic2Geometry load(TileEntityCable.CableRenderState key) throws Exception {
            return CableModel.this.generateModel(key);
         }
      });

   @Override
   public Collection<Identifier> getTextures() {
      return this.textures.keySet();
   }

   @Override
   public Ic2Geometry bake(Matrix4f state, VertexFormat format, Function<Identifier, TextureAtlasSprite> bakedTextureGetter) {
      for (Entry<Identifier, TextureAtlasSprite> entry : this.textures.entrySet()) {
         entry.setValue(bakedTextureGetter.apply(entry.getKey()));
      }

      return this;
   }

   private static Map<Identifier, TextureAtlasSprite> generateTextureLocations() {
      Map<Identifier, TextureAtlasSprite> ret = new HashMap<>();
      StringBuilder name = new StringBuilder();
      name.append("blocks/wiring/cable/");
      int reset0 = name.length();

      for (CableType type : CableType.values) {
         name.append(type.name());
         name.append("_cable");
         int reset1 = name.length();

         for (int insulation = 0; insulation <= type.maxInsulation; insulation++) {
            if (type.maxInsulation != 0) {
               name.append('_');
               name.append(insulation);
            }

            if (insulation >= type.minColoredInsulation) {
               name.append('_');
               int reset2 = name.length();

               for (Ic2Color color : Ic2Color.values) {
                  name.append(color.name());
                  ret.put(Identifier.fromNamespaceAndPath("ic2", name.toString()), null);
                  name.setLength(reset2);
               }
            } else {
               ret.put(Identifier.fromNamespaceAndPath("ic2", name.toString()), null);
               if (type == CableType.splitter || type == CableType.detector) {
                  ret.put(Identifier.fromNamespaceAndPath("ic2", name.toString() + "_active"), null);
               }
            }

            name.setLength(reset1);
         }

         name.setLength(reset0);
      }

      return ret;
   }

   private static Identifier getTextureLocation(CableType type, int insulation, Ic2Color color, boolean active) {
      String loc = "blocks/wiring/cable/" + type.getName(insulation, color);
      if (active) {
         loc = loc + "_active";
      }

      return Identifier.fromNamespaceAndPath("ic2", loc);
   }

   @Override
   public List<BakedQuad> func_188616_a(BlockState rawState, Direction side, long rand) {
      if (!(rawState instanceof Ic2BlockState.Ic2BlockStateInstance state)) {
         return ModelUtil.getMissingModel().func_188616_a(rawState, side, rand);
      } else if (!state.hasValue(TileEntityCable.renderStateProperty)) {
         return ModelUtil.getMissingModel().func_188616_a(state, side, rand);
      } else {
         TileEntityCable.CableRenderState prop = state.getValue(TileEntityCable.renderStateProperty);
         if (prop.foam == CableFoam.Soft) {
            return ModelUtil.getBlockModel(BlockName.foam.getBlockState(BlockFoam.FoamType.normal)).func_188616_a(state, side, rand);
         } else if (prop.foam == CableFoam.Hardened) {
            TileEntityWall.WallRenderState wallProp = state.getValue(TileEntityWall.renderStateProperty);
            if (wallProp == null) {
               return ModelUtil.getMissingModel().func_188616_a(state, side, rand);
            } else if (wallProp.obscurations == null) {
               return ModelUtil.getBlockModel(BlockName.wall.getBlockState(wallProp.color)).func_188616_a(state, side, rand);
            } else {
               Ic2Geometry model = ModelUtil.getBlockModel(BlockName.te.getBlockState(TeBlock.wall));
               return model.func_188616_a(state, side, rand);
            }
         } else {
            try {
               return ((Ic2Geometry)this.modelCache.get(prop)).func_188616_a(state, side, rand);
            } catch (ExecutionException var9) {
               throw new RuntimeException(var9);
            }
         }
      }
   }

   private Ic2Geometry generateModel(TileEntityCable.CableRenderState prop) {
      float th = prop.type.thickness + prop.insulation * 2 * 0.0625F;
      float sp = (1.0F - th) / 2.0F;
      List<BakedQuad>[] faceQuads = new List[Direction.values().length];

      for (int i = 0; i < faceQuads.length; i++) {
         faceQuads[i] = new ArrayList<>();
      }

      List<BakedQuad> generalQuads = new ArrayList<>();
      TextureAtlasSprite sprite = this.textures.get(getTextureLocation(prop.type, prop.insulation, prop.color, prop.active));

      for (Direction facing : Direction.values()) {
         boolean hasConnection = (prop.connectivity & 1 << facing.ordinal()) != 0;
         float zS = sp;
         float yS = sp;
         float xS = sp;
         float yE;
         float zE;
         float xE = yE = zE = sp + th;
         if (hasConnection) {
            switch (facing) {
               case DOWN:
                  yS = 0.0F;
                  yE = sp;
                  break;
               case UP:
                  yS = sp + th;
                  yE = 1.0F;
                  break;
               case NORTH:
                  zS = 0.0F;
                  zE = sp;
                  break;
               case SOUTH:
                  zS = sp + th;
                  zE = 1.0F;
                  break;
               case WEST:
                  xS = 0.0F;
                  xE = sp;
                  break;
               case EAST:
                  xS = sp + th;
                  xE = 1.0F;
                  break;
               default:
                  throw new RuntimeException();
            }

            VdUtil.addCuboid(xS, yS, zS, xE, yE, zE, EnumSet.complementOf(EnumSet.of(facing.getOpposite())), sprite, faceQuads, generalQuads);
         } else {
            VdUtil.addCuboid(sp, sp, sp, xE, yE, zE, EnumSet.of(facing), sprite, faceQuads, generalQuads);
         }
      }

      int used = 0;

      for (int i = 0; i < faceQuads.length; i++) {
         if (faceQuads[i].isEmpty()) {
            faceQuads[i] = Collections.emptyList();
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

      return new BasicBakedBlockModel(faceQuads, generalQuads, sprite);
   }

   @Override
   public void onReload() {
      this.modelCache.invalidateAll();
   }

   @Override
   public boolean needsEnhancing(BlockState state) {
      return true;
   }

   @Override
   public TextureAtlasSprite getParticleTexture(Ic2BlockState.Ic2BlockStateInstance state) {
      if (!state.hasValue(TileEntityCable.renderStateProperty)) {
         return ModelUtil.getMissingModel().func_177554_e();
      } else {
         TileEntityCable.CableRenderState prop = state.getValue(TileEntityCable.renderStateProperty);
         if (prop.foam == CableFoam.Soft) {
            return ModelUtil.getBlockModel(BlockName.foam.getBlockState(BlockFoam.FoamType.normal)).func_177554_e();
         } else if (prop.foam == CableFoam.Hardened) {
            TileEntityWall.WallRenderState wallProp = state.getValue(TileEntityWall.renderStateProperty);
            if (wallProp == null) {
               return ModelUtil.getMissingModel().func_177554_e();
            } else if (wallProp.obscurations == null) {
               return ModelUtil.getBlockModel(BlockName.wall.getBlockState(wallProp.color)).func_177554_e();
            } else {
               Obscuration.ObscurationData data = wallProp.obscurations[Direction.UP.ordinal()];
               return data == null
                  ? ModelUtil.getBlockModel(BlockName.wall.getBlockState(wallProp.color)).func_177554_e()
                  : ModelUtil.getBlockModel(data.state).func_177554_e();
            }
         } else {
            return this.textures.get(getTextureLocation(prop.type, prop.insulation, prop.color, prop.active));
         }
      }
   }
}
