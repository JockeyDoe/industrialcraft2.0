package ic2.core.block.wiring;

import ic2.core.profile.NotClassic;
import ic2.core.slot.ArmorSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@NotClassic
public class TileEntityChargepadMFSU extends TileEntityChargepadBlock {
   public TileEntityChargepadMFSU() {
      super(4, 2048, 40000000);
   }

   @Override
   protected void getItems(Player player) {
      for (ItemStack current : ArmorSlot.getEquipment(player.getInventory())) {
         if (current != null) {
            this.chargeItem(current, 2048);
         }
      }

      for (ItemStack currentx : player.getInventory().getNonEquipmentItems()) {
         if (currentx != null) {
            this.chargeItem(currentx, 2048);
         }
      }
   }
}
