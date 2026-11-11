package ic2.core.block.machine.container;

import ic2.core.block.machine.tileentity.TileEntityClassicCanner;
import ic2.core.slot.SlotInvSlot;
import java.util.List;
import net.minecraft.world.entity.player.Player;

public class ContainerClassicCanner extends ContainerElectricMachine<TileEntityClassicCanner> {
   public ContainerClassicCanner(Player player, TileEntityClassicCanner base) {
      super(player, base, 166, 30, 45);
      this.addSlot(new SlotInvSlot(base.resInputSlot, 0, 69, 17));
      this.addSlot(new SlotInvSlot(base.outputSlot, 0, 119, 35));
      this.addSlot(new SlotInvSlot(base.inputSlot, 0, 69, 53));
   }

   @Override
   public List<String> getNetworkedFields() {
      List<String> ret = super.getNetworkedFields();
      ret.add("progress");
      ret.add("mode");
      return ret;
   }
}
