package ic2.core.item;

import ic2.api.item.ICustomDamageItem;
import ic2.core.util.ItemStacks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class DamageHandler {
   public static int getDamage(ItemStack stack) {
      Item item = stack.getItem();
      if (item == null) {
         return 0;
      } else {
         return item instanceof ICustomDamageItem ? ((ICustomDamageItem)item).getCustomDamage(stack) : ItemStacks.rawMeta(stack);
      }
   }

   public static void setDamage(ItemStack stack, int damage, boolean displayOnly) {
      Item item = stack.getItem();
      if (item != null) {
         if (item instanceof ICustomDamageItem) {
            ((ICustomDamageItem)item).setCustomDamage(stack, damage);
         } else if (item instanceof IPseudoDamageItem) {
            if (!displayOnly) {
               throw new IllegalStateException("can't damage " + stack + " physically");
            }

            ((IPseudoDamageItem)item).setStackDamage(stack, damage);
         } else {
            ItemStacks.setRawMeta(stack, damage);
         }
      }
   }

   public static int getMaxDamage(ItemStack stack) {
      Item item = stack.getItem();
      if (item == null) {
         return 0;
      } else {
         return item instanceof ICustomDamageItem ? ((ICustomDamageItem)item).getMaxCustomDamage(stack) : stack.getMaxDamage();
      }
   }

   public static boolean damage(ItemStack stack, int damage, LivingEntity src) {
      Item item = stack.getItem();
      if (item == null) {
         return false;
      } else if (item instanceof ICustomDamageItem) {
         return ((ICustomDamageItem)item).applyCustomDamage(stack, damage, src);
      } else if (src == null) {
         if (!stack.isDamageableItem()) {
            return false;
         } else {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (damage > 0 && server != null) {
               damage = EnchantmentHelper.processDurabilityChange(server.overworld(), stack, damage);
            }

            stack.setDamageValue(stack.getDamageValue() + damage);
            return stack.getDamageValue() >= stack.getMaxDamage();
         }
      } else {
         EquipmentSlot slot = EquipmentSlot.MAINHAND;

         for (EquipmentSlot candidate : EquipmentSlot.values()) {
            if (src.getItemBySlot(candidate) == stack) {
               slot = candidate;
               break;
            }
         }

         stack.hurtAndBreak(damage, src, slot);
         return true;
      }
   }
}
