package ic2.core.block;

import ic2.core.IC2;
import ic2.core.entity.Ic2EntityTypes;
import ic2.core.item.tool.ItemToolWrench;
import ic2.core.ref.BlockName;
import ic2.core.ref.TeBlock;
import ic2.core.util.StackUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EntityNuke extends EntityIC2Explosive {
   public EntityNuke(Level world, double x, double y, double z, float power, int radiationRange) {
      super(Ic2EntityTypes.get("nuke"), world, x, y, z, 300, power, 0.05F, 1.5F, BlockName.te.getBlockState(TeBlock.nuke), radiationRange);
   }

   public EntityNuke(Level world) {
      this(world, 0.0, 0.0, 0.0, 0.0F, 0);
   }

   public boolean func_184230_a(Player player, InteractionHand hand) {
      ItemStack stack = StackUtil.get(player, hand);
      if (IC2.platform.isSimulating() && !StackUtil.isEmpty(stack) && stack.getItem() instanceof ItemToolWrench) {
         ItemToolWrench wrench = (ItemToolWrench)stack.getItem();
         if (wrench.canTakeDamage(stack, 1)) {
            wrench.damage(stack, 1, player);
            this.discard();
         }
      }

      return false;
   }
}
