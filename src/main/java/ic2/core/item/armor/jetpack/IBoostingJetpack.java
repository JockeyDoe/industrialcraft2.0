package ic2.core.item.armor.jetpack;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface IBoostingJetpack extends IJetpack {
   float getBaseThrust(ItemStack var1, boolean var2);

   float getBoostThrust(Player var1, ItemStack var2, boolean var3);

   boolean useBoostPower(ItemStack var1, float var2);

   float getHoverBoost(Player var1, ItemStack var2, boolean var3);
}
