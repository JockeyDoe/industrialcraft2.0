package ic2.core.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class NativeRotation {
   private NativeRotation() {
   }

   public static boolean rotate(Level level, BlockPos pos, Direction face) {
      BlockState old = level.getBlockState(pos);
      BlockState next = old;
      if (old.hasProperty(BlockStateProperties.FACING)) {
         Direction facing = (Direction)old.getValue(BlockStateProperties.FACING);
         if (facing.getAxis() != face.getAxis()) {
            next = (BlockState)old.setValue(
               BlockStateProperties.FACING,
               face.getAxisDirection() == AxisDirection.POSITIVE ? facing.getClockWise(face.getAxis()) : facing.getCounterClockWise(face.getAxis())
            );
         }
      } else if (face.getAxis() == Axis.Y) {
         next = old.rotate(level, pos, face == Direction.UP ? Rotation.CLOCKWISE_90 : Rotation.COUNTERCLOCKWISE_90);
      } else if (old.hasProperty(BlockStateProperties.AXIS)) {
         next = (BlockState)old.cycle(BlockStateProperties.AXIS);
      }

      return next != old && next.canSurvive(level, pos) && level.setBlock(pos, next, 3);
   }
}
