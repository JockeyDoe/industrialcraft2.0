package ic2.core.block.personal;

import ic2.core.ContainerFullInv;
import ic2.core.slot.SlotInvSlot;
import java.util.List;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class ContainerEnergyOMatOpen extends ContainerFullInv<TileEntityEnergyOMat> {
   private int lastTier = -1;

   public ContainerEnergyOMatOpen(Player player, TileEntityEnergyOMat tileEntity1) {
      super(player, tileEntity1, 166);
      this.addSlot(new SlotInvSlot(tileEntity1.demandSlot, 0, 24, 17));
      this.addSlot(new SlotInvSlot(tileEntity1.upgradeSlot, 0, 24, 53));
      this.addSlot(new SlotInvSlot(tileEntity1.inputSlot, 0, 60, 17));
      this.addSlot(new SlotInvSlot(tileEntity1.chargeSlot, 0, 60, 53));
   }

   @Override
   public List<String> getNetworkedFields() {
      List<String> ret = super.getNetworkedFields();
      ret.add("paidFor");
      ret.add("euBuffer");
      ret.add("euOffer");
      return ret;
   }

   @Override
   public void broadcastChanges() {
      super.broadcastChanges();

      for (ServerPlayer listener : this.getListeners()) {
         if (this.base.chargeSlot.tier != this.lastTier) {
            listener.connection.send(new ClientboundContainerSetDataPacket(this.containerId, 0, this.base.chargeSlot.tier));
         }
      }

      this.lastTier = this.base.chargeSlot.tier;
   }

   public void setData(int index, int value) {
      super.setData(index, value);
      switch (index) {
         case 0:
            this.base.chargeSlot.tier = value;
      }
   }
}
