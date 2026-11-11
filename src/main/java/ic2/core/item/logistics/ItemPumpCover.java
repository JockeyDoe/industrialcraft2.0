package ic2.core.item.logistics;

import ic2.api.item.IEnhancedOverlayProvider;
import ic2.core.IC2;
import ic2.core.block.transport.cover.CoverProperty;
import ic2.core.block.transport.cover.CoverRegistry;
import ic2.core.block.transport.cover.ICoverHolder;
import ic2.core.block.transport.cover.IFluidConsumingCover;
import ic2.core.init.Localization;
import ic2.core.item.ItemDefaults;
import ic2.core.item.ItemMulti;
import ic2.core.ref.ItemName;
import ic2.core.util.ItemStacks;
import ic2.core.util.LiquidUtil;
import ic2.core.util.RotationUtil;
import ic2.core.util.StackUtil;
import java.util.List;
import java.util.Set;
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
import net.neoforged.neoforge.fluids.FluidStack;

public class ItemPumpCover extends ItemMulti<PumpCoverType> implements IFluidConsumingCover, IEnhancedOverlayProvider {
   public ItemPumpCover() {
      super(ItemName.cover, PumpCoverType.class);
      ItemDefaults.variants(this, true);
   }

   public void initializeCovers() {
      for (PumpCoverType type : PumpCoverType.values()) {
         CoverRegistry.register(ItemStacks.create(this, 1, type.getId()));
      }
   }

   @Override
   public InteractionResult func_180614_a(
      Player player, Level world, BlockPos pos, InteractionHand hand, Direction side, float xOffset, float yOffset, float zOffset
   ) {
      ItemStack stack = StackUtil.get(player, hand);
      PumpCoverType type = this.getType(stack);
      if (type == null) {
         return InteractionResult.PASS;
      } else {
         BlockEntity tileEntity = world.getBlockEntity(pos);
         if (!(tileEntity instanceof ICoverHolder)) {
            return InteractionResult.PASS;
         } else {
            Direction selectedFacing = RotationUtil.rotateByHit(side, xOffset, yOffset, zOffset);
            if (((ICoverHolder)tileEntity).canPlaceCover(world, pos, selectedFacing, stack)) {
               if (!world.isClientSide()) {
                  ((ICoverHolder)tileEntity).placeCover(world, pos, selectedFacing, StackUtil.copyWithSize(stack, 1));
                  stack.shrink(1);
               } else {
                  IC2.platform.messagePlayer(player, Localization.translate("Attachment placed"));
               }
            }

            return (InteractionResult)(world.isClientSide() ? InteractionResult.PASS : InteractionResult.SUCCESS);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void func_77624_a(ItemStack stack, Level world, List<String> tooltip, TooltipFlag advanced) {
      super.func_77624_a(stack, world, tooltip, advanced);
      PumpCoverType type = this.getType(stack);
      if (type != null) {
         tooltip.add("Transfer rate: " + type.transferRate + " mb/sec (as attachment)");
      }
   }

   @Override
   public boolean isSuitableFor(ItemStack stack, Set<CoverProperty> types) {
      PumpCoverType type = this.getType(stack);
      return type == null ? false : types.contains(CoverProperty.FluidConsuming);
   }

   @Override
   public boolean onTick(ItemStack stack, ICoverHolder parent) {
      PumpCoverType type = this.getType(stack);
      if (type == null) {
         return false;
      } else {
         CompoundTag nbtTagCompound = StackUtil.getOrCreateNbtData(stack);
         Direction side = Direction.values()[nbtTagCompound.getByteOr("side", (byte)0) & 255];
         boolean ret = false;
         BlockEntity holder = (BlockEntity)parent;
         LiquidUtil.AdjacentFluidHandler target = LiquidUtil.getAdjacentHandler(holder, side);
         if (target != null) {
            int amount = type.transferRate / 20;
            LiquidUtil.transfer(target.handler, target.dir.getOpposite(), holder, amount);
         }

         return ret;
      }
   }

   @Override
   public boolean allowsInput(ItemStack stack) {
      return false;
   }

   @Override
   public boolean allowsInput(FluidStack stack) {
      return true;
   }

   @Override
   public boolean allowsOutput(ItemStack stack) {
      return false;
   }

   @Override
   public boolean allowsOutput(FluidStack stack) {
      return false;
   }

   @Override
   public boolean providesEnhancedOverlay(Level world, BlockPos pos, Direction side, Player player, ItemStack stack) {
      BlockEntity tileEntity = world.getBlockEntity(pos);
      return tileEntity instanceof ICoverHolder;
   }

   private static String getSideName(Direction dir) {
      switch (dir) {
         case WEST:
            return "ic2.dir.west";
         case EAST:
            return "ic2.dir.east";
         case DOWN:
            return "ic2.dir.bottom";
         case UP:
            return "ic2.dir.top";
         case NORTH:
            return "ic2.dir.north";
         case SOUTH:
            return "ic2.dir.south";
         default:
            throw new RuntimeException("Invalid direction: " + dir);
      }
   }
}
