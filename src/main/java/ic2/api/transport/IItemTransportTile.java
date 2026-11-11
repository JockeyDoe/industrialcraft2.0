package ic2.api.transport;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public interface IItemTransportTile extends IPipe {
   int putItems(ItemStack var1, Direction var2, boolean var3);

   ItemStack getContents();

   void setContents(ItemStack var1);

   int getMaxStackSizeAllowed();

   int getTransferRate();
}
