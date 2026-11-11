package ic2.core.block.kineticgenerator.container;

import ic2.core.ContainerFullInv;
import ic2.core.block.kineticgenerator.tileentity.TileEntityElectricKineticGenerator;
import ic2.core.slot.SlotInvSlot;
import net.minecraft.world.entity.player.Player;

public class ContainerElectricKineticGenerator extends ContainerFullInv<TileEntityElectricKineticGenerator> {
   public ContainerElectricKineticGenerator(Player player, TileEntityElectricKineticGenerator tileEntity1) {
      super(player, tileEntity1, 166);

      for (int i = 0; i < 5; i++) {
         this.addSlot(new SlotInvSlot(tileEntity1.slotMotor, i, 44 + i * 18, 27));
      }

      for (int i = 5; i < 10; i++) {
         this.addSlot(new SlotInvSlot(tileEntity1.slotMotor, i, 44 + (i - 5) * 18, 45));
      }

      this.addSlot(new SlotInvSlot(tileEntity1.dischargeSlot, 0, 8, 62));
   }
}
