package ic2.core.item;

import ic2.core.ref.ItemName;
import net.minecraft.world.item.ItemStack;

public class ItemCoke extends ItemIC2 {
   public ItemCoke() {
      super(ItemName.coke);
      ItemDefaults.stackSize(this, 64);
   }

   public int getItemBurnTime(ItemStack itemStack) {
      return 3200;
   }
}
