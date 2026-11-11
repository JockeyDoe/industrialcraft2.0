package ic2.api.item;

import ic2.core.model.ModelKey;
import net.minecraft.world.item.ItemStack;

public interface ITeBlockSpecialItem {
   boolean doesOverrideDefault(ItemStack var1);

   ModelKey getModelLocation(ItemStack var1);
}
