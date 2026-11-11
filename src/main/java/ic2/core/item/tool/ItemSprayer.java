package ic2.core.item.tool;

import ic2.api.item.IBoxable;
import ic2.core.IC2;
import ic2.core.block.BlockBase;
import ic2.core.block.BlockFoam;
import ic2.core.block.BlockIC2Fence;
import ic2.core.block.BlockScaffold;
import ic2.core.block.wiring.TileEntityCable;
import ic2.core.item.ItemDefaults;
import ic2.core.item.ItemIC2FluidContainer;
import ic2.core.ref.BlockName;
import ic2.core.ref.FluidName;
import ic2.core.ref.ItemName;
import ic2.core.slot.ArmorSlot;
import ic2.core.util.FluidTransfer;
import ic2.core.util.ItemData;
import ic2.core.util.LiquidUtil;
import ic2.core.util.StackUtil;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.neoforge.fluids.FluidStack;

public class ItemSprayer extends ItemIC2FluidContainer implements IBoxable {
   public ItemSprayer() {
      super(ItemName.foam_sprayer, 8000);
      ItemDefaults.stackSize(this, 1);
   }

   @Override
   public void func_150895_a(CreativeModeTab tab, NonNullList<ItemStack> subItems) {
      if (this.func_194125_a(tab)) {
         subItems.add(new ItemStack(this));
         subItems.add(this.getItemStack(FluidName.construction_foam));
      }
   }

   public InteractionResult use(Level world, Player player, InteractionHand hand) {
      if (IC2.platform.isSimulating() && IC2.keyboard.isModeSwitchKeyDown(player)) {
         ItemStack stack = StackUtil.get(player, hand);
         CompoundTag nbtData = StackUtil.getOrCreateNbtData(stack);
         int mode = nbtData.getIntOr("mode", 0);
         mode = mode == 0 ? 1 : 0;
         nbtData.putInt("mode", mode);
         ItemData.commit(stack, nbtData);
         String sMode = mode == 0 ? "ic2.tooltip.mode.normal" : "ic2.tooltip.mode.single";
         IC2.platform.messagePlayer(player, "ic2.tooltip.mode", sMode);
      }

      return super.use(world, player, hand);
   }

   @Override
   public InteractionResult func_180614_a(
      Player player, Level world, BlockPos pos, InteractionHand hand, Direction side, float xOffset, float yOffset, float zOffset
   ) {
      if (IC2.keyboard.isModeSwitchKeyDown(player)) {
         return InteractionResult.PASS;
      } else if (!IC2.platform.isSimulating()) {
         return InteractionResult.SUCCESS;
      } else {
         HitResult rtResult = this.func_77621_a(world, player, true);
         if (rtResult == null) {
            return InteractionResult.PASS;
         } else {
            if (rtResult.getType() == Type.BLOCK && !pos.equals(((BlockHitResult)rtResult).getBlockPos())) {
               BlockPos fluidPos = ((BlockHitResult)rtResult).getBlockPos();
               if (LiquidUtil.drainBlockToContainer(world, fluidPos, player, hand)) {
                  return InteractionResult.SUCCESS;
               }
            }

            int maxFoamBlocks = 0;
            ItemStack stack = StackUtil.get(player, hand);
            FluidStack fluid = FluidTransfer.getFluidContained(stack);
            if (fluid != null && fluid.getAmount() > 0) {
               maxFoamBlocks += fluid.getAmount() / this.getFluidPerFoam();
            }

            ItemStack pack = ArmorSlot.getEquipment(player.getInventory()).get(2);
            if (pack != null && pack.getItem() == ItemName.cf_pack.getInstance()) {
               fluid = FluidTransfer.getFluidContained(pack);
               if (fluid != null && fluid.getAmount() > 0) {
                  maxFoamBlocks += fluid.getAmount() / this.getFluidPerFoam();
               } else {
                  pack = null;
               }
            } else {
               pack = null;
            }

            if (maxFoamBlocks == 0) {
               return InteractionResult.FAIL;
            } else {
               maxFoamBlocks = Math.min(maxFoamBlocks, this.getMaxFoamBlocks(stack));
               ItemSprayer.Target target;
               if (canPlaceFoam(world, pos, ItemSprayer.Target.Scaffold)) {
                  target = ItemSprayer.Target.Scaffold;
               } else if (canPlaceFoam(world, pos, ItemSprayer.Target.Cable)) {
                  target = ItemSprayer.Target.Cable;
               } else {
                  pos = pos.relative(side);
                  target = ItemSprayer.Target.Any;
               }

               Vec3 viewVec = player.getLookAngle();
               Direction playerViewFacing = Direction.getApproximateNearest((float)viewVec.x, (float)viewVec.y, (float)viewVec.z);
               int amount = this.sprayFoam(world, pos, playerViewFacing.getOpposite(), target, maxFoamBlocks);
               amount *= this.getFluidPerFoam();
               if (amount > 0) {
                  if (pack != null) {
                     FluidTransfer packHandler = FluidTransfer.getFluidHandler(pack);

                     assert packHandler != null;

                     fluid = packHandler.drain(amount, true);
                     amount -= fluid.getAmount();
                     ArmorSlot.getEquipment(player.getInventory()).set(2, packHandler.getContainer());
                  }

                  if (amount > 0) {
                     FluidTransfer handler = FluidTransfer.getFluidHandler(stack);

                     assert handler != null;

                     handler.drain(amount, true);
                     StackUtil.set(player, hand, handler.getContainer());
                  }

                  return InteractionResult.SUCCESS;
               } else {
                  return InteractionResult.PASS;
               }
            }
         }
      }
   }

