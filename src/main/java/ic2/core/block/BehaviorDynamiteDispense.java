package ic2.core.block;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;

public class BehaviorDynamiteDispense extends DefaultDispenseItemBehavior {
   private final boolean sticky;

   public BehaviorDynamiteDispense(boolean sticky) {
      this.sticky = sticky;
   }

   protected ItemStack execute(BlockSource source, ItemStack stack) {
      Position pos = DispenserBlock.getDispensePosition(source);
      Direction facing = (Direction)source.state().getValue(DispenserBlock.FACING);
      EntityDynamite entity = (EntityDynamite)(this.sticky
         ? new EntityStickyDynamite(source.level(), pos.x(), pos.y(), pos.z())
         : new EntityDynamite(source.level(), pos.x(), pos.y(), pos.z()));
      entity.func_70186_c(facing.getStepX(), facing.getStepY() + 0.1, facing.getStepZ(), 1.1F, 6.0F);
      source.level().addFreshEntity(entity);
      stack.shrink(1);
      return stack;
   }

   protected void playSound(BlockSource source) {
      source.level().levelEvent(1002, source.pos(), 0);
   }
}
