package ic2.core.item.tfbp;

import ic2.core.block.machine.tileentity.TileEntityTerra;
import ic2.core.ref.BlockName;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

class Desertification extends TerraformerBase {
   @Override
   boolean terraform(Level world, BlockPos pos) {
      pos = TileEntityTerra.getFirstBlockFrom(world, pos, 10);
      if (pos == null) {
         return false;
      } else {
         BlockState sand = Blocks.SAND.defaultBlockState();
         if (!TileEntityTerra.switchGround(world, pos, Blocks.DIRT, sand, false)
            && !TileEntityTerra.switchGround(world, pos, Blocks.GRASS_BLOCK, sand, false)
            && !TileEntityTerra.switchGround(world, pos, Blocks.FARMLAND, sand, false)) {
            Block block = world.getBlockState(pos).getBlock();
            if (block == Blocks.WATER
               || block == Blocks.WATER
               || block == Blocks.SNOW
               || block == Blocks.OAK_LEAVES
               || block == Blocks.ACACIA_LEAVES
               || block == BlockName.leaves.getInstance()
               || isPlant(block)) {
               world.removeBlock(pos, false);
               if (isPlant(world.getBlockState(pos.above()).getBlock())) {
                  world.removeBlock(pos.above(), false);
               }

               return true;
            } else if (block != Blocks.ICE && block != Blocks.SNOW) {
               if ((world.getBlockState(pos).is(BlockTags.PLANKS) || block == Blocks.OAK_LOG || block == BlockName.rubber_wood.getInstance())
                  && world.getRandom().nextInt(15) == 0) {
                  world.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
                  return true;
               } else {
                  return false;
               }
            } else {
               world.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
               return true;
            }
         } else {
            TileEntityTerra.switchGround(world, pos, Blocks.DIRT, sand, false);
            return true;
         }
      }
   }

   private static boolean isPlant(Block block) {
      for (BlockState state : Cultivation.plants) {
         if (state.getBlock() == block) {
            return true;
         }
      }

      return false;
   }
}
