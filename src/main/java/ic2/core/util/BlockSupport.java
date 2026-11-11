package ic2.core.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public final class BlockSupport {
   private BlockSupport() {
   }

   public static boolean top(Level level, BlockPos pos, boolean unloadedDefault) {
      return !level.hasChunkAt(pos) ? unloadedDefault : level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP);
   }
}
