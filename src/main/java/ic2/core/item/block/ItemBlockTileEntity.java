package ic2.core.item.block;

import ic2.core.IC2;
import ic2.core.block.BlockTileEntity;
import ic2.core.block.ITeBlock;
import ic2.core.block.NativeBlockEntities;
import ic2.core.block.TeBlockRegistry;
import ic2.core.block.TileEntityBlock;
import ic2.core.item.ItemDefaults;
import ic2.core.util.ItemStacks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ItemBlockTileEntity extends ItemBlockIC2 {
   public final Identifier identifier;

   public ItemBlockTileEntity(Block block, Identifier identifier) {
      super(block);
      ItemDefaults.variants(this, true);
      this.identifier = identifier;
   }

   @Override
   public String func_77667_c(ItemStack stack) {
      ITeBlock teBlock = this.getTeBlock(stack);
      String name = teBlock == null ? "invalid" : teBlock.getName();
      return super.func_77667_c(stack) + "." + name;
   }

   public void func_150895_a(CreativeModeTab tab, NonNullList<ItemStack> items) {
      ((BlockTileEntity)this.getBlock()).func_149666_a(tab, items);
   }

   @OnlyIn(Dist.CLIENT)
   public void func_77624_a(ItemStack stack, Level world, List<String> tooltip, TooltipFlag advanced) {
      ITeBlock block = this.getTeBlock(stack);
      if (block != null && block.getDummyTe() != null) {
         block.getDummyTe().addInformation(stack, tooltip, advanced);
      }
   }

   @Override
   public boolean placeBlockAt(
      ItemStack stack, Player player, Level world, BlockPos pos, Direction side, float hitX, float hitY, float hitZ, BlockState newState
   ) {
      assert newState.getBlock() == this.getBlock();

      if (!((BlockTileEntity)this.getBlock()).canReplace(world, pos, side, stack)) {
         return false;
      } else {
         ITeBlock teBlock = this.getTeBlock(stack);
         if (teBlock == null) {
            return false;
         } else {
            Class<? extends TileEntityBlock> teClass = teBlock.getTeClass();
            if (teClass == null) {
               return false;
            } else {
               TileEntityBlock te = NativeBlockEntities.instantiate(teClass, pos, newState);
               return placeTeBlock(stack, player, world, pos, side, te);
            }
         }
      }
   }

   public static boolean placeTeBlock(ItemStack stack, LivingEntity placer, Level world, BlockPos pos, Direction side, TileEntityBlock te) {
      te = NativeBlockEntities.at(te, pos);
      BlockState oldState = world.getBlockState(pos);
      BlockState newState = te.getBlockState();
      if (!world.setBlock(pos, newState, 0)) {
         return false;
      } else {
         world.setBlockEntity(te);
         te.onPlaced(stack, placer, side);
         world.markAndNotifyBlock(pos, world.getChunkAt(pos), oldState, newState, 3, 512);
         if (!world.isClientSide()) {
            IC2.network.get(true).sendInitialData(te);
         }

         return true;
      }
   }

   @Override
   public Rarity func_77613_e(ItemStack stack) {
      ITeBlock teblock = this.getTeBlock(stack);
      return teblock != null ? teblock.getRarity() : Rarity.COMMON;
   }

   private ITeBlock getTeBlock(ItemStack stack) {
      return stack == null ? null : TeBlockRegistry.get(this.identifier, ItemStacks.rawMeta(stack));
   }
}
