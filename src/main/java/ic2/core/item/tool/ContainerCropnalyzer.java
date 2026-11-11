package ic2.core.item.tool;

import ic2.core.item.ContainerHandHeldInventory;
import ic2.core.ref.ItemName;
import ic2.core.slot.SlotCustom;
import ic2.core.slot.SlotDischarge;
import net.minecraft.world.entity.player.Player;

public class ContainerCropnalyzer extends ContainerHandHeldInventory<HandHeldCropnalyzer> {
   public ContainerCropnalyzer(Player player, HandHeldCropnalyzer cropnalyzer1) {
      super(cropnalyzer1);
      this.addSlot(new SlotCustom(cropnalyzer1, ItemName.crop_seed_bag.getInstance(), 0, 8, 7));
      this.addSlot(new SlotCustom(cropnalyzer1, null, 1, 41, 7));
      this.addSlot(new SlotDischarge(cropnalyzer1, 2, 152, 7));
      this.addPlayerInventorySlots(player, 223);
   }
}
