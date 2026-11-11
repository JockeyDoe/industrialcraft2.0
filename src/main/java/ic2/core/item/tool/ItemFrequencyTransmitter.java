package ic2.core.item.tool;

import ic2.core.IC2;
import ic2.core.block.machine.tileentity.TileEntityTeleporter;
import ic2.core.init.Localization;
import ic2.core.item.ItemDefaults;
import ic2.core.item.ItemIC2;
import ic2.core.ref.ItemName;
import ic2.core.util.ItemData;
import ic2.core.util.StackUtil;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ItemFrequencyTransmitter extends ItemIC2 {
   private static final String targetSetNbt = "targetSet";
   private static final String targetJustSetNbt = "targetJustSet";
   private static final String targetXNbt = "targetX";
   private static final String targetYNbt = "targetY";
   private static final String targetZNbt = "targetZ";

   public ItemFrequencyTransmitter() {
      super(ItemName.frequency_transmitter);
      ItemDefaults.stackSize(this, 1);
   }

   public InteractionResult use(Level world, Player player, InteractionHand hand) {
      ItemStack stack = StackUtil.get(player, hand);
      if (IC2.platform.isSimulating()) {
         CompoundTag nbtData = StackUtil.getOrCreateNbtData(stack);
         boolean hadJustSet = nbtData.getBooleanOr("targetJustSet", false);
         if (nbtData.getBooleanOr("targetSet", false) && !hadJustSet) {
            nbtData.putBoolean("targetSet", false);
            ItemData.commit(stack, nbtData);
            IC2.platform.messagePlayer(player, "Frequency Transmitter unlinked");
         }

         if (hadJustSet) {
            nbtData.putBoolean("targetJustSet", false);
            ItemData.commit(stack, nbtData);
         }
      }

      return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
   }

   public InteractionResult onItemUseFirst(Player player, Level world, BlockPos pos, Direction side, float hitX, float hitY, float hitZ, InteractionHand hand) {
      if (world.isClientSide()) {
         return InteractionResult.PASS;
      } else if (!(world.getBlockEntity(pos) instanceof TileEntityTeleporter tp)) {
         return InteractionResult.PASS;
      } else {
         CompoundTag nbtData = StackUtil.getOrCreateNbtData(StackUtil.get(player, hand));
         boolean targetSet = nbtData.getBooleanOr("targetSet", false);
         boolean justSetTarget = true;
         BlockPos target = new BlockPos(nbtData.getIntOr("targetX", 0), nbtData.getIntOr("targetY", 0), nbtData.getIntOr("targetZ", 0));
         if (!targetSet) {
            targetSet = true;
            target = tp.getBlockPos();
            IC2.platform.messagePlayer(player, "Frequency Transmitter linked to Teleporter.");
         } else if (tp.getBlockPos().equals(target)) {
            IC2.platform.messagePlayer(player, "Can't link Teleporter to itself.");
         } else if (tp.hasTarget() && tp.getTarget().equals(target)) {
            IC2.platform.messagePlayer(player, "Teleportation link unchanged.");
         } else {
            BlockEntity targetTe = world.getBlockEntity(target);
            if (targetTe instanceof TileEntityTeleporter) {
               tp.setTarget(target);
               ((TileEntityTeleporter)targetTe).setTarget(pos);
               IC2.platform.messagePlayer(player, "Teleportation link established.");
            } else {
               justSetTarget = false;
               targetSet = false;
            }
         }

         nbtData.putBoolean("targetSet", targetSet);
         ItemData.commit(StackUtil.get(player, hand), nbtData);
         nbtData.putBoolean("targetJustSet", justSetTarget);
         ItemData.commit(StackUtil.get(player, hand), nbtData);
         nbtData.putInt("targetX", target.getX());
         ItemData.commit(StackUtil.get(player, hand), nbtData);
         nbtData.putInt("targetY", target.getY());
         ItemData.commit(StackUtil.get(player, hand), nbtData);
         nbtData.putInt("targetZ", target.getZ());
         ItemData.commit(StackUtil.get(player, hand), nbtData);
         return InteractionResult.SUCCESS;
      }
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void func_77624_a(ItemStack stack, Level world, List<String> tooltip, TooltipFlag advanced) {
      CompoundTag nbtData = StackUtil.getOrCreateNbtData(stack);
      if (nbtData.getBooleanOr("targetSet", false)) {
         tooltip.add(
            Localization.translate(
               "ic2.frequency_transmitter.tooltip.target", nbtData.getIntOr("targetX", 0), nbtData.getIntOr("targetY", 0), nbtData.getIntOr("targetZ", 0)
            )
         );
      } else {
         tooltip.add(Localization.translate("ic2.frequency_transmitter.tooltip.blank"));
      }
   }
}
