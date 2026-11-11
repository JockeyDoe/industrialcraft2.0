package ic2.core.item.tool;

import ic2.api.item.IBoxable;
import ic2.core.IC2;
import ic2.core.audio.PositionSpec;
import ic2.core.block.BlockRubWood;
import ic2.core.item.ItemDefaults;
import ic2.core.item.ItemIC2;
import ic2.core.item.type.MiscResourceType;
import ic2.core.ref.BlockName;
import ic2.core.ref.ItemName;
import ic2.core.util.StackUtil;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ItemTreetap extends ItemIC2 implements IBoxable {
   public ItemTreetap() {
      super(ItemName.treetap);
      ItemDefaults.stackSize(this, 1);
      ItemDefaults.durability(this, 16);
   }

   @Override
   public InteractionResult func_180614_a(
      Player player, Level world, BlockPos pos, InteractionHand hand, Direction side, float xOffset, float yOffset, float zOffset
   ) {
      BlockState state = world.getBlockState(pos);
      Block block = state.getBlock();
      if (block == BlockName.rubber_wood.getInstance()) {
         if (attemptExtract(player, world, pos, side, state, null)) {
            if (!world.isClientSide()) {
               StackUtil.damage(player, hand, StackUtil.anyStack, 1);
            }

            return InteractionResult.SUCCESS;
         } else {
            return InteractionResult.FAIL;
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   public static boolean attemptExtract(Player player, Level world, BlockPos pos, Direction side, BlockState state, List<ItemStack> stacks) {
      assert state.getBlock() == BlockName.rubber_wood.getInstance();

      BlockRubWood.RubberWoodState rwState = (BlockRubWood.RubberWoodState)state.getValue(BlockRubWood.stateProperty);
      if (rwState.isPlain() || rwState.facing != side) {
         return false;
      } else if (rwState.wet) {
         if (!world.isClientSide()) {
            world.setBlock(pos, (BlockState)state.setValue(BlockRubWood.stateProperty, rwState.getDry()), 3);
            if (stacks != null) {
               stacks.add(StackUtil.copyWithSize(ItemName.misc_resource.getItemStack(MiscResourceType.resin), world.getRandom().nextInt(3) + 1));
            } else {
               ejectResin(world, pos, side, world.getRandom().nextInt(3) + 1);
            }

            if (player != null) {
               IC2.achievements.issueAchievement(player, "acquireResin");
            }
         }

         if (world.isClientSide() && player != null) {
            IC2.audioManager.playOnce(player, PositionSpec.Hand, "Tools/Treetap.ogg", true, IC2.audioManager.getDefaultVolume());
         }

         return true;
      } else {
         if (!world.isClientSide() && world.getRandom().nextInt(5) == 0) {
            world.setBlock(pos, (BlockState)state.setValue(BlockRubWood.stateProperty, BlockRubWood.RubberWoodState.plain_y), 3);
         }

         if (world.getRandom().nextInt(5) == 0) {
            if (!world.isClientSide()) {
               ejectResin(world, pos, side, 1);
               if (stacks != null) {
                  stacks.add(ItemName.misc_resource.getItemStack(MiscResourceType.resin));
               } else {
                  ejectResin(world, pos, side, 1);
               }
            }

            if (world.isClientSide() && player != null) {
               IC2.audioManager.playOnce(player, PositionSpec.Hand, "Tools/Treetap.ogg", true, IC2.audioManager.getDefaultVolume());
            }

            return true;
         } else {
            return false;
         }
      }
   }

   private static void ejectResin(Level world, BlockPos pos, Direction side, int quantity) {
      double ejectBias = 0.3;
      double ejectX = pos.getX() + 0.5 + side.getStepX() * 0.3;
      double ejectY = pos.getY() + 0.5 + side.getStepY() * 0.3;
      double ejectZ = pos.getZ() + 0.5 + side.getStepZ() * 0.3;

      for (int i = 0; i < quantity; i++) {
         ItemEntity entityitem = new ItemEntity(world, ejectX, ejectY, ejectZ, ItemName.misc_resource.getItemStack(MiscResourceType.resin));
         entityitem.setDefaultPickUpDelay();
         world.addFreshEntity(entityitem);
      }
   }

   @Override
   public boolean canBeStoredInToolbox(ItemStack itemstack) {
      return true;
   }
}
