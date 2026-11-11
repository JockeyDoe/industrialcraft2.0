package ic2.core.block.steam;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface IMultiBlockController {
   Level getWorld();

   BlockPos getPos();

   boolean isInvalid();

   boolean hasValidStructure();

   boolean isFormed();
}
