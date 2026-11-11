package ic2.core.block.machine.container;

import ic2.core.ContainerFullInv;
import ic2.core.block.machine.tileentity.TileEntityClassicCropmatron;
import ic2.core.slot.SlotInvSlot;
import net.minecraft.world.entity.player.Player;

public class ContainerClassicCropmatron extends ContainerFullInv<TileEntityClassicCropmatron> {
   public ContainerClassicCropmatron(Player player, TileEntityClassicCropmatron base) {
      super(player, base, 166);

      for (int i = 0; i < base.fertilizerSlot.size(); i++) {
         this.addSlot(new SlotInvSlot(base.fertilizerSlot, i, 62, 20 + i * 18));
      }

      for (int i = 0; i < base.hydrationSlot.size(); i++) {
         this.addSlot(new SlotInvSlot(base.hydrationSlot, i, 98, 20 + i * 18));
      }

      for (int i = 0; i < base.weedExSlot.size(); i++) {
         this.addSlot(new SlotInvSlot(base.weedExSlot, i, 134, 20 + i * 18));
      }
   }
}
