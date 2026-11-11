package ic2.core.item.block;

import ic2.core.item.ItemDefaults;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ItemIC2Door extends ItemBlockIC2 {
   public ItemIC2Door(Block block) {
      super(block);
      ItemDefaults.stackSize(this, 8);
   }

   @Override
   public boolean placeBlockAt(
      ItemStack stack, Player player, Level world, BlockPos pos, Direction side, float hitX, float hitY, float hitZ, BlockState newState
   ) {
      if (!world.setBlock(pos, newState, 3)) {
         return false;
      } else {
         this.getBlock().setPlacedBy(world, pos, newState, player, stack);
         return true;
      }
   }
}
