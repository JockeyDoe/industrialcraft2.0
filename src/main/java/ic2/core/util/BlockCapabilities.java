package ic2.core.util;

import ic2.core.block.TileEntityBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;

public final class BlockCapabilities {
   public static <T> T get(BlockEntity entity, BlockCapability<T, Direction> capability, Direction side) {
      if (entity == null || entity.isRemoved()) {
         return null;
      } else if (entity instanceof TileEntityBlock machine) {
         return machine.getCapability(capability, side);
      } else {
         Level level = entity.getLevel();
         return (T)(level == null ? null : level.getCapability(capability, entity.getBlockPos(), entity.getBlockState(), entity, side));
      }
   }

   public static boolean has(BlockEntity entity, BlockCapability<?, Direction> capability, Direction side) {
      return get(entity, capability, side) != null;
   }

   public static <T> T cast(BlockCapability<T, Direction> capability, Object value) {
      return (T)(value == null ? null : capability.typeClass().cast(value));
   }
}
