package ic2.core.item.tool;

import ic2.api.item.IBoxable;
import ic2.api.tile.IWrenchable;
import ic2.core.IC2;
import ic2.core.audio.PositionSpec;
import ic2.core.init.MainConfig;
import ic2.core.item.ItemDefaults;
import ic2.core.item.ItemIC2;
import ic2.core.ref.ItemName;
import ic2.core.util.ConfigUtil;
import ic2.core.util.LogCategory;
import ic2.core.util.NativeRotation;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

public class ItemToolWrench extends ItemIC2 implements IBoxable {
   private static final boolean logEmptyWrenchDrops = ConfigUtil.getBool(MainConfig.get(), "debug/logEmptyWrenchDrops");

   public ItemToolWrench() {
      this(ItemName.wrench);
   }

   protected ItemToolWrench(ItemName name) {
      super(name);
      ItemDefaults.durability(this, 120);
      ItemDefaults.stackSize(this, 1);
   }

   public boolean canTakeDamage(ItemStack stack, int amount) {
      return true;
   }

   public InteractionResult onItemUseFirst(Player player, Level world, BlockPos pos, Direction side, float hitX, float hitY, float hitZ, InteractionHand hand) {
      ItemStack stack = StackUtil.get(player, hand);
      if (!this.canTakeDamage(stack, 1)) {
         return InteractionResult.FAIL;
      } else {
         ItemToolWrench.WrenchResult result = wrenchBlock(world, pos, side, player, this.canTakeDamage(stack, 10));
         if (result != ItemToolWrench.WrenchResult.Nothing) {
            if (!world.isClientSide()) {
               this.damage(stack, result == ItemToolWrench.WrenchResult.Rotated ? 1 : 10, player);
            } else {
               IC2.audioManager.playOnce(player, PositionSpec.Hand, "Tools/wrench.ogg", true, IC2.audioManager.getDefaultVolume());
            }

            return (InteractionResult)(world.isClientSide() ? InteractionResult.PASS : InteractionResult.SUCCESS);
         } else {
            return InteractionResult.FAIL;
         }
      }
   }

   public static ItemToolWrench.WrenchResult wrenchBlock(Level world, BlockPos pos, Direction side, Player player, boolean remove) {
      BlockState state = Util.getBlockState(world, pos);
      Block block = state.getBlock();
      if (state.isAir()) {
         return ItemToolWrench.WrenchResult.Nothing;
      } else {
         if (block instanceof IWrenchable wrenchable) {
            Direction currentFacing = wrenchable.getFacing(world, pos);
            Direction newFacing = currentFacing;
            if (!IC2.keyboard.isAltKeyDown(player)) {
               if (player.isShiftKeyDown()) {
                  newFacing = side.getOpposite();
               } else {
                  newFacing = side;
               }
            } else {
               Axis axis = side.getAxis();
               if ((side.getAxisDirection() != AxisDirection.POSITIVE || player.isShiftKeyDown())
                  && (side.getAxisDirection() != AxisDirection.NEGATIVE || !player.isShiftKeyDown())) {
                  for (int i = 0; i < 3; i++) {
                     newFacing = newFacing.getClockWise(axis);
                  }
               } else {
                  newFacing = currentFacing.getClockWise(axis);
               }
            }

            if (newFacing != currentFacing && wrenchable.setFacing(world, pos, newFacing, player)) {
               return ItemToolWrench.WrenchResult.Rotated;
            }

            if (remove && wrenchable.wrenchCanRemove(world, pos, player)) {
               if (!world.isClientSide()) {
                  BlockEntity te = world.getBlockEntity(pos);
                  if (ConfigUtil.getBool(MainConfig.get(), "protection/wrenchLogging")) {
                     String playerName = player.getGameProfile().name() + "/" + player.getGameProfile().id();
                     IC2.log
                        .info(
                           LogCategory.PlayerActivity,
                           "Player %s used a wrench to remove the block %s (te %s) at %s.",
                           playerName,
                           state,
                           getTeName(te),
                           Util.formatPosition(world, pos)
                        );
                  }

                  int experience;
                  if (player instanceof ServerPlayer) {
                     if (((BreakBlockEvent)NeoForge.EVENT_BUS.post(new BreakBlockEvent(world, pos, state, player))).isCanceled()) {
                        return ItemToolWrench.WrenchResult.Nothing;
                     }

                     experience = state.getExpDrop(world, pos, te, player, player.getMainHandItem());
                  } else {
                     experience = 0;
                  }

                  block.playerWillDestroy(world, pos, state, player);
                  if (!state.onDestroyedByPlayer(world, pos, player, player.getMainHandItem(), true, state.getFluidState())) {
                     return ItemToolWrench.WrenchResult.Nothing;
                  }

                  block.destroy(world, pos, state);
                  List<ItemStack> drops = wrenchable.getWrenchDrops(world, pos, state, te, player, 0);
                  if (drops != null && !drops.isEmpty()) {
                     for (ItemStack stack : drops) {
                        StackUtil.dropAsEntity(world, pos, stack);
                     }
                  } else if (logEmptyWrenchDrops) {
                     IC2.log
                        .warn(
                           LogCategory.General,
                           "The block %s (te %s) at %s didn't yield any wrench drops.",
                           state,
                           getTeName(te),
                           Util.formatPosition(world, pos)
                        );
                  }

                  if (!player.getAbilities().instabuild && experience > 0) {
                     ExperienceOrb.award((ServerLevel)world, Vec3.atCenterOf(pos), experience);
                  }
               }

               return ItemToolWrench.WrenchResult.Removed;
            }
         } else if (NativeRotation.rotate(world, pos, side)) {
            return ItemToolWrench.WrenchResult.Rotated;
         }

         return ItemToolWrench.WrenchResult.Nothing;
      }
   }

   private static String getTeName(Object te) {
      return te != null ? te.getClass().getSimpleName().replace("TileEntity", "") : "none";
   }

   public void damage(ItemStack is, int damage, Player player) {
      is.hurtAndBreak(damage, player, player.getOffhandItem() == is ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
   }

   @Override
   public boolean canBeStoredInToolbox(ItemStack itemstack) {
      return true;
   }

   public boolean func_82789_a(ItemStack toRepair, ItemStack repair) {
      return repair != null && Util.matchesOD(repair, "ingotBronze");
   }

   private static enum WrenchResult {
      Rotated,
      Removed,
      Nothing;
   }
}
