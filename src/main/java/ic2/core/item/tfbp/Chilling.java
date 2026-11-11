package ic2.core.item.tfbp;

import ic2.core.block.machine.tileentity.TileEntityTerra;
import ic2.core.util.DirectionOrder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;

class Chilling extends TerraformerBase {
   @Override
   boolean terraform(Level world, BlockPos pos) {
      pos = TileEntityTerra.getFirstBlockFrom(world, pos, 10);
      if (pos == null) {
         return false;
      } else {
         BlockState state = world.getBlockState(pos);
         Block block = state.getBlock();
         if (block != Blocks.WATER && block != Blocks.WATER) {
            if (block == Blocks.ICE) {
               BlockPos below = pos.below();
               Block blockBelow = world.getBlockState(below).getBlock();
               if (blockBelow == Blocks.WATER || blockBelow == Blocks.WATER) {
                  world.setBlock(below, Blocks.ICE.defaultBlockState(), 3);
                  return true;
               }
            } else if (block == Blocks.SNOW) {
               if (isSurroundedBySnow(world, pos)) {
                  world.setBlock(pos, Blocks.SNOW.defaultBlockState(), 3);
                  return true;
               }

               int size = (Integer)state.getValue(SnowLayerBlock.LAYERS);
               if (SnowLayerBlock.LAYERS.getPossibleValues().contains(size + 1)) {
                  world.setBlock(pos, (BlockState)state.setValue(SnowLayerBlock.LAYERS, size + 1), 3);
                  return true;
               }
            }

            pos = pos.above();
            if (!Blocks.SNOW.defaultBlockState().canSurvive(world, pos) && block != Blocks.ICE) {
               return false;
            } else {
               world.setBlock(pos, Blocks.SNOW.defaultBlockState(), 3);
               return true;
            }
         } else {
            world.setBlock(pos, Blocks.ICE.defaultBlockState(), 3);
            return true;
         }
      }
   }

   private static boolean isSurroundedBySnow(Level world, BlockPos pos) {
      for (Direction dir : DirectionOrder.HORIZONTAL) {
         if (!isSnowHere(world, pos.relative(dir))) {
            return false;
         }
      }

      return true;
   }

   private static boolean isSnowHere(Level world, BlockPos pos) {
      int prevY = pos.getY();
      pos = TileEntityTerra.getFirstBlockFrom(world, pos, 16);
      if (pos != null && prevY <= pos.getY()) {
         Block block = world.getBlockState(pos).getBlock();
         if (block != Blocks.SNOW && block != Blocks.SNOW) {
            pos = pos.above();
            if (Blocks.SNOW.defaultBlockState().canSurvive(world, pos) || block == Blocks.ICE) {
               world.setBlock(pos, Blocks.SNOW.defaultBlockState(), 3);
            }

            return false;
         } else {
            return true;
         }
      } else {
         return false;
      }
   }
}
