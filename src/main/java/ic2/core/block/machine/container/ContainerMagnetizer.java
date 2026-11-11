package ic2.core.block.machine.container;

import ic2.core.block.machine.tileentity.TileEntityMagnetizer;
import ic2.core.slot.SlotArmor;
import ic2.core.slot.SlotInvSlot;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

public class ContainerMagnetizer extends ContainerElectricMachine<TileEntityMagnetizer> {
   public final Player player;

   public ContainerMagnetizer(Player player, TileEntityMagnetizer base1) {
      super(player, base1, 166, 8, 44);
      this.player = player;

      for (int i = 0; i < 4; i++) {
         this.addSlot(new SlotInvSlot(base1.upgradeSlot, i, 152, 8 + i * 18));
      }

      this.addSlot(new SlotArmor(player.getInventory(), EquipmentSlot.FEET, 45, 26));
   }
}
