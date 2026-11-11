package ic2.core.block.wiring;

import ic2.core.profile.NotClassic;
import ic2.core.slot.ArmorSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@NotClassic
public class TileEntityChargepadBatBox extends TileEntityChargepadBlock {
   public TileEntityChargepadBatBox() {
      super(1, 32, 40000);
   }

   @Override
   protected void getItems(Player player) {
      if (player != null) {
         for (ItemStack current : ArmorSlot.getEquipment(player.getInventory())) {
            if (current != null) {
               this.chargeItem(current, 32);
            }
         }

         for (ItemStack currentx : player.getInventory().getNonEquipmentItems()) {
            if (currentx != null) {
               this.chargeItem(currentx, 32);
            }
         }
      }
   }
}
