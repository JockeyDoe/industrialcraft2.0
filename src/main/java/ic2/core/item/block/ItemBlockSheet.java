package ic2.core.item.block;

import ic2.core.block.BlockSheet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ItemBlockSheet extends ItemBlockMulti {
   public ItemBlockSheet(Block block) {
      super(block);
   }

   @Override
   public boolean placeBlockAt(
      ItemStack stack, Player player, Level world, BlockPos pos, Direction side, float hitX, float hitY, float hitZ, BlockState newState
   ) {
      return !((BlockSheet)this.getBlock()).canReplace(world, pos, side, stack)
         ? false
         : super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState);
   }
}
