package ic2.core.item.tfbp;

import ic2.core.block.machine.tileentity.TileEntityTerra;
import ic2.core.util.DirectionOrder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;

class Irrigation extends TerraformerBase {
   @Override
   boolean terraform(Level world, BlockPos pos) {
      if (world.getRandom().nextInt(48000) == 0) {
         if (world instanceof ServerLevel server) {
            server.getWeatherData().setRaining(true);
         }

         return true;
      } else {
         pos = TileEntityTerra.getFirstBlockFrom(world, pos, 10);
         if (pos == null) {
            return false;
         } else if (TileEntityTerra.switchGround(world, pos, Blocks.SAND, Blocks.DIRT.defaultBlockState(), true)) {
            TileEntityTerra.switchGround(world, pos, Blocks.SAND, Blocks.DIRT.defaultBlockState(), true);
            return true;
         } else {
            BlockState state = world.getBlockState(pos);
            Block block = state.getBlock();
            if (block instanceof BonemealableBlock && ((BonemealableBlock)block).isValidBonemealTarget(world, pos, state, BonemealSource.INTERACTION)) {
               if (world instanceof ServerLevel server) {
                  ((BonemealableBlock)block).performBonemeal(server, world.getRandom(), pos, state, BonemealSource.INTERACTION);
               }

               return true;
            } else if (block != Blocks.SHORT_GRASS) {
               if (block == Blocks.OAK_LOG || block == Blocks.ACACIA_LOG) {
                  BlockPos above = pos.above();
                  world.setBlock(above, state, 3);
                  BlockState leaves = getLeaves(world, pos);
                  if (leaves != null) {
                     createLeaves(world, above, leaves);
                  }

                  return true;
               } else if (block == Blocks.FIRE) {
                  world.removeBlock(pos, false);
                  return true;
               } else {
                  return false;
               }
            } else {
               return spreadGrass(world, pos.north()) || spreadGrass(world, pos.east()) || spreadGrass(world, pos.south()) || spreadGrass(world, pos.west());
            }
         }
      }
   }

   private static BlockState getLeaves(Level world, BlockPos pos) {
      for (Direction facing : DirectionOrder.HORIZONTAL) {
         BlockPos cPos = pos.relative(facing);
         BlockState state = world.getBlockState(cPos);
         if (state.is(BlockTags.LEAVES)) {
            return state;
         }
      }

      return null;
   }

   private static void createLeaves(Level world, BlockPos pos, BlockState state) {
      BlockPos above = pos.above();
      if (world.isEmptyBlock(above)) {
         world.setBlock(above, state, 3);
      }

      for (Direction facing : DirectionOrder.HORIZONTAL) {
         BlockPos cPos = pos.relative(facing);
         if (world.isEmptyBlock(cPos)) {
            world.setBlock(cPos, state, 3);
         }
      }
   }

   private static boolean spreadGrass(Level world, BlockPos pos) {
      if (world.getRandom().nextBoolean()) {
         return false;
      } else {
         pos = TileEntityTerra.getFirstBlockFrom(world, pos, 0);
         if (pos == null) {
            return false;
         } else {
            Block block = world.getBlockState(pos).getBlock();
            if (block == Blocks.DIRT) {
               world.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
               return true;
            } else if (block == Blocks.GRASS_BLOCK) {
               world.setBlock(pos.above(), Blocks.SHORT_GRASS.defaultBlockState(), 3);
               return true;
            } else {
               return false;
            }
         }
      }
   }
}
