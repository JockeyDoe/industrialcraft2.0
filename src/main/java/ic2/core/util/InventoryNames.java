package ic2.core.util;

import ic2.core.IHasGui;
import ic2.core.block.TileEntityInventory;
import net.minecraft.world.Container;
import net.minecraft.world.Nameable;

public final class InventoryNames {
   private InventoryNames() {
   }

   public static String get(Container inventory) {
      if (inventory instanceof IHasGui gui) {
         return gui.getName();
      } else if (inventory instanceof TileEntityInventory machine) {
         return machine.getName();
      } else {
         return inventory instanceof Nameable named ? named.getDisplayName().getString() : "container.inventory";
      }
   }
}
