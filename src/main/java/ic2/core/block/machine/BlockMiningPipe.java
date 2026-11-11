package ic2.core.block.machine;

import ic2.core.block.BlockMultiID;
import ic2.core.block.state.IIdProvider;
import ic2.core.ref.BlockMaterial;
import ic2.core.ref.BlockName;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class BlockMiningPipe extends BlockMultiID<BlockMiningPipe.MiningPipeType> {
   private static final AABB pipeAabb = new AABB(0.375, 0.0, 0.375, 0.625, 1.0, 0.625);

   public static BlockMiningPipe create() {
      return BlockMultiID.create(BlockMiningPipe.class, BlockMiningPipe.MiningPipeType.class);
   }

   public BlockMiningPipe() {
      super(BlockName.mining_pipe, BlockMaterial.IRON);
      this.func_149711_c(6.0F);
      this.func_149752_b(10.0F);
   }

   @Override
   public boolean func_176196_c(Level worldIn, BlockPos pos) {
      return false;
   }

   public boolean doesSideBlockRendering(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
      BlockMiningPipe.MiningPipeType type = this.getType(state);
      return type == null ? true : type != BlockMiningPipe.MiningPipeType.pipe;
   }

   @Override
   public AABB func_185496_a(BlockState state, BlockGetter world, BlockPos pos) {
      BlockMiningPipe.MiningPipeType type = this.getType(state);
      return type == null ? super.func_185496_a(state, world, pos) : this.getAabb(type);
   }

   private AABB getAabb(BlockMiningPipe.MiningPipeType type) {
      switch (type) {
         case pipe:
            return pipeAabb;
         case tip:
         default:
            return new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
      }
   }

   public int func_149717_k(BlockState state) {
      return state.isSolidRender() ? 255 : 0;
   }

   @Override
   public boolean func_149686_d(BlockState state) {
      BlockMiningPipe.MiningPipeType type = this.getType(state);
      if (type == null) {
         return super.func_149686_d(state);
      } else {
         switch (type) {
            case pipe:
               return false;
            case tip:
            default:
               return true;
         }
      }
   }

   public boolean isNormalCube(BlockState state, BlockGetter world, BlockPos pos) {
      BlockMiningPipe.MiningPipeType type = this.getType(state);
      if (type == null) {
         return true;
      } else {
         switch (type) {
            case pipe:
               return false;
            case tip:
               return true;
            default:
               return true;
         }
      }
   }

   @Override
   public ItemStack getItemStack(BlockState state) {
      BlockMiningPipe.MiningPipeType type = this.getType(state);
      return type == BlockMiningPipe.MiningPipeType.tip ? this.getItemStack(BlockMiningPipe.MiningPipeType.pipe) : super.getItemStack(state);
   }

   @Override
   public void func_149666_a(CreativeModeTab tabs, NonNullList<ItemStack> itemList) {
      for (BlockMiningPipe.MiningPipeType type : this.typeProperty.getShownValues()) {
         if (type != BlockMiningPipe.MiningPipeType.tip) {
            itemList.add(this.getItemStack(type));
         }
      }
   }

   public static enum MiningPipeType implements IIdProvider {
      pipe,
      tip;

      @Override
      public String getName() {
         return this.name();
      }

      @Override
      public int getId() {
         return this.ordinal();
      }
   }
}
