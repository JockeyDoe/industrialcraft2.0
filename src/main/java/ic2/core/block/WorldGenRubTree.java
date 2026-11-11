package ic2.core.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import ic2.core.IC2;
import ic2.core.ref.BlockName;
import ic2.core.util.DirectionOrder;
import ic2.core.util.LogCategory;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockGrowFeatureEvent;

public class WorldGenRubTree implements Feature {
   public static final int maxHeight = 8;
   private final boolean notify;
   public static final MapCodec<WorldGenRubTree> CODEC = Codec.BOOL.optionalFieldOf("notify", false).xmap(WorldGenRubTree::new, tree -> tree.notify);

   public MapCodec<WorldGenRubTree> codec() {
      return CODEC;
   }

   public boolean place(WorldGenLevel world, ChunkGenerator generator, RandomSource random, BlockPos pos) {
      return this.grow(world, pos, new Random(random.nextLong()));
   }

   private void func_175903_a(LevelAccessor world, BlockPos pos, BlockState state) {
      world.setBlock(pos, state, this.notify ? 3 : 2);
   }

   public WorldGenRubTree(boolean notify) {
      this.notify = notify;
   }

   public boolean func_180709_b(Level world, Random random, BlockPos pos) {
      MutableBlockPos cPos = new MutableBlockPos();
      cPos.set(pos.getX() + 8, world.getMaxY(), pos.getZ() + 8);

      while (world.isEmptyBlock(cPos) && cPos.getY() > world.getMinY()) {
         cPos.set(cPos.getX(), cPos.getY() - 1, cPos.getZ());
      }

      cPos.set(cPos.getX(), cPos.getY() + 1, cPos.getZ());
      return this.grow(world, cPos, random);
   }

   public boolean grow(LevelAccessor world, BlockPos pos, Random random) {
      if (world == null) {
         IC2.log.warn(LogCategory.General, "RubberTree did not spawn! w=%s.", world);
         return false;
      } else {
         BlockGrowFeatureEvent event = new BlockGrowFeatureEvent(world, RandomSource.create(random.nextLong()), pos, Holder.direct(this));
         NeoForge.EVENT_BUS.post(event);
         if (event.isCanceled()) {
            return false;
         } else if (event.getFeature().value() != this && world instanceof WorldGenLevel gen) {
            return ((Feature)event.getFeature().value()).place(gen, gen.getLevel().getChunkSource().getGenerator(), event.getRandom(), pos);
         } else {
            Block woodBlock = BlockName.rubber_wood.getInstance();
            BlockState leaves = Ic2Leaves.dryState(BlockName.leaves.getInstance().defaultBlockState());
            int treeholechance = 25;
            int height = this.getGrowHeight(world, pos);
            if (height < 2) {
               return false;
            } else {
               height -= random.nextInt(height / 2 + 1);
               MutableBlockPos tmpPos = new MutableBlockPos();

               for (int cHeight = 0; cHeight < height; cHeight++) {
                  BlockPos cPos = pos.above(cHeight);
                  if (random.nextInt(100) <= treeholechance) {
                     treeholechance -= 10;
                     this.func_175903_a(
                        world,
                        cPos,
                        (BlockState)woodBlock.defaultBlockState()
                           .setValue(BlockRubWood.stateProperty, BlockRubWood.RubberWoodState.getWet(DirectionOrder.HORIZONTAL[random.nextInt(4)]))
                     );
                  } else {
                     this.func_175903_a(
                        world, cPos, (BlockState)woodBlock.defaultBlockState().setValue(BlockRubWood.stateProperty, BlockRubWood.RubberWoodState.plain_y)
                     );
                  }

                  if (height < 4 || height < 7 && cHeight > 1 || cHeight > 2) {
                     for (int cx = pos.getX() - 2; cx <= pos.getX() + 2; cx++) {
                        for (int cz = pos.getZ() - 2; cz <= pos.getZ() + 2; cz++) {
                           int chance = Math.max(1, cHeight + 4 - height);
                           int dx = Math.abs(cx - pos.getX());
                           int dz = Math.abs(cz - pos.getZ());
                           if (dx <= 1 && dz <= 1 || dx <= 1 && random.nextInt(chance) == 0 || dz <= 1 && random.nextInt(chance) == 0) {
                              tmpPos.set(cx, pos.getY() + cHeight, cz);
                              if (world.isEmptyBlock(tmpPos)) {
                                 this.func_175903_a(world, tmpPos.immutable(), leaves);
                              }
                           }
                        }
                     }
                  }
               }

               for (int i = 0; i <= height / 4 + random.nextInt(2); i++) {
                  tmpPos.set(pos.getX(), pos.getY() + height + i, pos.getZ());
                  if (world.isEmptyBlock(tmpPos)) {
                     this.func_175903_a(world, tmpPos.immutable(), leaves);
                  }
               }

               return true;
            }
         }
      }
   }

   public int getGrowHeight(LevelAccessor world, BlockPos pos) {
      BlockPos below = pos.below();
      BlockState baseState = world.getBlockState(below);
      Block baseBlock = baseState.getBlock();
      if (!baseState.isAir()
         && this.plantSupported(baseState, world, below, pos)
         && (world.isEmptyBlock(pos.above()) || world.getBlockState(pos.above()).getBlock() == BlockName.sapling.getInstance())) {
         int height = 1;

         for (BlockPos var7 = pos.above(); world.isEmptyBlock(var7) && height < 8 && !world.isOutsideBuildHeight(var7); height++) {
            var7 = var7.above();
         }

         return height;
      } else {
         return 0;
      }
   }

   private boolean plantSupported(BlockState soil, LevelAccessor world, BlockPos below, BlockPos pos) {
      BlockState sapling = BlockName.sapling.getInstance().defaultBlockState();
      TriState support = soil.canSustainPlant(world, below, Direction.UP, sapling);
      return support.isTrue() || support.isDefault() && sapling.canSurvive(world, pos);
   }
}
