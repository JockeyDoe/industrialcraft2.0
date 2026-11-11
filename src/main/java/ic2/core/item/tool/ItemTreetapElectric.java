package ic2.core.item.tool;

import ic2.api.item.ElectricItem;
import ic2.core.ref.BlockName;
import ic2.core.ref.ItemName;
import ic2.core.util.StackUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ItemTreetapElectric extends ItemElectricTool {
   public ItemTreetapElectric() {
      super(ItemName.electric_treetap, 50);
      this.maxCharge = 10000;
      this.transferLimit = 100;
      this.tier = 1;
   }

   @Override
   public InteractionResult func_180614_a(Player player, Level world, BlockPos pos, InteractionHand hand, Direction side, float hitX, float hitY, float hitZ) {
      BlockState state = world.getBlockState(pos);
      Block block = state.getBlock();
      ItemStack stack = StackUtil.get(player, hand);
      if (block != BlockName.rubber_wood.getInstance() || !ElectricItem.manager.canUse(stack, this.operationEnergyCost)) {
         return InteractionResult.PASS;
      } else if (ItemTreetap.attemptExtract(player, world, pos, side, state, null)) {
         ElectricItem.manager.use(stack, this.operationEnergyCost, player);
         return InteractionResult.SUCCESS;
      } else {
         return super.func_180614_a(player, world, pos, hand, side, hitX, hitY, hitZ);
      }
   }
}
