package ic2.core.block.kineticgenerator.container;

import ic2.core.ContainerFullInv;
import ic2.core.block.kineticgenerator.tileentity.TileEntitySteamKineticGenerator;
import ic2.core.slot.SlotInvSlot;
import java.util.List;
import net.minecraft.world.entity.player.Player;

public class ContainerSteamKineticGenerator extends ContainerFullInv<TileEntitySteamKineticGenerator> {
   public ContainerSteamKineticGenerator(Player player, TileEntitySteamKineticGenerator te) {
      super(player, te, 166);
      this.addSlot(new SlotInvSlot(te.upgradeSlot, 0, 152, 26));
      this.addSlot(new SlotInvSlot(te.turbineSlot, 0, 80, 26));
   }

   @Override
   public List<String> getNetworkedFields() {
      List<String> ret = super.getNetworkedFields();
      ret.add("distilledWaterTank");
      ret.add("kUoutput");
      ret.add("ventingSteam");
      ret.add("throttled");
      ret.add("isTurbineFilledWithWater");
      return ret;
   }
}
