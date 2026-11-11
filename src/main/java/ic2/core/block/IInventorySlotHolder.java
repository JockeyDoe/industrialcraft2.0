package ic2.core.block;

import ic2.core.block.invslot.InvSlot;
import net.minecraft.world.Container;

public interface IInventorySlotHolder<P extends TileEntityBlock & Container> {
   P getParent();

   InvSlot getInventorySlot(String var1);

   void addInventorySlot(InvSlot var1);

   int getBaseIndex(InvSlot var1);
}
