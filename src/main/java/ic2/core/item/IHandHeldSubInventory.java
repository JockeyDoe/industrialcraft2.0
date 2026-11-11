package ic2.core.item;

import ic2.core.IHasGui;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface IHandHeldSubInventory extends IHandHeldInventory {
   IHasGui getSubInventory(Player var1, ItemStack var2, int var3);
}
