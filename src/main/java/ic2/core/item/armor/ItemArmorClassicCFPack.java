package ic2.core.item.armor;

import ic2.core.item.ItemDefaults;
import ic2.core.ref.ItemName;
import ic2.core.util.ItemStacks;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ItemArmorClassicCFPack extends ItemArmorUtility {
   public ItemArmorClassicCFPack() {
      super(ItemName.cf_pack, "batpack", EquipmentSlot.CHEST);
      ItemDefaults.durability(this, 260);
   }

   public boolean getCFPellet(Player player, ItemStack pack) {
      if (ItemStacks.rawMeta(pack) > 0) {
         ItemStacks.setRawMeta(pack, pack.getDamageValue() - 1);
         return true;
      } else {
         return false;
      }
   }

   public double getDurabilityForDisplay(ItemStack stack) {
      return 1.0 - (double)stack.getDamageValue() / stack.getMaxDamage();
   }

   public boolean isBarVisible(ItemStack stack) {
      return stack.getDamageValue() > 0;
   }

   public int getBarWidth(ItemStack stack) {
      return Math.round(13.0F * stack.getDamageValue() / stack.getMaxDamage());
   }

   public int getBarColor(ItemStack stack) {
      return Mth.hsvToRgb((float)stack.getDamageValue() / stack.getMaxDamage() / 3.0F, 1.0F, 1.0F);
   }
}
