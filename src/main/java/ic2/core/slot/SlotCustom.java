package ic2.core.slot;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class SlotCustom extends Slot {
   private final Item item;

   public SlotCustom(Container iinventory, Item item, int i, int j, int k) {
      super(iinventory, i, j, k);
      this.item = item;
   }

   public boolean mayPlace(ItemStack itemstack) {
      return itemstack == null ? false : this.item != null && itemstack.getItem() == this.item;
   }
}
