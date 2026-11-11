package ic2.core.util;

import ic2.api.util.FluidContainerOutputMode;
import ic2.core.ref.BlockMaterial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.apache.commons.lang3.mutable.MutableObject;

public class LiquidUtil {
   private static final Collection<Fluid> registeredFluids = BuiltInRegistries.FLUID.stream().toList();

   public static List<Fluid> getAllFluids() {
      Set<Fluid> fluids = new HashSet<>(BuiltInRegistries.FLUID.stream().toList());
      fluids.remove(null);
      List<Fluid> ret = new ArrayList<>(fluids);
      Collections.sort(ret, new Comparator<Fluid>() {
         public int compare(Fluid a, Fluid b) {
            String nameA = FluidKeys.name(a);
            String nameB = FluidKeys.name(b);
            if (nameA == null) {
               return nameB == null ? 0 : 1;
            } else {
               return nameB == null ? -1 : nameA.toLowerCase(Locale.ENGLISH).compareTo(nameB.toLowerCase(Locale.ENGLISH));
            }
         }
      });
      return ret;
   }

   public static LiquidUtil.LiquidData getLiquid(Level world, BlockPos pos) {
      BlockState state = world.getBlockState(pos);
      Block block = state.getBlock();
      FluidState fluidState = state.getFluidState();
      return fluidState.isEmpty() ? null : new LiquidUtil.LiquidData(FluidKeys.source(fluidState.getType()), fluidState.isSource());
   }

   public static boolean isFluidContainer(ItemStack stack) {
      return FluidTransfer.getFluidHandler(stack) != null;
   }

   public static boolean isDrainableFluidContainer(ItemStack stack) {
      if (!isFluidContainer(stack)) {
         return false;
      } else {
         ItemStack singleStack = StackUtil.copyWithSize(stack, 1);
         FluidTransfer handler = FluidTransfer.getFluidHandler(singleStack);
         if (handler == null) {
            return false;
         } else {
            FluidStack fs = handler.drain(Integer.MAX_VALUE, false);
            return fs != null && fs.getAmount() > 0;
         }
      }
   }

   public static boolean isFillableFluidContainer(ItemStack stack) {
      return isFillableFluidContainer(stack, null);
   }

   public static boolean isFillableFluidContainer(ItemStack stack, Iterable<Fluid> testFluids) {
      if (!isFluidContainer(stack)) {
         return false;
      } else {
         if (testFluids == null) {
            Iterable<Fluid> var9 = registeredFluids;
         }

         ItemStack singleStack = StackUtil.copyWithSize(stack, 1);
         FluidTransfer handler = FluidTransfer.getFluidHandler(singleStack);
         if (handler == null) {
            return false;
         } else {
            FluidStack fs = handler.drain(Integer.MAX_VALUE, false);
            if (fs != null && handler.fill(fs.copyWithAmount(Integer.MAX_VALUE), false) > 0) {
               return true;
            } else {
               for (FluidTransfer.TankContents properties : handler.getTankProperties()) {
                  fs = properties.getContents();
                  if (fs != null && handler.fill(fs.copyWithAmount(Integer.MAX_VALUE), false) > 0) {
                     return true;
                  }
               }

               for (Fluid fluid : registeredFluids) {
                  if (testFillFluid(handler, fluid, null)) {
                     return true;
                  }
               }

               return false;
            }
         }
      }
   }

   private static boolean testFillFluid(FluidTransfer handler, Fluid fluid, CompoundTag nbt) {
      FluidStack fs = new FluidStack(fluid, Integer.MAX_VALUE);
      return handler.fill(fs, false) > 0;
   }

