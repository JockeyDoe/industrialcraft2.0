package ic2.core.block.reactor.tileentity;

import ic2.core.profile.NotClassic;
import ic2.core.ref.ItemName;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

@NotClassic
public class TileEntityRCI_RSH extends TileEntityAbstractRCI {
   public TileEntityRCI_RSH() {
      super(ItemName.rsh_condensator.getItemStack(), new ItemStack(Blocks.REDSTONE_BLOCK));
   }
}
