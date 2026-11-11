package ic2.core.item.tfbp;

import ic2.core.block.machine.tileentity.TileEntityTerra;
import ic2.core.util.BiomeUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.state.BlockState;

class Mushroom extends TerraformerBase {
   @Override
   boolean terraform(Level world, BlockPos pos) {
      pos = TileEntityTerra.getFirstSolidBlockFrom(world, pos, 20);
      return pos == null ? false : growBlockWithDependancy(world, pos, Blocks.BROWN_MUSHROOM_BLOCK, Blocks.BROWN_MUSHROOM);
   }

   private static boolean growBlockWithDependancy(Level world, BlockPos pos, Block target, Block dependancy) {
      MutableBlockPos cPos = new MutableBlockPos();

      for (int xm = pos.getX() - 1; dependancy != null && xm < pos.getX() + 1; xm++) {
         for (int zm = pos.getZ() - 1; zm < pos.getZ() + 1; zm++) {
            for (int ym = pos.getY() + 5; ym > pos.getY() - 2; ym--) {
               cPos.set(xm, ym, zm);
               BlockState state = world.getBlockState(cPos);
               Block block = state.getBlock();
               if (dependancy == Blocks.MYCELIUM) {
                  if (block == dependancy || block == Blocks.BROWN_MUSHROOM_BLOCK || block == Blocks.RED_MUSHROOM_BLOCK) {
                     break;
                  }

                  if (!state.isAir() && (block == Blocks.DIRT || block == Blocks.GRASS_BLOCK)) {
                     BlockPos dstPos = cPos.immutable();
                     world.setBlock(dstPos, dependancy.defaultBlockState(), 3);
                     BiomeUtil.setBiome(world, dstPos, Biomes.MUSHROOM_FIELDS);
                     return true;
                  }
               } else if (dependancy == Blocks.BROWN_MUSHROOM) {
                  if (block == Blocks.BROWN_MUSHROOM || block == Blocks.RED_MUSHROOM) {
                     break;
                  }

                  if (!state.isAir() && growBlockWithDependancy(world, cPos, Blocks.BROWN_MUSHROOM, Blocks.MYCELIUM)) {
                     return true;
                  }
               }
            }
         }
      }

      if (target == Blocks.BROWN_MUSHROOM) {
         Block base = world.getBlockState(pos).getBlock();
         if (base != Blocks.MYCELIUM) {
            if (base != Blocks.BROWN_MUSHROOM_BLOCK && base != Blocks.RED_MUSHROOM_BLOCK) {
               return false;
            }

            world.setBlock(pos, Blocks.MYCELIUM.defaultBlockState(), 3);
         }

         BlockPos above = pos.above();
         BlockState state = world.getBlockState(above);
         Block block = state.getBlock();
         if (!state.isAir() && block != Blocks.SHORT_GRASS) {
            return false;
         } else {
            Block shroom = world.getRandom().nextBoolean() ? Blocks.BROWN_MUSHROOM : Blocks.RED_MUSHROOM;
            world.setBlock(above, shroom.defaultBlockState(), 3);
            return true;
         }
      } else {
         if (target == Blocks.BROWN_MUSHROOM_BLOCK) {
            BlockPos above = pos.above();
            BlockState state = world.getBlockState(above);
            Block basex = state.getBlock();
            if (basex != Blocks.BROWN_MUSHROOM && basex != Blocks.RED_MUSHROOM) {
               return false;
            }

            if (((MushroomBlock)basex).growMushroom((ServerLevel)world, above, state, world.getRandom())) {
               for (int xm = pos.getX() - 1; xm < pos.getX() + 1; xm++) {
                  for (int zm = pos.getZ() - 1; zm < pos.getZ() + 1; zm++) {
                     cPos.set(xm, above.getY(), zm);
                     Block block = world.getBlockState(cPos).getBlock();
                     if (block == Blocks.BROWN_MUSHROOM || block == Blocks.RED_MUSHROOM) {
                        world.removeBlock(cPos.immutable(), false);
                     }
                  }
               }

               return true;
            }
         }

         return false;
      }
   }
}
