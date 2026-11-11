package ic2.core.block.machine.container;

import ic2.core.block.machine.tileentity.TileEntityFluidBottler;
import ic2.core.slot.SlotInvSlot;
import java.util.List;
import net.minecraft.world.entity.player.Player;

public class ContainerFluidBottler extends ContainerStandardMachine<TileEntityFluidBottler> {
   public ContainerFluidBottler(Player player, TileEntityFluidBottler tileEntity1) {
      super(player, tileEntity1, 184, 8, 53, 0, 0, 117, 53, 152, 26);
      this.addSlot(new SlotInvSlot(tileEntity1.drainInputSlot, 0, 44, 35));
      this.addSlot(new SlotInvSlot(tileEntity1.fillInputSlot, 0, 44, 72));
   }

   @Override
   public List<String> getNetworkedFields() {
      List<String> ret = super.getNetworkedFields();
      ret.add("fluidTank");
      return ret;
   }
}
