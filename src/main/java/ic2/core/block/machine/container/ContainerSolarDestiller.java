package ic2.core.block.machine.container;

import ic2.core.ContainerFullInv;
import ic2.core.block.machine.tileentity.TileEntitySolarDestiller;
import ic2.core.slot.SlotInvSlot;
import java.util.List;
import net.minecraft.world.entity.player.Player;

public class ContainerSolarDestiller extends ContainerFullInv<TileEntitySolarDestiller> {
   public ContainerSolarDestiller(Player player, TileEntitySolarDestiller tileEntite) {
      super(player, tileEntite, 184);
      this.addSlot(new SlotInvSlot(tileEntite.waterinputSlot, 0, 17, 27));
      this.addSlot(new SlotInvSlot(tileEntite.destiwaterinputSlot, 0, 136, 64));
      this.addSlot(new SlotInvSlot(tileEntite.wateroutputSlot, 0, 17, 45));
      this.addSlot(new SlotInvSlot(tileEntite.destiwateroutputSlott, 0, 136, 82));

      for (int i = 0; i < 2; i++) {
         this.addSlot(new SlotInvSlot(tileEntite.upgradeSlot, i, 152, 8 + i * 18));
      }
   }

   @Override
   public List<String> getNetworkedFields() {
      List<String> ret = super.getNetworkedFields();
      ret.add("skyLight");
      ret.add("inputTank");
      ret.add("outputTank");
      return ret;
   }
}
