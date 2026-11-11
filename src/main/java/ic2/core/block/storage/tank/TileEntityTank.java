package ic2.core.block.storage.tank;

import ic2.core.ContainerBase;
import ic2.core.IHasGui;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.comp.Fluids;
import ic2.core.block.comp.Ic2FluidTank;
import ic2.core.gui.dynamic.DynamicContainer;
import ic2.core.gui.dynamic.DynamicGui;
import ic2.core.gui.dynamic.GuiParser;
import ic2.core.network.GuiSynced;
import ic2.core.util.FluidTransfer;
import ic2.core.util.ItemData;
import ic2.core.util.LiquidUtil;
import ic2.core.util.StackUtil;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.Capabilities.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

public abstract class TileEntityTank extends TileEntityInventory implements IHasGui {
   protected final Fluids fluidsComponent = this.addComponent(new Fluids(this));
   @GuiSynced
   protected final Ic2FluidTank contents;

   public TileEntityTank(int bucketMultiplier) {
      this.contents = this.fluidsComponent.addTank("contents", 1000 * bucketMultiplier);
   }

   @Override
   protected List<ItemStack> getAuxDrops(int fortune) {
      return Collections.emptyList();
   }

   @Override
   public void onPlaced(ItemStack stack, LivingEntity placer, Direction facing) {
      super.onPlaced(stack, placer, facing);
      if (!this.getLevel().isClientSide()) {
         CompoundTag tag = StackUtil.getOrCreateNbtData(stack);
         this.contents.readFromNBT(tag);
      }
   }

   @Override
   protected ItemStack adjustDrop(ItemStack drop, boolean wrench) {
      CompoundTag tag = StackUtil.getOrCreateNbtData(drop);
      if (this.contents.getFluidAmount() > 0) {
         this.contents.writeToNBT(tag);
         ItemData.commit(drop, tag);
      }

      return drop;
   }

   @Override
   protected boolean onActivated(Player player, InteractionHand hand, Direction side, float hitX, float hitY, float hitZ) {
      ItemStack stack = StackUtil.get(player, hand);
      if (!this.getLevel().isClientSide() && LiquidUtil.isFluidContainer(stack)) {
         boolean changed = FluidTransfer.interactWithFluidHandler(player, hand, this.fluidsComponent.getCapability(Fluid.BLOCK, side));
         if (changed) {
            this.setChanged();
         }

         return changed;
      } else {
         return super.onActivated(player, hand, side, hitX, hitY, hitZ);
      }
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void addInformation(ItemStack stack, List<String> info, TooltipFlag advanced) {
      info.add("Capacity: " + this.contents.getCapacity() + " mB");
      CompoundTag tag = StackUtil.getOrCreateNbtData(stack);
      if (!tag.contains("Empty")) {
         Ic2FluidTank stored = new Ic2FluidTank(this.contents.getCapacity()).readFromNBT(tag);
         FluidStack fluidStack = stored.getFluid();
         if (fluidStack == null) {
            info.add("Empty");
         } else {
            info.add(fluidStack.getHoverName().getString());
            info.add("Amount: " + fluidStack.getAmount() + " mB");
            info.add("Type: " + (fluidStack.getFluidType().isLighterThanAir() ? "Gas" : "Liquid"));
         }
      } else {
         info.add("Empty");
      }
   }

   @Override
   protected SoundType getBlockSound(Entity entity) {
      return SoundType.METAL;
   }

   @Override
   public ContainerBase<? extends TileEntityTank> getGuiContainer(Player player) {
      return DynamicContainer.create(this, player, GuiParser.parse(this.teBlock));
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Screen getGui(Player player, boolean isAdmin) {
      return DynamicGui.<TileEntityTank>create(this, player, GuiParser.parse(this.teBlock));
   }

   @Override
   public void onGuiClosed(Player player) {
   }
}
