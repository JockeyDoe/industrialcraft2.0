package ic2.core.model;

import ic2.core.block.state.Ic2BlockState;
import ic2.core.util.Util;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ModelComparator {
   private static final Direction[] facings = new Direction[]{
      null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
   };
   private static final Byte UNCACHEABLE = (byte)-1;
   private static final ConcurrentMap<ModelComparator.CacheKey, Byte> cache = new ConcurrentHashMap<>();

   public static boolean isEqual(BlockState stateA, BlockState stateB, Level world, BlockPos pos) {
      assert stateA != stateB;

      byte renderMask = 0;

      for (Direction facing : Direction.values()) {
         boolean renderA = Block.shouldRenderFace(world, pos, stateA, world.getBlockState(pos.relative(facing)), facing);
         boolean renderB = Block.shouldRenderFace(world, pos, stateB, world.getBlockState(pos.relative(facing)), facing);
         if (renderA != renderB) {
            return false;
         }

         if (renderA) {
            renderMask = (byte)(renderMask | 1 << facing.ordinal());
         }
      }

      ModelComparator.CacheKey cacheKey;
      Byte cacheResult;
      if (stateA.getClass() == stateB.getClass()
         && !(stateA instanceof Ic2BlockState.Ic2BlockStateInstance a && a.hasExtraProperties())
         && !(stateB instanceof Ic2BlockState.Ic2BlockStateInstance b && b.hasExtraProperties())) {
         cacheKey = new ModelComparator.CacheKey(stateA, stateB);
         cacheResult = cache.get(cacheKey);
         if (cacheResult != null && cacheResult != UNCACHEABLE) {
            return (cacheResult | ~renderMask) == -1;
         }
      } else {
         cacheKey = null;
         cacheResult = UNCACHEABLE;
      }

      assert cacheResult == null || cacheResult == UNCACHEABLE;

      Ic2Geometry modelA = ModelUtil.getBlockModel(stateA);
      Ic2Geometry modelB = ModelUtil.getBlockModel(stateB);
      Class<?> modelCls = modelA.getClass();
      if (modelB.getClass() != modelCls) {
         if (cacheResult == null) {
            cache.putIfAbsent(cacheKey, (byte)0);
         }

         return false;
      } else {
         if (cacheResult == null
            && modelCls != NativeGeometry.class
            && modelCls != BasicBakedBlockModel.class
            && !modelCls.getName().equals("net.minecraftforge.client.model.ModelLoader$VanillaModelWrapper$1")) {
            assert !Util.inDev();

            cacheResult = UNCACHEABLE;
            cache.putIfAbsent(cacheKey, UNCACHEABLE);
         }

         long rand = Mth.getSeed(pos);
         byte equal = 63;

         label127:
         for (Direction facing : facings) {
            if (cacheResult == null || facing == null || (renderMask & 1 << facing.ordinal()) != 0) {
               List<BakedQuad> quadsA = modelA.func_188616_a(stateA, facing, rand);
               List<BakedQuad> quadsB = modelB.func_188616_a(stateB, facing, rand);
               if (quadsA.size() != quadsB.size()) {
                  if (cacheResult != null) {
                     return false;
                  }

                  if (facing == null) {
                     equal = 0;
                     break;
                  }

                  equal = (byte)(equal & ~(1 << facing.ordinal()));
               } else if (!quadsA.isEmpty()) {
                  for (int i = 0; i < quadsA.size(); i++) {
                     if (!Arrays.equals(QuadData.read(quadsA.get(i)), QuadData.read(quadsB.get(i)))) {
                        if (cacheResult != null) {
                           return false;
                        }

                        if (facing == null) {
                           equal = 0;
                           break label127;
                        }

                        equal = (byte)(equal & ~(1 << facing.ordinal()));
                        break;
                     }
                  }
               }
            }
         }

         if (cacheResult != null) {
            return true;
         } else {
            cache.putIfAbsent(cacheKey, equal);
            return (equal | ~renderMask) == -1;
         }
      }
   }

   public static void onReload() {
      cache.clear();
   }

   private static class CacheKey {
      private final BlockState stateA;
      private final BlockState stateB;

      CacheKey(BlockState stateA, BlockState stateB) {
         this.stateA = stateA;
         this.stateB = stateB;
      }

      @Override
      public boolean equals(Object obj) {
         if (obj != null && obj.getClass() == ModelComparator.CacheKey.class) {
            ModelComparator.CacheKey o = (ModelComparator.CacheKey)obj;
            return this.stateA == o.stateA && this.stateB == o.stateB || this.stateA == o.stateB && this.stateB == o.stateA;
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return System.identityHashCode(this.stateA) ^ System.identityHashCode(this.stateB);
      }
   }
}