   public static FluidStack drainContainer(
      Player player, InteractionHand hand, Fluid fluid, int maxAmount, FluidContainerOutputMode outputMode, boolean simulate
   ) {
      ItemStack stack = StackUtil.get(player, hand);
      LiquidUtil.FluidOperationResult result = drainContainer(stack, fluid, maxAmount, outputMode);
      if (result == null) {
         return null;
      } else if (result.extraOutput != null && !StackUtil.storeInventoryItem(result.extraOutput, player, simulate)) {
         return null;
      } else {
         if (!simulate) {
            StackUtil.set(player, hand, result.inPlaceOutput);
         }

         return result.fluidChange;
      }
   }

   public static int fillContainer(Player player, InteractionHand hand, FluidStack fs, FluidContainerOutputMode outputMode, boolean simulate) {
      ItemStack stack = StackUtil.get(player, hand);
      LiquidUtil.FluidOperationResult result = fillContainer(stack, fs, outputMode);
      if (result == null) {
         return 0;
      } else if (result.extraOutput != null && !StackUtil.storeInventoryItem(result.extraOutput, player, simulate)) {
         return 0;
      } else {
         if (!simulate) {
            StackUtil.set(player, hand, result.inPlaceOutput);
         }

         return result.fluidChange.getAmount();
      }
   }

   public static LiquidUtil.FluidOperationResult drainContainer(ItemStack stack, Fluid fluid, int maxAmount, FluidContainerOutputMode outputMode) {
      if (!StackUtil.isEmpty(stack) && maxAmount > 0) {
         ItemStack inPlace = StackUtil.copy(stack);
         ItemStack extra = null;
         if (FluidTransfer.getFluidHandler(inPlace) == null) {
            return null;
         } else {
            ItemStack singleStack = StackUtil.copyWithSize(inPlace, 1);
            FluidTransfer handler = FluidTransfer.getFluidHandler(singleStack);
            if (handler == null) {
               return null;
            } else {
               FluidStack fs;
               if (fluid == null) {
                  fs = handler.drain(maxAmount, true);
               } else {
                  fs = handler.drain(new FluidStack(fluid, maxAmount), true);
               }

               if (fs != null && fs.getAmount() > 0) {
                  if (StackUtil.isEmpty(singleStack)) {
                     inPlace = StackUtil.decSize(inPlace);
                  } else {
                     FluidStack leftOver = handler.drain(Integer.MAX_VALUE, false);
                     boolean isEmpty = leftOver == null || leftOver.getAmount() <= 0;
                     if ((!isEmpty || !outputMode.isOutputEmptyFull())
                        && outputMode != FluidContainerOutputMode.AnyToOutput
                        && (outputMode != FluidContainerOutputMode.InPlacePreferred || StackUtil.getSize(inPlace) <= 1)) {
                        if (StackUtil.getSize(inPlace) > 1) {
                           return null;
                        }

                        inPlace = handler.getContainer();
                     } else {
                        extra = handler.getContainer();
                        inPlace = StackUtil.decSize(inPlace);
                     }
                  }

                  assert fs.getAmount() > 0;

                  return new LiquidUtil.FluidOperationResult(fs, inPlace, extra);
               } else {
                  return null;
               }
            }
         }
      } else {
         return null;
      }
   }

