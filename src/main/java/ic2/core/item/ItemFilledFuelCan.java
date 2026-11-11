package ic2.core.item;

import ic2.core.item.type.CraftingItemType;
import ic2.core.ref.ItemName;
import ic2.core.util.StackUtil;
import net.minecraft.world.item.ItemStack;

public class ItemFilledFuelCan extends ItemIC2 {
   public ItemFilledFuelCan() {
      super(ItemName.filled_fuel_can);
      ItemDefaults.stackSize(this, 1);
   }

   @Override
   public boolean hasContainerItem(ItemStack stack) {
      return true;
   }

   @Override
   public ItemStack getContainerItem(ItemStack stack) {
      return ItemName.crafting.getItemStack(CraftingItemType.empty_fuel_can);
   }

   public int getItemBurnTime(ItemStack stack) {
      return StackUtil.getOrCreateNbtData(stack).getIntOr("value", 0) * 2;
   }
}
