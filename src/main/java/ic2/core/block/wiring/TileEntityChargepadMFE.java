package ic2.core.block.wiring;

import ic2.core.profile.NotClassic;
import ic2.core.slot.ArmorSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@NotClassic
public class TileEntityChargepadMFE extends TileEntityChargepadBlock {
   public TileEntityChargepadMFE() {
      super(3, 512, 4000000);
   }

   @Override
   protected void getItems(Player player) {
      if (player != null) {
         for (ItemStack current : ArmorSlot.getEquipment(player.getInventory())) {
            if (current != null) {
               this.chargeItem(current, 512);
            }
         }

         for (ItemStack currentx : player.getInventory().getNonEquipmentItems()) {
            if (currentx != null) {
               this.chargeItem(currentx, 512);
            }
         }
      }
   }
}
