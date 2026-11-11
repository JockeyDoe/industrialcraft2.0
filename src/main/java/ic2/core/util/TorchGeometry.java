package ic2.core.util;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

public final class TorchGeometry {
   private TorchGeometry() {
   }

   public static AABB box(Direction facing) {
      return switch (facing) {
         case EAST -> new AABB(0.0, 0.2, 0.35, 0.3, 0.8, 0.65);
         case WEST -> new AABB(0.7, 0.2, 0.35, 1.0, 0.8, 0.65);
         case SOUTH -> new AABB(0.35, 0.2, 0.0, 0.65, 0.8, 0.3);
         case NORTH -> new AABB(0.35, 0.2, 0.7, 0.65, 0.8, 1.0);
         case UP, DOWN -> new AABB(0.4, 0.0, 0.4, 0.6, 0.6, 0.6);
         default -> throw new MatchException(null, null);
      };
   }
}
