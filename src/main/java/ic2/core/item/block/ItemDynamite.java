package ic2.core.item.block;

import ic2.api.item.IBoxable;
import ic2.core.IC2;
import ic2.core.block.BehaviorDynamiteDispense;
import ic2.core.block.EntityDynamite;
import ic2.core.block.EntityStickyDynamite;
import ic2.core.item.ItemDefaults;
import ic2.core.item.ItemIC2;
import ic2.core.ref.BlockName;
import ic2.core.ref.ItemName;
import ic2.core.util.NativePlacement;
import ic2.core.util.StackUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;

public class ItemDynamite extends ItemIC2 implements IBoxable {
   public boolean sticky;

   public ItemDynamite(ItemName name) {
      super(name);
      this.sticky = name == ItemName.dynamite_sticky;
      ItemDefaults.stackSize(this, 16);
      DispenserBlock.registerBehavior(this, new BehaviorDynamiteDispense(this.sticky));
   }

   public int func_77647_b(int i) {
      return i;
   }

   @Override
   public InteractionResult func_180614_a(Player player, Level world, BlockPos pos, InteractionHand hand, Direction side, float a, float b, float c) {
      if (this.sticky) {
         return InteractionResult.PASS;
      } else {
         pos = pos.relative(side);
         BlockState state = world.getBlockState(pos);
         Block dynamite = BlockName.dynamite.getInstance();
         if (state.isAir() && NativePlacement.canPlace(dynamite, world, pos, side, player, hand, false)) {
            world.setBlock(pos, NativePlacement.state(dynamite, world, pos, side, player, hand, a, b, c), 3);
            StackUtil.consumeOrError(player, hand, 1);
            return InteractionResult.SUCCESS;
         } else {
            return InteractionResult.FAIL;
         }
      }
   }

   public InteractionResult use(Level world, Player player, InteractionHand hand) {
      ItemStack stack = StackUtil.get(player, hand);
      if (!player.getAbilities().instabuild) {
         stack = StackUtil.decSize(stack);
      }

      world.playSound(player, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
      if (IC2.platform.isSimulating()) {
         if (this.sticky) {
            world.addFreshEntity(new EntityStickyDynamite(world, player));
         } else {
            world.addFreshEntity(new EntityDynamite(world, player));
         }
      }

      return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
   }

   @Override
   public boolean canBeStoredInToolbox(ItemStack itemstack) {
      return true;
   }
}
