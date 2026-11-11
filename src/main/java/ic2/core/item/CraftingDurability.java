package ic2.core.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class CraftingDurability {
   private CraftingDurability() {
   }

   public static ItemStack remainder(ItemStack input, ServerLevel level) {
      ItemStack result = input.copy();
      if (!result.isDamageableItem()) {
         return result;
      } else {
         int amount = level == null ? 1 : EnchantmentHelper.processDurabilityChange(level, result, 1);
         int damage = result.getDamageValue() + amount;
         if (damage > result.getMaxDamage()) {
            return ItemStack.EMPTY;
         } else {
            result.setDamageValue(damage);
            return result;
         }
      }
   }
}
