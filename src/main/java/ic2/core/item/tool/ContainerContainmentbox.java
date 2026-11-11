package ic2.core.item.tool;

import ic2.core.item.ContainerHandHeldInventory;
import ic2.core.slot.SlotRadioactive;
import net.minecraft.world.entity.player.Player;

public class ContainerContainmentbox extends ContainerHandHeldInventory<HandHeldContainmentbox> {
   protected static final int height = 166;

   public ContainerContainmentbox(Player player, HandHeldContainmentbox box) {
      super(box);

      for (int i = 0; i < 4; i++) {
         this.addSlot(new SlotRadioactive(box, i, 53 + i * 18, 19));
      }

      for (int i = 4; i < 8; i++) {
         this.addSlot(new SlotRadioactive(box, i, 53 + (i - 4) * 18, 37));
      }

      for (int i = 8; i < 12; i++) {
         this.addSlot(new SlotRadioactive(box, i, 53 + (i - 8) * 18, 55));
      }

      this.addPlayerInventorySlots(player, 166);
   }
}
