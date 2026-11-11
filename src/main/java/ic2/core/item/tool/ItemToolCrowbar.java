package ic2.core.item.tool;

import ic2.api.item.IEnhancedOverlayProvider;
import ic2.core.IC2;
import ic2.core.audio.PositionSpec;
import ic2.core.block.transport.cover.ICoverHolder;
import ic2.core.init.Localization;
import ic2.core.item.ItemDefaults;
import ic2.core.item.ItemToolIC2;
import ic2.core.ref.ItemName;
import ic2.core.util.RotationUtil;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ItemToolCrowbar extends ItemToolIC2 implements IEnhancedOverlayProvider {
   public ItemToolCrowbar() {
      super(ItemName.crowbar, HarvestLevel.Iron, EnumSet.of(ToolClass.Crowbar));
      ItemDefaults.durability(this, 250);
   }

   public boolean canTakeDamage(ItemStack stack, int amount) {
      return true;
   }

   public InteractionResult onItemUseFirst(Player player, Level world, BlockPos pos, Direction side, float hitX, float hitY, float hitZ, InteractionHand hand) {
      ItemStack stack = StackUtil.get(player, hand);
      if (!this.canTakeDamage(stack, 1)) {
         return InteractionResult.FAIL;
      } else {
         BlockState state = Util.getBlockState(world, pos);
         Block block = state.getBlock();
         if (state.isAir()) {
            return InteractionResult.FAIL;
         } else if (world.getBlockEntity(pos) instanceof ICoverHolder) {
            ICoverHolder target = (ICoverHolder)world.getBlockEntity(pos);
            Direction selectedFacing = RotationUtil.rotateByHit(side, hitX, hitY, hitZ);
            if (target.canRemoveCover(world, pos, selectedFacing)) {
               if (!world.isClientSide()) {
                  target.removeCover(world, pos, selectedFacing);
                  stack.hurtAndBreak(1, player, hand);
               } else {
                  IC2.audioManager.playOnce(player, PositionSpec.Hand, "Tools/Crowbar.ogg", true, IC2.audioManager.getDefaultVolume());
                  IC2.platform.messagePlayer(player, Localization.translate("Attachment removed"));
               }
            }

            return (InteractionResult)(world.isClientSide() ? InteractionResult.PASS : InteractionResult.SUCCESS);
         } else {
            return InteractionResult.FAIL;
         }
      }
   }

   public boolean func_82789_a(ItemStack toRepair, ItemStack repair) {
      return repair != null && Util.matchesOD(repair, "ingotBronze");
   }

   public boolean func_77616_k(ItemStack stack) {
      return false;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void func_77624_a(ItemStack stack, @Nullable Level worldIn, List<String> info, TooltipFlag flagIn) {
      info.add(Minecraft.getInstance().options.keyShift.getDisplayName() + ":");
      info.add(" Remove attachments from blocks");
   }

   @Override
   public boolean providesEnhancedOverlay(Level world, BlockPos pos, Direction side, Player player, ItemStack stack) {
      BlockEntity tileEntity = world.getBlockEntity(pos);
      return tileEntity instanceof ICoverHolder;
   }
}
