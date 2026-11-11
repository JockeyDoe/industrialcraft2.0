package ic2.core.block.machine.container;

import ic2.core.ContainerFullInv;
import ic2.core.block.machine.tileentity.TileEntityFluidDistributor;
import ic2.core.slot.SlotInvSlot;
import java.util.List;
import net.minecraft.world.entity.player.Player;

public class ContainerFluidDistributor extends ContainerFullInv<TileEntityFluidDistributor> {
   public ContainerFluidDistributor(Player player, TileEntityFluidDistributor tileEntite) {
      super(player, tileEntite, 184);
      this.addSlot(new SlotInvSlot(tileEntite.inputSlot, 0, 9, 54));
      this.addSlot(new SlotInvSlot(tileEntite.OutputSlot, 0, 9, 72));
   }

   @Override
   public List<String> getNetworkedFields() {
      List<String> ret = super.getNetworkedFields();
      ret.add("fluidTank");
      return ret;
   }
}
