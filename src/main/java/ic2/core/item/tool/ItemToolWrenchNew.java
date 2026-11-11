package ic2.core.item.tool;

import ic2.api.item.IEnhancedOverlayProvider;
import ic2.api.tile.IWrenchable;
import ic2.api.transport.IPipe;
import ic2.core.IC2;
import ic2.core.IHitSoundOverride;
import ic2.core.audio.PositionSpec;
import ic2.core.item.ItemDefaults;
import ic2.core.item.ItemToolIC2;
import ic2.core.ref.ItemName;
import ic2.core.util.RotationUtil;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
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

public class ItemToolWrenchNew extends ItemToolIC2 implements IEnhancedOverlayProvider, IHitSoundOverride {
   public ItemToolWrenchNew() {
      super(ItemName.wrench_new, HarvestLevel.Iron, EnumSet.of(ToolClass.Wrench));
      ItemDefaults.durability(this, 120);
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
         } else if (world.getBlockEntity(pos) instanceof IPipe) {
            IPipe target = (IPipe)world.getBlockEntity(pos);
            Direction newFacing = RotationUtil.rotateByHit(side, hitX, hitY, hitZ);

            assert target != null;

            target.flipConnection(newFacing);
            if (world.getBlockEntity(pos.relative(newFacing)) instanceof IPipe) {
               IPipe other = (IPipe)world.getBlockEntity(pos.relative(newFacing));

               assert other != null;

               if (target.isConnected(newFacing) != other.isConnected(newFacing.getOpposite())) {
                  other.flipConnection(newFacing.getOpposite());
               }
            }

            if (world.isClientSide()) {
               IC2.audioManager.playOnce(player, PositionSpec.Hand, "Tools/wrench.ogg", true, IC2.audioManager.getDefaultVolume());
            }

            return InteractionResult.SUCCESS;
         } else if (block instanceof IWrenchable wrenchable) {
            Direction newFacing = RotationUtil.rotateByHit(side, hitX, hitY, hitZ);
            wrenchable.setFacing(world, pos, newFacing, player);
            if (world.isClientSide()) {
               IC2.audioManager.playOnce(player, PositionSpec.Hand, "Tools/wrench.ogg", true, IC2.audioManager.getDefaultVolume());
            }

            return InteractionResult.SUCCESS;
         } else {
            return InteractionResult.FAIL;
         }
      }
   }

   public boolean func_82789_a(ItemStack toRepair, ItemStack repair) {
      return Util.matchesOD(repair, "ingotBronze");
   }

   public boolean func_77616_k(ItemStack stack) {
      return false;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void func_77624_a(ItemStack stack, @Nullable Level worldIn, List<String> info, TooltipFlag flagIn) {
      info.add(Minecraft.getInstance().options.keyAttack.getDisplayName() + ":");
      info.add(" Safely mine IC2 machines (Yes you will get the machine and not the machine block)");
      info.add("");
      info.add(Minecraft.getInstance().options.keyShift.getDisplayName() + ":");
      info.add(" Set the machine facing (rotate)");
      info.add(" Connect pipes together and to covers");
   }

   @Override
   public boolean providesEnhancedOverlay(Level world, BlockPos pos, Direction side, Player player, ItemStack stack) {
      Block block = world.getBlockState(pos).getBlock();
      if (!(block instanceof IWrenchable)) {
         return false;
      } else {
         BlockEntity tileEntity = world.getBlockEntity(pos);
         return tileEntity instanceof IPipe || Arrays.stream(Direction.values()).anyMatch(face -> ((IWrenchable)block).canSetFacing(world, pos, face, player));
      }
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public String getHitSoundForBlock(LocalPlayer player, Level world, BlockPos pos, ItemStack stack) {
      return "";
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public String getBreakSoundForBlock(LocalPlayer player, Level world, BlockPos pos, ItemStack stack) {
      if (player.getAbilities().instabuild) {
         return null;
      } else {
         BlockState state = world.getBlockState(pos);
         return state.getBlock() instanceof IWrenchable ? "Tools/wrench.ogg" : null;
      }
   }
}
