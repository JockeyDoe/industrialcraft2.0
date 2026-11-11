package ic2.core.item.tfbp;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

abstract class TerraformerBase {
   abstract boolean terraform(Level var1, BlockPos var2);

   void init() {
   }
}
