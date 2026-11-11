package ic2.core.item.armor;

import ic2.api.item.ElectricItem;
import ic2.core.block.generator.tileentity.TileEntitySolarGenerator;
import ic2.core.item.ItemDefaults;
import ic2.core.ref.ItemName;
import ic2.core.slot.ArmorSlot;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemArmorSolarHelmet extends ItemArmorUtility {
   public ItemArmorSolarHelmet() {
      super(ItemName.solar_helmet, "solar", EquipmentSlot.HEAD);
      ItemDefaults.durability(this, 0);
   }

   public void onArmorTick(Level world, Player player, ItemStack stack) {
      boolean ret = false;
      if (ArmorSlot.getEquipment(player.getInventory()).get(2) != null) {
         double chargeAmount = TileEntitySolarGenerator.getSkyLight(player.level(), player.blockPosition());
         if (chargeAmount > 0.0) {
            ret = ElectricItem.manager.charge(ArmorSlot.getEquipment(player.getInventory()).get(2), chargeAmount, Integer.MAX_VALUE, true, false) > 0.0;
         }
      }

      if (ret) {
         player.inventoryMenu.broadcastChanges();
      }
   }

   @Override
   public int func_77619_b() {
      return 0;
   }
}
