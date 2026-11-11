package ic2.core.util;

import ic2.core.block.BlockBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class NativePlacement {
   private NativePlacement() {
   }

   public static BlockPlaceContext context(Level level, final BlockPos pos, Direction face, Player player, InteractionHand hand, float x, float y, float z) {
      return new BlockPlaceContext(
         level, player, hand, player.getItemInHand(hand), new BlockHitResult(new Vec3(pos.getX() + x, pos.getY() + y, pos.getZ() + z), face, pos, false)
      ) {
         public BlockPos getClickedPos() {
            return pos;
         }
      };
   }

   public static BlockState state(Block block, Level level, BlockPos pos, Direction face, Player player, InteractionHand hand, float x, float y, float z) {
      return block instanceof BlockBase legacy
         ? legacy.func_180642_a(level, pos, face, x, y, z, 0, player)
         : block.getStateForPlacement(context(level, pos, face, player, hand, x, y, z));
   }

   public static boolean canPlace(Block block, Level level, BlockPos pos, Direction face, Player player, InteractionHand hand, boolean ignoreCollision) {
      BlockPlaceContext context = context(level, pos, face, player, hand, 0.5F, 0.5F, 0.5F);
      BlockState state = state(block, level, pos, face, player, hand, 0.5F, 0.5F, 0.5F);
      return level.isInWorldBounds(pos)
         && level.getBlockState(pos).canBeReplaced(context)
         && state != null
         && state.canSurvive(level, pos)
         && (ignoreCollision || level.isUnobstructed(state, pos, CollisionContext.of(player)));
   }
}
