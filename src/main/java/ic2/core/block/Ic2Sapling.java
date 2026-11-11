package ic2.core.block;

import ic2.core.IC2;
import ic2.core.init.BlocksItems;
import ic2.core.item.block.ItemBlockIC2;
import ic2.core.ref.BlockName;
import ic2.core.ref.IBlockModelProvider;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Ic2Sapling extends VegetationBlock implements IBlockModelProvider, BonemealableBlock {
   public Ic2Sapling() {
      super(
         Properties.of()
            .noCollision()
            .randomTicks()
            .strength(0.0F)
            .sound(SoundType.GRASS)
            .setId(ResourceKey.create(Registries.BLOCK, IC2.getIdentifier("sapling")))
            .overrideDescription("ic2.sapling.rubber")
      );
      BlocksItems.registerBlock(this, IC2.getIdentifier("sapling"));
      BlocksItems.registerItem(new ItemBlockIC2(this), IC2.getIdentifier("sapling"));
      BlockName.sapling.setInstance(this);
   }

   @Override
   public void registerModels(BlockName name) {
      BlockBase.registerDefaultItemModel(this);
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return Block.column(12.0, 0.0, 12.0);
   }

   public boolean canBeReplacedByLeaves(BlockState state, BlockGetter world, BlockPos pos) {
      return true;
   }

   protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
      if (world.getMaxLocalRawBrightness(pos.above()) >= 9 && random.nextInt(30) == 0) {
         this.grow(world, pos, new Random(random.nextLong()));
      }
   }

   public void func_180650_b(Level world, BlockPos pos, BlockState state, Random random) {
      if (!state.canSurvive(world, pos)) {
         Block.dropResources(state, world, pos);
         world.removeBlock(pos, false);
      } else if (!world.isClientSide() && world.getMaxLocalRawBrightness(pos.above()) >= 9 && random.nextInt(30) == 0) {
         this.grow(world, pos, random);
      }
   }

   private void grow(Level world, BlockPos pos, Random random) {
      new WorldGenRubTree(true).grow(world, pos, random);
   }

   public void func_176474_b(Level world, Random random, BlockPos pos, BlockState state) {
      this.grow(world, pos, random);
   }

   public int func_180651_a(BlockState state) {
      return 0;
   }

   public boolean func_176473_a(Level world, BlockPos pos, BlockState state, boolean client) {
      return true;
   }

   public boolean func_180670_a(Level world, Random random, BlockPos pos, BlockState state) {
      return true;
   }

   public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state, BonemealSource source) {
      return state.canSurvive(world, pos);
   }

   public boolean isBonemealSuccess(Level world, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
      return true;
   }

   public void performBonemeal(ServerLevel world, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
      this.grow(world, pos, new Random(random.nextLong()));
   }
}
