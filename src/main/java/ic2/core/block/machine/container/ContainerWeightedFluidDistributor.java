package ic2.core.block.machine.container;

import ic2.core.ContainerFullInv;
import ic2.core.block.machine.tileentity.TileEntityWeightedFluidDistributor;
import ic2.core.slot.SlotInvSlot;
import java.util.List;
import net.minecraft.world.entity.player.Player;

public class ContainerWeightedFluidDistributor extends ContainerFullInv<TileEntityWeightedFluidDistributor> {
   public static final short HEIGHT = 211;

   public ContainerWeightedFluidDistributor(Player player, TileEntityWeightedFluidDistributor te) {
      super(player, te, 211);
      this.addSlot(new SlotInvSlot(te.inputSlot, 0, 8, 108));
      this.addSlot(new SlotInvSlot(te.OutputSlot, 0, 152, 108));
   }

   @Override
   public List<String> getNetworkedFields() {
      List<String> ret = super.getNetworkedFields();
      ret.add("fluidTank");
      return ret;
   }
}