   public static LiquidUtil.FluidOperationResult fillContainer(ItemStack stack, FluidStack fsIn, FluidContainerOutputMode outputMode) {
      if (!StackUtil.isEmpty(stack) && fsIn != null && fsIn.getAmount() > 0) {
         ItemStack inPlace = StackUtil.copy(stack);
         ItemStack extra = null;
         if (FluidTransfer.getFluidHandler(inPlace) != null) {
            ItemStack singleStack = StackUtil.copyWithSize(inPlace, 1);
            FluidTransfer handler = FluidTransfer.getFluidHandler(singleStack);
            if (handler == null) {
               return null;
            } else {
               FluidStack fsChange = fsIn.copy();
               int amount = handler.fill(fsChange, true);
               if (amount <= 0) {
                  return null;
               } else {
                  fsChange.setAmount(amount);
                  FluidStack fillTestFs = fsIn.copy();
                  fillTestFs.setAmount(Integer.MAX_VALUE);
                  boolean isFull = handler.fill(fillTestFs, false) <= 0;
                  singleStack = handler.getContainer();

                  assert fsChange.getFluid() == fsIn.getFluid();

                  assert fsChange.getAmount() > 0;

                  assert StackUtil.getSize(singleStack) == 1;

                  if ((!isFull || !outputMode.isOutputEmptyFull())
                     && outputMode != FluidContainerOutputMode.AnyToOutput
                     && (outputMode != FluidContainerOutputMode.InPlacePreferred || StackUtil.getSize(inPlace) <= 1)) {
                     if (StackUtil.getSize(inPlace) > 1) {
                        return null;
                     }

                     inPlace = singleStack;
                  } else {
                     extra = singleStack;
                     inPlace = StackUtil.decSize(inPlace);
                  }

                  return new LiquidUtil.FluidOperationResult(fsChange, inPlace, extra);
               }
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public static boolean isFluidTile(BlockEntity te, Direction side) {
      return te != null && BlockCapabilities.has(te, net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, side);
   }

   public static FluidStack drainTile(BlockEntity te, Direction side, int maxAmount, boolean simulate) {
      return drainTile(te, side, null, maxAmount, simulate);
   }

   public static FluidStack drainTile(BlockEntity te, Direction side, Fluid fluid, int maxAmount, boolean simulate) {
      if (BlockCapabilities.has(te, net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, side)) {
         FluidTransfer handler = new FluidTransfer(BlockCapabilities.get(te, net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, side), null);
         if (handler == null) {
            return null;
         } else {
            return fluid == null ? handler.drain(maxAmount, !simulate) : handler.drain(new FluidStack(fluid, maxAmount), !simulate);
         }
      } else {
         return null;
      }
   }

   public static int fillTile(BlockEntity te, Direction side, FluidStack fs, boolean simulate) {
      if (BlockCapabilities.has(te, net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, side)) {
         FluidTransfer handler = new FluidTransfer(BlockCapabilities.get(te, net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, side), null);
         return handler == null ? 0 : handler.fill(fs, !simulate);
      } else {
         return 0;
      }
   }

   public static List<LiquidUtil.AdjacentFluidHandler> getAdjacentHandlers(BlockEntity source) {
      List<LiquidUtil.AdjacentFluidHandler> ret = new ArrayList<>();

      for (Direction dir : Direction.values()) {
         BlockEntity te = source.getLevel().getBlockEntity(source.getBlockPos().relative(dir));
         if (isFluidTile(te, dir.getOpposite())) {
            ret.add(new LiquidUtil.AdjacentFluidHandler(te, dir));
         }
      }

      return ret;
   }

   public static LiquidUtil.AdjacentFluidHandler getAdjacentHandler(BlockEntity source, Direction dir) {
      BlockEntity te = source.getLevel().getBlockEntity(source.getBlockPos().relative(dir));
      return !isFluidTile(te, dir.getOpposite()) ? null : new LiquidUtil.AdjacentFluidHandler(te, dir);
   }

   public static int distribute(BlockEntity source, FluidStack stack, boolean simulate) {
      int transferred = 0;

      for (LiquidUtil.AdjacentFluidHandler handler : getAdjacentHandlers(source)) {
         int amount = fillTile(handler.handler, handler.dir.getOpposite(), stack, simulate);
         transferred += amount;
         stack.setAmount(stack.getAmount() - amount);
         if (stack.getAmount() <= 0) {
            break;
         }
      }

      stack.setAmount(stack.getAmount() + transferred);
      return transferred;
   }

   public static int distributeAll(BlockEntity source, int amount) {
      if (source == null) {
         throw new IllegalArgumentException("source has to be a tile entity");
      } else {
         BlockEntity srcTe = source;
         int transferred = 0;

         for (Direction dir : Direction.values()) {
            BlockEntity te = srcTe.getLevel().getBlockEntity(srcTe.getBlockPos().relative(dir));
            if (isFluidTile(te, dir.getOpposite())) {
               FluidStack stack = transfer(source, dir, te, amount);
               if (stack != null) {
                  amount -= stack.getAmount();
                  transferred += stack.getAmount();
                  if (amount <= 0) {
                     break;
                  }
               }
            }
         }

         return transferred;
      }
   }

   public static FluidStack transfer(BlockEntity source, Direction dir, BlockEntity target, int amount) {
      while (true) {
         FluidStack ret = drainTile(source, dir, amount, true);
         if (ret != null && ret.getAmount() > 0) {
            if (ret.getAmount() > amount) {
               throw new IllegalStateException("The fluid handler " + source + " drained more than the requested amount.");
            }

            int cAmount = fillTile(target, getOppositeDir(dir), ret.copy(), true);
            if (cAmount > amount) {
               throw new IllegalStateException("The fluid handler " + target + " filled more than the requested amount.");
            }

            amount = cAmount;
            if (cAmount != ret.getAmount() && cAmount > 0) {
               continue;
            }

            if (cAmount <= 0) {
               return null;
            }

            ret = drainTile(source, dir, cAmount, false);
            if (ret == null) {
               throw new IllegalStateException(
                  "The fluid handler " + source + " drained inconsistently. Expected " + cAmount + ", couldn't find previous IFluidHandler facing " + dir + "."
               );
            }

            if (ret.getAmount() != cAmount) {
               throw new IllegalStateException(
                  "The fluid handler " + source + " drained inconsistently. Expected " + cAmount + ", got " + ret.getAmount() + "."
               );
            }

            amount = fillTile(target, getOppositeDir(dir), ret.copy(), false);
            if (amount != ret.getAmount()) {
               throw new IllegalStateException("The fluid handler " + target + " filled inconsistently. Expected " + ret.getAmount() + ", got " + amount + ".");
            }

            return ret;
         }

         return null;
      }
   }

   private static Direction getOppositeDir(Direction dir) {
      return dir == null ? null : dir.getOpposite();
   }

   public static boolean check(FluidStack fs) {
      return fs.getFluid() != null;
   }

   public static FluidStack drainBlock(Level world, BlockPos pos, boolean simulate) {
      FluidState state = world.getFluidState(pos);
      if (!state.isEmpty() && state.isSource()) {
         FluidStack result = new FluidStack(FluidKeys.source(state.getType()), 1000);
         if (!simulate) {
            world.removeBlock(pos, false);
         }

         return result;
      } else {
         return null;
      }
   }

   public static boolean drainBlockToContainer(Level world, BlockPos pos, Player player, InteractionHand hand) {
      FluidStack fs = drainBlock(world, pos, true);
      if (fs != null && fs.getAmount() > 0) {
         int amount = fillContainer(player, hand, fs, FluidContainerOutputMode.InPlacePreferred, true);
         if (amount != fs.getAmount()) {
            return false;
         } else {
            fs = drainBlock(world, pos, false);
            fillContainer(player, hand, fs, FluidContainerOutputMode.InPlacePreferred, false);
            return true;
         }
      } else {
         return false;
      }
   }

   public static boolean fillBlock(FluidStack fs, Level world, BlockPos pos, boolean simulate) {
      if (fs != null && fs.getAmount() >= 1000) {
         Fluid fluid = fs.getFluid();
         if (fluid != null && !fluid.defaultFluidState().createLegacyBlock().isAir() && fluid.defaultFluidState().createLegacyBlock().getBlock() != null) {
            BlockState state = world.getBlockState(pos);
            Block block = state.getBlock();
            if (!state.isAir() && BlockMaterial.of(state).isSolid()) {
               return false;
            } else if (block == fluid.defaultFluidState().createLegacyBlock().getBlock() && isFullFluidBlock(world, pos, block, state)) {
               return false;
            } else if (simulate) {
               return true;
            } else {
               Block fluidBlock;
               if ((Boolean)world.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, pos)
                  && (fluidBlock = fluid.defaultFluidState().createLegacyBlock().getBlock()) != null
                  && BlockMaterial.of(fluidBlock.defaultBlockState()) == BlockMaterial.WATER) {
                  world.playSound(
                     null,
                     pos,
                     SoundEvents.FIRE_EXTINGUISH,
                     SoundSource.BLOCKS,
                     0.5F,
                     2.6F + (world.getRandom().nextFloat() - world.getRandom().nextFloat()) * 0.8F
                  );

                  for (int i = 0; i < 8; i++) {
                     world.addParticle(
                        ParticleTypes.LARGE_SMOKE, pos.getX() + Math.random(), pos.getY() + Math.random(), pos.getZ() + Math.random(), 0.0, 0.0, 0.0
                     );
                  }
               } else {
                  if (!world.isClientSide() && !BlockMaterial.of(state).isSolid() && !BlockMaterial.of(state).isLiquid()) {
                     world.destroyBlock(pos, true);
                  }

                  Block var9;
                  if (fluid == Fluids.WATER) {
                     var9 = Blocks.WATER;
                  } else if (fluid == Fluids.LAVA) {
                     var9 = Blocks.LAVA;
                  } else {
                     var9 = fluid.defaultFluidState().createLegacyBlock().getBlock();
                  }

                  if (!world.setBlock(pos, var9.defaultBlockState(), 3)) {
                     return false;
                  }
               }

               fs.setAmount(fs.getAmount() - 1000);
               return true;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean isFullFluidBlock(Level world, BlockPos pos, Block block, BlockState state) {
      FluidState fluid = state.getFluidState();
      return !fluid.isEmpty() && fluid.isSource();
   }

   public static boolean fillBlockFromContainer(Level world, BlockPos pos, Player player, InteractionHand hand) {
      FluidStack fs = drainContainer(player, hand, null, 1000, FluidContainerOutputMode.InPlacePreferred, true);
      if (fs == null || fs.getAmount() < 1000) {
         return false;
      } else if (!fillBlock(fs, world, pos, false)) {
         return false;
      } else {
         drainContainer(player, hand, null, 1000, FluidContainerOutputMode.InPlacePreferred, false);
         return true;
      }
   }

   public static boolean storeOutputContainer(MutableObject<ItemStack> output, Player player) {
      return output.getValue() == null ? true : StackUtil.storeInventoryItem((ItemStack)output.getValue(), player, false);
   }

   public static String toStringSafe(FluidStack fluidStack) {
      return fluidStack.getFluid() == null ? fluidStack.getAmount() + "(mb)x(null)@(unknown)" : fluidStack.toString();
   }

   public static class AdjacentFluidHandler {
      public final BlockEntity handler;
      public final Direction dir;

      AdjacentFluidHandler(BlockEntity handler, Direction dir) {
         this.handler = handler;
         this.dir = dir;
      }
   }

   public static class FluidOperationResult {
      public final FluidStack fluidChange;
      public final ItemStack inPlaceOutput;
      public final ItemStack extraOutput;

      FluidOperationResult(FluidStack fluidChange, ItemStack inPlaceOutput, ItemStack extraOutput) {
         if (fluidChange == null) {
            throw new NullPointerException("null fluid change");
         } else {
            this.fluidChange = fluidChange;
            this.inPlaceOutput = inPlaceOutput;
            this.extraOutput = extraOutput;
         }
      }
   }

   public static class LiquidData {
      public final Fluid liquid;
      public final boolean isSource;

      LiquidData(Fluid liquid1, boolean isSource1) {
         this.liquid = liquid1;
         this.isSource = isSource1;
      }
   }
}
