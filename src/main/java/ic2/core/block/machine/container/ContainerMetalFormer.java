package ic2.core.block.machine.container;

import ic2.core.block.machine.tileentity.TileEntityMetalFormer;
import java.util.List;
import net.minecraft.world.entity.player.Player;

public class ContainerMetalFormer extends ContainerStandardMachine<TileEntityMetalFormer> {
   public ContainerMetalFormer(Player player, TileEntityMetalFormer tileEntity1) {
      super(player, tileEntity1, 166, 17, 53, 17, 17, 116, 35, 152, 8);
   }

   @Override
   public List<String> getNetworkedFields() {
      List<String> ret = super.getNetworkedFields();
      ret.add("mode");
      return ret;
   }
}