   public int sprayFoam(Level world, BlockPos pos, Direction excludedDir, ItemSprayer.Target target, int maxFoamBlocks) {
      if (!canPlaceFoam(world, pos, target)) {
         return 0;
      } else {
         Queue<BlockPos> toCheck = new ArrayDeque<>();
         Set<BlockPos> positions = new HashSet<>();
         toCheck.add(pos);

         BlockPos cPos;
         while ((cPos = toCheck.poll()) != null && positions.size() < maxFoamBlocks) {
            if (canPlaceFoam(world, cPos, target) && positions.add(cPos)) {
               for (Direction dir : Direction.values()) {
                  if (dir != excludedDir) {
                     toCheck.add(cPos.relative(dir));
                  }
               }
            }
         }

         toCheck.clear();
         int failedPlacements = 0;

         for (BlockPos targetPos : positions) {
            BlockState state = world.getBlockState(targetPos);
            Block targetBlock = state.getBlock();
            if (targetBlock == BlockName.scaffold.getInstance()) {
               BlockScaffold scaffold = (BlockScaffold)targetBlock;
               switch ((BlockScaffold.ScaffoldType)state.getValue(scaffold.getTypeProperty())) {
                  case wood:
                  case reinforced_wood:
                     scaffold.func_176226_b(world, targetPos, state, 0);
                     world.setBlock(targetPos, BlockName.foam.getBlockState(BlockFoam.FoamType.normal), 3);
                     break;
                  case reinforced_iron:
                     StackUtil.dropAsEntity(world, targetPos, BlockName.fence.getItemStack(BlockIC2Fence.IC2FenceType.iron));
                  case iron:
                     world.setBlock(targetPos, BlockName.foam.getBlockState(BlockFoam.FoamType.reinforced), 3);
               }
            } else if (targetBlock == BlockName.te.getInstance()) {
               BlockEntity te = world.getBlockEntity(targetPos);
               if (te instanceof TileEntityCable && !((TileEntityCable)te).foam()) {
                  failedPlacements++;
               }
            } else if (!world.setBlock(targetPos, BlockName.foam.getBlockState(BlockFoam.FoamType.normal), 3)) {
               failedPlacements++;
            }
         }

         return positions.size() - failedPlacements;
      }
   }

   protected int getMaxFoamBlocks(ItemStack stack) {
      CompoundTag nbtData = StackUtil.getOrCreateNbtData(stack);
      return nbtData.getIntOr("mode", 0) == 0 ? 10 : 1;
   }

   protected int getFluidPerFoam() {
      return 100;
   }

   @Override
   public boolean canBeStoredInToolbox(ItemStack itemstack) {
      return true;
   }

   @Override
   public boolean canfill(Fluid fluid) {
      return fluid == FluidName.construction_foam.getInstance();
   }

   private static boolean canPlaceFoam(Level world, BlockPos pos, ItemSprayer.Target target) {
      switch (target) {
         case Any:
            return BlockName.foam.<BlockBase>getInstance().func_176198_a(world, pos, Direction.DOWN);
         case Scaffold:
            return world.getBlockState(pos).getBlock() == BlockName.scaffold.getInstance();
         case Cable:
            if (world.getBlockState(pos).getBlock() != BlockName.te.getInstance()) {
               return false;
            }

            BlockEntity te = world.getBlockEntity(pos);
            if (te instanceof TileEntityCable) {
               return !((TileEntityCable)te).isFoamed();
            }
            break;
         default:
            assert false;
      }

      return false;
   }

   private static enum Target {
      Any,
      Scaffold,
      Cable;
   }
}
