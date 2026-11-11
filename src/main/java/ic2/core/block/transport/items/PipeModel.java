package ic2.core.block.transport.items;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import ic2.core.block.state.Ic2BlockState;
import ic2.core.block.transport.TileEntityFluidPipe;
import ic2.core.block.transport.TileEntityPipe;
import ic2.core.model.AbstractModel;
import ic2.core.model.BasicBakedBlockModel;
import ic2.core.model.ISpecialParticleModel;
import ic2.core.model.Ic2Geometry;
import ic2.core.model.ModelUtil;
import ic2.core.model.VdUtil;
import ic2.core.util.Util;
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
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class PipeModel extends AbstractModel implements ISpecialParticleModel {
   private final Map<Identifier, TextureAtlasSprite> textures = generateTextureLocations();
   private final LoadingCache<TileEntityPipe.PipeRenderState, Ic2Geometry> modelCache = CacheBuilder.newBuilder()
      .maximumSize(256L)
      .expireAfterAccess(5L, TimeUnit.MINUTES)
      .build(new CacheLoader<TileEntityPipe.PipeRenderState, Ic2Geometry>() {
         {
            Objects.requireNonNull(PipeModel.this);
         }

         public Ic2Geometry load(TileEntityPipe.PipeRenderState key) throws Exception {
            return PipeModel.this.generateModel(key);
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
      ret.put(Identifier.fromNamespaceAndPath("ic2", "blocks/transport/pipe_side"), null);
      StringBuilder name = new StringBuilder();
      name.append("blocks/transport/");
      name.append("pipe");
      int reset0 = name.length();

      for (PipeSize size : PipeSize.values()) {
         name.append('_');
         name.append(size.name());
         ret.put(Identifier.fromNamespaceAndPath("ic2", name.toString()), null);
         name.setLength(reset0);
      }

      return ret;
   }

   private static Identifier getTextureLocation(PipeSize size) {
      String loc = "blocks/transport/pipe_" + size.getName();
      return Identifier.fromNamespaceAndPath("ic2", loc);
   }

   private static Identifier getSideTextureLocation() {
      String loc = "blocks/transport/pipe_side";
      return Identifier.fromNamespaceAndPath("ic2", loc);
   }

   @Override
   public List<BakedQuad> func_188616_a(BlockState rawState, Direction side, long rand) {
      if (!(rawState instanceof Ic2BlockState.Ic2BlockStateInstance state)) {
         return ModelUtil.getMissingModel().func_188616_a(rawState, side, rand);
      } else if (!state.hasValue(TileEntityFluidPipe.renderStateProperty)) {
         return ModelUtil.getMissingModel().func_188616_a(state, side, rand);
      } else {
         TileEntityPipe.PipeRenderState prop = state.getValue(TileEntityFluidPipe.renderStateProperty);

         try {
            return ((Ic2Geometry)this.modelCache.get(prop)).func_188616_a(state, side, rand);
         } catch (ExecutionException var8) {
            throw new RuntimeException(var8);
         }
      }
   }

   private Ic2Geometry generateModel(TileEntityPipe.PipeRenderState prop) {
      PipeType type = prop.type;
      int color = 0xFF000000 | ((byte)type.blue & 255) << 16 | ((byte)type.green & 255) << 8 | (byte)type.red & 255;
      float th = prop.size.thickness;
      float sp = (1.0F - th) / 2.0F;
      Direction pFacing = Direction.values()[prop.facing];
      List<BakedQuad>[] faceQuads = new List[Direction.values().length];

      for (int i = 0; i < faceQuads.length; i++) {
         faceQuads[i] = new ArrayList<>();
      }

      List<BakedQuad> generalQuads = new ArrayList<>();
      TextureAtlasSprite sideSprite = this.textures.get(getSideTextureLocation());
      TextureAtlasSprite sizeSprite = this.textures.get(getTextureLocation(prop.size));
      int totalConnections = Integer.bitCount(prop.connectivity);
      if (totalConnections == 0) {
         float yE;
         float zE;
         float xE = yE = zE = sp + th;
         VdUtil.addCuboid(sp, sp, sp, xE, yE, zE, color, Util.allFacings, sizeSprite, faceQuads, generalQuads);
      } else if (totalConnections == 1) {
         Direction connected = Direction.values()[Integer.numberOfTrailingZeros(prop.connectivity)];

         for (Direction facing : Direction.values()) {
            float zS = sp;
            float yS = sp;
            float xS = sp;
            float yE;
            float zE;
            float xE = yE = zE = sp + th;
            if (facing == connected) {
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

               VdUtil.addCuboid(xS, yS, zS, xE, yE, zE, color, EnumSet.of(facing), sizeSprite, faceQuads, generalQuads);
               VdUtil.addCuboid(
                  xS, yS, zS, xE, yE, zE, color, EnumSet.complementOf(EnumSet.of(facing, facing.getOpposite())), sideSprite, faceQuads, generalQuads
               );
            } else if (facing == connected.getOpposite()) {
               VdUtil.addCuboid(sp, sp, sp, xE, yE, zE, color, EnumSet.of(facing), sizeSprite, faceQuads, generalQuads);
            } else {
               VdUtil.addCuboid(sp, sp, sp, xE, yE, zE, color, EnumSet.of(facing), sideSprite, faceQuads, generalQuads);
            }
         }
      } else {
         for (Direction facingx : Direction.values()) {
            boolean hasConnection = (prop.connectivity & 1 << facingx.ordinal()) != 0;
            float zS = sp;
            float yS = sp;
            float xS = sp;
            float yE;
            float zE;
            float xE = yE = zE = sp + th;
            if (hasConnection) {
               switch (facingx) {
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

               VdUtil.addCuboid(xS, yS, zS, xE, yE, zE, color, EnumSet.of(facingx), sizeSprite, faceQuads, generalQuads);
               VdUtil.addCuboid(
                  xS, yS, zS, xE, yE, zE, color, EnumSet.complementOf(EnumSet.of(facingx, facingx.getOpposite())), sideSprite, faceQuads, generalQuads
               );
            } else {
               VdUtil.addCuboid(sp, sp, sp, xE, yE, zE, color, EnumSet.of(facingx), sideSprite, faceQuads, generalQuads);
            }
         }
      }

      float cs = 1.0F;
      float ch = 0.1F;

      for (Direction facingxx : Direction.values()) {
         boolean hasCover = (prop.covers & 1 << facingxx.ordinal()) != 0;
         float zS = 0.0F;
         float yS = 0.0F;
         float xS = 0.0F;
         float zE = 1.0F;
         float yE = 1.0F;
         float xE = 1.0F;
         if (hasCover) {
            switch (facingxx) {
               case DOWN:
                  yS = 0.0F;
                  yE = ch;
                  break;
               case UP:
                  yS = cs - ch;
                  yE = 1.0F;
                  break;
               case NORTH:
                  zS = 0.0F;
                  zE = ch;
                  break;
               case SOUTH:
                  zS = cs - ch;
                  zE = 1.0F;
                  break;
               case WEST:
                  xS = 0.0F;
                  xE = ch;
                  break;
               case EAST:
                  xS = cs - ch;
                  xE = 1.0F;
                  break;
               default:
                  throw new RuntimeException();
            }

            VdUtil.addCuboid(xS, yS, zS, xE, yE, zE, color, Util.allFacings, sideSprite, faceQuads, generalQuads);
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

      return new BasicBakedBlockModel(faceQuads, generalQuads, sizeSprite);
   }

   @Override
   public void onReload() {
      this.modelCache.invalidateAll();
   }

   @Override
   public TextureAtlasSprite func_177554_e() {
      return this.textures.get(getSideTextureLocation());
   }

   @Override
   public boolean needsEnhancing(BlockState state) {
      return true;
   }

   @Override
   public void enhanceParticle(Particle particle, Ic2BlockState.Ic2BlockStateInstance state) {
      if (state.hasValue(TileEntityPipe.renderStateProperty)) {
         TileEntityPipe.PipeRenderState prop = state.getValue(TileEntityPipe.renderStateProperty);
         if (particle instanceof SingleQuadParticle quad) {
            quad.setColor(prop.type.red / 255.0F, prop.type.green / 255.0F, prop.type.blue / 255.0F);
         }
      }
   }
}
