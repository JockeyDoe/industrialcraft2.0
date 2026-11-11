package ic2.core.item.block;

import ic2.core.block.EntityIC2Explosive;
import ic2.core.block.EntityItnt;
import ic2.core.ref.BlockName;
import ic2.core.ref.TeBlock;
import ic2.core.util.StackUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

public class BehaviorTeBlockDispense extends DefaultDispenseItemBehavior {
   protected ItemStack execute(BlockSource source, ItemStack stack) {
      if (StackUtil.checkItemEquality(stack, BlockName.te.getItemStack(TeBlock.itnt))) {
         Level world = source.level();
         BlockPos pos = source.pos().relative((Direction)source.state().getValue(DispenserBlock.FACING));

         assert !world.isClientSide();

         EntityIC2Explosive entity = new EntityItnt(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
         world.removeBlock(pos, false);
         world.addFreshEntity(entity);
         world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
         return StackUtil.decSize(stack);
      } else {
         return stack;
      }
   }
}
