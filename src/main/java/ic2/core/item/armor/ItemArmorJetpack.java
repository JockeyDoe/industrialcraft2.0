package ic2.core.item.armor;

import ic2.core.item.armor.jetpack.IJetpack;
import ic2.core.ref.FluidName;
import ic2.core.ref.ItemName;
import ic2.core.util.FluidTransfer;
import ic2.core.util.ItemStacks;
import ic2.core.util.Util;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public class ItemArmorJetpack extends ItemArmorFluidTank implements IJetpack {
   public ItemArmorJetpack() {
      super(ItemName.jetpack, "jetpack", FluidName.biogas.getInstance(), 30000);
   }

   @Override
   public void func_150895_a(CreativeModeTab tab, NonNullList<ItemStack> subItems) {
      if (this.func_194125_a(tab)) {
         ItemStack stack = new ItemStack(this, 1);
         this.filltank(stack);
         ItemStacks.setRawMeta(stack, 1);
         subItems.add(stack);
         stack = new ItemStack(this, 1);
         ItemStacks.setRawMeta(stack, this.getMaxDamage(stack));
         subItems.add(stack);
      }
   }

   @Override
   public boolean drainEnergy(ItemStack pack, int amount) {
      if (this.isEmpty(pack)) {
         return false;
      } else {
         FluidTransfer handler = FluidTransfer.getFluidHandler(pack);

         assert handler != null;

         FluidStack drained = handler.drain(amount, false);
         if (drained != null && drained.getAmount() >= amount) {
            handler.drain(amount, true);
            this.Updatedamage(pack);
            return true;
         } else {
            return false;
         }
      }
   }

   @Override
   public float getPower(ItemStack stack) {
      return 1.0F;
   }

   @Override
   public float getDropPercentage(ItemStack stack) {
      return 0.2F;
   }

   @Override
   public boolean isJetpackActive(ItemStack stack) {
      return true;
   }

   @Override
   public double getChargeLevel(ItemStack stack) {
      return this.getCharge(stack) / this.getMaxCharge(stack);
   }

   @Override
   public float getHoverMultiplier(ItemStack stack, boolean upwards) {
      return 0.2F;
   }

   @Override
   public float getWorldHeightDivisor(ItemStack stack) {
      return 1.0F;
   }

   @Override
   public int getBarPercent(ItemStack stack) {
      return (int)Util.map(this.getCharge(stack), this.getMaxCharge(stack), 100.0);
   }
}
