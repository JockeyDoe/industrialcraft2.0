package ic2.core.item.capability;

import ic2.core.block.comp.Ic2FluidTank;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class CapabilityFluidHandlerItem extends ItemAccessResourceHandler<FluidResource> {
   protected final int capacity;
   private final Item validItem;

   public CapabilityFluidHandlerItem(ItemStack stack, int capacity) {
      this(stack, ItemAccess.forStack(stack), capacity);
   }

   public CapabilityFluidHandlerItem(ItemStack stack, ItemAccess access, int capacity) {
      super(access, 1);
      this.capacity = capacity;
      this.validItem = stack.getItem();
   }

   private Ic2FluidTank contents(ItemResource resource) {
      Ic2FluidTank tank = new Ic2FluidTank(this.capacity);
      if (!resource.isEmpty() && resource.is(this.validItem)) {
         CompoundTag tag = ((CustomData)resource.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
         if (tag.contains("Fluid")) {
            tank.readFromNBT(tag.getCompoundOrEmpty("Fluid"));
         }

         return tank;
      } else {
         return tank;
      }
   }

   protected FluidResource getResourceFrom(ItemResource resource, int index) {
      return (FluidResource)this.contents(resource).getResource(0);
   }

   protected int getAmountFrom(ItemResource resource, int index) {
      return this.contents(resource).getFluidAmount();
   }

   protected ItemResource update(ItemResource item, int index, FluidResource resource, int amount) {
      CompoundTag tag = ((CustomData)item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
      if (amount <= 0) {
         tag.remove("Fluid");
      } else {
         Ic2FluidTank tank = new Ic2FluidTank(resource.toStack(amount), this.capacity);
         tag.put("Fluid", tank.writeToNBT(new CompoundTag()));
      }

      return tag.isEmpty() ? item.without(DataComponents.CUSTOM_DATA) : item.with(DataComponents.CUSTOM_DATA, CustomData.of(tag));
   }

   protected int getCapacity(int index, FluidResource resource) {
      return this.capacity;
   }

   public boolean canFillFluidType(FluidStack fluid) {
      return fluid != null && !fluid.isEmpty();
   }

   public boolean canDrainFluidType(FluidStack fluid) {
      return fluid != null && !fluid.isEmpty();
   }

   public boolean isValid(int index, FluidResource resource) {
      return this.itemAccess.getResource().is(this.validItem) && this.canFillFluidType(resource.toStack(1));
   }

   public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
      return this.canDrainFluidType(resource.toStack(1)) ? super.extract(index, resource, amount, transaction) : 0;
   }

   public ItemStack getContainer() {
      return this.itemAccess.getResource().toStack(this.itemAccess.getAmount());
   }
}
