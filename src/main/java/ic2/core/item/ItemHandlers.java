package ic2.core.item;

import ic2.api.recipe.Recipes;
import ic2.core.IC2;
import ic2.core.IC2Potion;
import ic2.core.block.BlockBase;
import ic2.core.block.BlockSheet;
import ic2.core.block.reactor.tileentity.TileEntityNuclearReactorElectric;
import ic2.core.item.armor.ItemArmorHazmat;
import ic2.core.item.type.CellType;
import ic2.core.item.type.IRadioactiveItemType;
import ic2.core.item.upgrade.ItemUpgradeModule;
import ic2.core.ref.BlockName;
import ic2.core.ref.FluidName;
import ic2.core.ref.ItemName;
import ic2.core.ref.TeBlock;
import ic2.core.util.LiquidUtil;
import ic2.core.util.NativePlacement;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;

public class ItemHandlers {
   public static ItemMulti.IItemRightClickHandler cfPowderApply = new ItemMulti.IItemRightClickHandler() {
      @Override
      public InteractionResult onRightClick(ItemStack stack, Player player, InteractionHand hand) {
         HitResult position = Util.traceBlocks(player, true);
         if (position == null) {
            return InteractionResult.PASS;
         } else {
            if (position.getType() == Type.BLOCK) {
               Level world = player.level();
               if (!world.mayInteract(player, ((BlockHitResult)position).getBlockPos())) {
                  return InteractionResult.FAIL;
               }

               if (world.getBlockState(((BlockHitResult)position).getBlockPos()).getBlock() == Blocks.WATER) {
                  stack = StackUtil.decSize(stack);
                  world.setBlock(
                     ((BlockHitResult)position).getBlockPos(),
                     FluidName.construction_foam.getInstance().defaultFluidState().createLegacyBlock().getBlock().defaultBlockState(),
                     3
                  );
                  InteractionResult.SUCCESS.heldItemTransformedTo(stack);
               }
            }

            return InteractionResult.FAIL;
         }
      }
   };
   public static ItemMulti.IItemRightClickHandler scrapBoxUnpack = new ItemMulti.IItemRightClickHandler() {
      @Override
      public InteractionResult onRightClick(ItemStack stack, Player player, InteractionHand hand) {
         if (!player.level().isClientSide()) {
            ItemStack drop = Recipes.scrapboxDrops.getDrop(stack, false);
            if (drop != null && player.drop(drop, false, Prediction.SERVER_ONLY) != null && !player.getAbilities().instabuild) {
               stack = StackUtil.decSize(stack);
               return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
            }
         }

         return InteractionResult.PASS;
      }
   };
   public static ItemMulti.IItemUseHandler resinUse = new ItemMulti.IItemUseHandler() {
      @Override
      public InteractionResult onUse(ItemStack stack, Player player, BlockPos pos, InteractionHand hand, Direction side) {
         Level world = player.level();
         BlockState state = world.getBlockState(pos);
         if (state.getBlock() == Blocks.PISTON && state.getValue(PistonBaseBlock.FACING) == side) {
            BlockState newState = (BlockState)((BlockState)Blocks.STICKY_PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, side))
               .setValue(PistonBaseBlock.EXTENDED, (Boolean)state.getValue(PistonBaseBlock.EXTENDED));
            world.setBlock(pos, newState, 3);
            if (!player.getAbilities().instabuild) {
               StackUtil.consumeOrError(player, hand, 1);
            }

            return InteractionResult.SUCCESS;
         } else if (side != Direction.UP) {
            return InteractionResult.PASS;
         } else {
            pos = pos.above();
            if (state.isAir() && BlockName.sheet.<BlockBase>getInstance().func_176198_a(world, pos, side)) {
               world.setBlock(pos, BlockName.sheet.getBlockState(BlockSheet.SheetType.resin), 3);
               if (!player.getAbilities().instabuild) {
                  StackUtil.consumeOrError(player, hand, 1);
               }

               return InteractionResult.PASS;
            } else {
               return InteractionResult.PASS;
            }
         }
      }
   };
   public static ItemMulti.IItemUpdateHandler radioactiveUpdate = new ItemMulti.IItemUpdateHandler() {
      @Override
      public void onUpdate(ItemStack stack, Level world, Entity rawEntity, int slotIndex, boolean isCurrentItem) {
         Item item = stack.getItem();
         if (item != null
            && item instanceof ItemMulti
            && ((ItemMulti)item).getType(stack) instanceof IRadioactiveItemType type
            && rawEntity instanceof LivingEntity entity
            && !ItemArmorHazmat.hasCompleteHazmat(entity)) {
            IC2Potion.radiation.applyTo(entity, type.getRadiationDuration() * 20, type.getRadiationAmplifier());
         }
      }
   };
   public static TeBlock.ITePlaceHandler reactorChamberPlace = new TeBlock.ITePlaceHandler() {
      @Override
      public boolean canReplace(Level world, BlockPos pos, Direction side, ItemStack stack) {
         int count = 0;

         for (Direction dir : Direction.values()) {
            BlockEntity te = world.getBlockEntity(pos.relative(dir));
            if (te instanceof TileEntityNuclearReactorElectric) {
               count++;
            }
         }

         return count == 1;
      }
   };
   public static ItemMulti.IItemRightClickHandler openAdvancedUpgradeGUI = new ItemMulti.IItemRightClickHandler() {
      @Override
      public InteractionResult onRightClick(ItemStack stack, Player player, InteractionHand hand) {
         assert stack.getItem() == ItemName.upgrade.getInstance();

         if (IC2.platform.isSimulating()) {
            IC2.platform.launchGui(player, ((ItemUpgradeModule)stack.getItem()).getInventory(player, stack));
         }

         return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
      }
   };
   public static ItemMulti.IItemUseHandler emptyCellFill = new ItemMulti.IItemUseHandler() {
      @Override
      public InteractionResult onUse(ItemStack stack, Player player, BlockPos pos, InteractionHand hand, Direction side) {
         assert stack.getItem() == ItemName.cell.getInstance();

         Level world = player.level();
         HitResult position = Util.traceBlocks(player, true);
         if (position == null) {
            return InteractionResult.FAIL;
         } else {
            if (position.getType() == Type.BLOCK) {
               pos = ((BlockHitResult)position).getBlockPos();
               if (!world.mayInteract(player, pos)) {
                  return InteractionResult.FAIL;
               }

               if (!player.mayUseItemAt(pos, ((BlockHitResult)position).getDirection(), player.getItemInHand(hand))) {
                  return InteractionResult.FAIL;
               }

               LiquidUtil.LiquidData data = LiquidUtil.getLiquid(world, pos);
               if (data != null && data.isSource) {
                  if (data.liquid == Fluids.WATER && StackUtil.storeInventoryItem(ItemName.cell.getItemStack(CellType.water), player, true)) {
                     world.removeBlock(pos, false);
                     StackUtil.consumeOrError(player, hand, 1);
                     StackUtil.storeInventoryItem(ItemName.cell.getItemStack(CellType.water), player, false);
                     return InteractionResult.SUCCESS;
                  }

                  if (data.liquid == Fluids.LAVA && StackUtil.storeInventoryItem(ItemName.cell.getItemStack(CellType.lava), player, true)) {
                     world.removeBlock(pos, false);
                     StackUtil.consumeOrError(player, hand, 1);
                     StackUtil.storeInventoryItem(ItemName.cell.getItemStack(CellType.lava), player, false);
                     return InteractionResult.SUCCESS;
                  }
               }
            }

            return InteractionResult.PASS;
         }
      }
   };

   public static ItemMulti.IItemUseHandler getFluidPlacer(final Block type) {
      return new ItemMulti.IItemUseHandler() {
         @Override
         public InteractionResult onUse(ItemStack stack, Player player, BlockPos pos, InteractionHand hand, Direction side) {
            assert stack.getItem() == ItemName.misc_resource.getInstance();

            Level world = player.level();
            if (!world.getBlockState(pos).canBeReplaced()) {
               pos = pos.relative(side);
            }

            if (player.mayUseItemAt(pos, side, stack) && NativePlacement.canPlace(type, world, pos, side, player, hand, false)) {
               BlockState placedState = NativePlacement.state(type, world, pos, side, player, hand, 0.0F, 0.0F, 0.0F);
               world.setBlock(pos, placedState, 3);
               SoundType sound = placedState.getBlock().getSoundType(placedState, world, pos, player);
               world.playSound(player, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
               StackUtil.consumeOrError(player, hand, 1);
               return InteractionResult.SUCCESS;
            } else {
               return InteractionResult.FAIL;
            }
         }
      };
   }
}
