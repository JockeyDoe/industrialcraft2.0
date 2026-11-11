package ic2.core.item.armor;

import ic2.core.ref.FluidName;
import ic2.core.ref.ItemName;
import ic2.core.util.ItemStacks;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ItemArmorCFPack extends ItemArmorFluidTank {
   public ItemArmorCFPack() {
      super(ItemName.cf_pack, "batpack", FluidName.construction_foam.getInstance(), 80000);
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
}
