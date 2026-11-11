package ic2.core.block.wiring;

import ic2.core.ContainerFullInv;
import java.util.List;
import net.minecraft.world.entity.player.Player;

public class ContainerTransformer extends ContainerFullInv<TileEntityTransformer> {
   public ContainerTransformer(Player player, TileEntityTransformer tileEntity1, int height) {
      super(player, tileEntity1, height);
   }

   @Override
   public List<String> getNetworkedFields() {
      List<String> ret = super.getNetworkedFields();
      ret.add("configuredMode");
      ret.add("inputFlow");
      ret.add("outputFlow");
      return ret;
   }
}
