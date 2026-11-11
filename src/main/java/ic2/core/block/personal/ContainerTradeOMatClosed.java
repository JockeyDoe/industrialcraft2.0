package ic2.core.block.personal;

import ic2.core.ContainerFullInv;
import ic2.core.slot.SlotInvSlot;
import ic2.core.slot.SlotInvSlotReadOnly;
import java.util.List;
import net.minecraft.world.entity.player.Player;

public class ContainerTradeOMatClosed extends ContainerFullInv<TileEntityTradeOMat> {
   public ContainerTradeOMatClosed(Player player, TileEntityTradeOMat tileEntity1) {
      super(player, tileEntity1, 166);
      this.addSlot(new SlotInvSlotReadOnly(tileEntity1.demandSlot, 0, 50, 19));
      this.addSlot(new SlotInvSlotReadOnly(tileEntity1.offerSlot, 0, 50, 38));
      this.addSlot(new SlotInvSlot(tileEntity1.inputSlot, 0, 143, 17));
      this.addSlot(new SlotInvSlot(tileEntity1.outputSlot, 0, 143, 53));
   }

   @Override
   public List<String> getNetworkedFields() {
      List<String> ret = super.getNetworkedFields();
      ret.add("stock");
      return ret;
   }
}
