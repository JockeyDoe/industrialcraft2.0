package ic2.core.item.tfbp;

import ic2.core.block.machine.tileentity.TileEntityTerra;
import ic2.core.ref.BlockName;
import ic2.core.util.DirectionOrder;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

class Cultivation extends TerraformerBase {
   static ArrayList<BlockState> plants = new ArrayList<>();

   @Override
   void init() {
      plants.add(Blocks.SHORT_GRASS.defaultBlockState());
      plants.add(Blocks.SHORT_GRASS.defaultBlockState());
      plants.add(Blocks.FERN.defaultBlockState());
      plants.add(Blocks.POPPY.defaultBlockState());
      plants.add(Blocks.DANDELION.defaultBlockState());
      plants.add(Blocks.TALL_GRASS.defaultBlockState());
      plants.add(Blocks.ROSE_BUSH.defaultBlockState());
      plants.add(Blocks.SUNFLOWER.defaultBlockState());

      for (Block sapling : new Block[]{
         Blocks.OAK_SAPLING, Blocks.SPRUCE_SAPLING, Blocks.BIRCH_SAPLING, Blocks.JUNGLE_SAPLING, Blocks.ACACIA_SAPLING, Blocks.DARK_OAK_SAPLING
      }) {
         plants.add(sapling.defaultBlockState());
      }

      plants.add(Blocks.WHEAT.defaultBlockState());
      plants.add(Blocks.RED_MUSHROOM.defaultBlockState());
      plants.add(Blocks.BROWN_MUSHROOM.defaultBlockState());
      plants.add(Blocks.PUMPKIN.defaultBlockState());
      plants.add(Blocks.MELON.defaultBlockState());
      plants.add(BlockName.sapling.getInstance().defaultBlockState());
   }

   @Override
   boolean terraform(Level world, BlockPos pos) {
      pos = TileEntityTerra.getFirstSolidBlockFrom(world, pos, 10);
      if (pos == null) {
         return false;
      } else if (TileEntityTerra.switchGround(world, pos, Blocks.SAND, Blocks.DIRT.defaultBlockState(), true)) {
         return true;
      } else {
         if (TileEntityTerra.switchGround(world, pos, Blocks.END_STONE, Blocks.DIRT.defaultBlockState(), true)) {
            int i = 4;

            while (--i > 0 && TileEntityTerra.switchGround(world, pos, Blocks.END_STONE, Blocks.DIRT.defaultBlockState(), true)) {
            }
         }

         Block block = world.getBlockState(pos).getBlock();
         if (block == Blocks.DIRT) {
            world.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            return true;
         } else {
            return block == Blocks.GRASS_BLOCK ? growPlantsOn(world, pos) : false;
         }
      }
   }

   private static boolean growPlantsOn(Level world, BlockPos pos) {
      BlockPos above = pos.above();
      BlockState state = world.getBlockState(above);
      Block block = state.getBlock();
      if (state.isAir() || block == Blocks.SHORT_GRASS && world.getRandom().nextInt(4) == 0) {
         BlockState plant = pickRandomPlant(world.getRandom());
         if (plant.hasProperty(DirectionalBlock.FACING)) {
            plant = (BlockState)plant.setValue(DirectionalBlock.FACING, DirectionOrder.HORIZONTAL[world.getRandom().nextInt(DirectionOrder.HORIZONTAL.length)]);
         }

         if (plant.getBlock() instanceof CropBlock) {
            world.setBlock(pos, Blocks.FARMLAND.defaultBlockState(), 3);
         } else if (plant.getBlock() instanceof DoublePlantBlock) {
            if (world.getBlockState(above.above()).canBeReplaced() && plant.canSurvive(world, above)) {
               plant = (BlockState)plant.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER);
               world.setBlock(above, (BlockState)plant.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER), 3);
               world.setBlock(above.above(), (BlockState)plant.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER), 3);
               return true;
            }

            return false;
         }

         if (!plant.canSurvive(world, above)) {
            return false;
         } else {
            world.setBlock(above, plant, 3);
            return true;
         }
      } else {
         return false;
      }
   }

   private static BlockState pickRandomPlant(RandomSource random) {
      return plants.get(random.nextInt(plants.size()));
   }
}
