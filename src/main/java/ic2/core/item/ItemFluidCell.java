package ic2.core.item;

import ic2.core.IC2;
import ic2.core.ref.ItemName;
import ic2.core.util.FluidTransfer;
import ic2.core.util.LiquidUtil;
import ic2.core.util.StackUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.neoforge.transfer.fluid.DispenseFluidContainer;

public class ItemFluidCell extends ItemIC2FluidContainer {
   public ItemFluidCell() {
      super(ItemName.fluid_cell, 1000);
      DispenserBlock.registerBehavior(this, DispenseFluidContainer.getInstance());
   }

   public boolean isRepairable() {
      return false;
   }

   @Override
   public InteractionResult func_180614_a(
      Player player, Level world, BlockPos pos, InteractionHand hand, Direction side, float xOffset, float yOffset, float zOffset
   ) {
      if (world.isClientSide()) {
         return InteractionResult.SUCCESS;
      } else if (this.interactWithTank(player, hand, world, pos, side)) {
         player.inventoryMenu.broadcastChanges();
         return InteractionResult.SUCCESS;
      } else {
         HitResult position = this.func_77621_a(world, player, true);
         if (position == null) {
            return InteractionResult.FAIL;
         } else {
            if (position.getType() == Type.BLOCK) {
               pos = ((BlockHitResult)position).getBlockPos();
               if (!world.mayInteract(player, pos)) {
                  return InteractionResult.FAIL;
               }

               if (!player.mayUseItemAt(pos, ((BlockHitResult)position).getDirection(), player.getItemInHand(hand))) {
                  return InteractionResult.FAIL;
               }

               if (LiquidUtil.drainBlockToContainer(world, pos, player, hand)
                  || LiquidUtil.fillBlockFromContainer(world, pos, player, hand)
                  || LiquidUtil.fillBlockFromContainer(world, pos.relative(side), player, hand)) {
                  player.inventoryMenu.broadcastChanges();
                  return InteractionResult.SUCCESS;
               }
            }

            return InteractionResult.FAIL;
         }
      }
   }

   @Override
   public boolean canfill(Fluid fluid) {
      return true;
   }

   @Override
   public void func_150895_a(CreativeModeTab tab, NonNullList<ItemStack> subItems) {
      if (this.func_194125_a(tab) && !IC2.version.isClassic()) {
         ItemStack emptyStack = new ItemStack(this);
         subItems.add(emptyStack);

         for (Fluid fluid : LiquidUtil.getAllFluids()) {
            if (fluid != null) {
               ItemStack stack = this.getItemStack(fluid);
               if (stack != null) {
                  subItems.add(stack);
               }
            }
         }
      }
   }

   private boolean interactWithTank(Player player, InteractionHand hand, Level world, BlockPos pos, Direction side) {
      assert !world.isClientSide();

      FluidTransfer tileHandler = FluidTransfer.getFluidHandler(world, pos, side);
      if (tileHandler == null) {
         return false;
      } else {
         ItemStack stack = StackUtil.get(player, hand);
         boolean single = StackUtil.getSize(stack) == 1;
         if (!single) {
            stack = StackUtil.copyWithSize(stack, 1);
         }

         boolean changeMade = false;

         do {
            FluidTransfer itemHandler = FluidTransfer.getFluidHandler(StackUtil.copy(stack));

            assert itemHandler != null;

            if (FluidTransfer.tryFluidTransfer(tileHandler, itemHandler, Integer.MAX_VALUE, true) == null) {
               break;
            }

            if (single) {
               StackUtil.set(player, hand, itemHandler.getContainer());
               return true;
            }

            StackUtil.consumeOrError(player, hand, 1);
            StackUtil.storeInventoryItem(itemHandler.getContainer(), player, false);
            changeMade = true;
         } while (!StackUtil.isEmpty(player, hand));

         return changeMade;
      }
   }
}
