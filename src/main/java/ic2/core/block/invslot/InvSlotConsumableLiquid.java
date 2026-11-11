package ic2.core.block.invslot;

import ic2.api.util.FluidContainerOutputMode;
import ic2.core.block.IInventorySlotHolder;
import ic2.core.block.comp.Ic2FluidTank;
import ic2.core.util.FluidTransfer;
import ic2.core.util.LiquidUtil;
import ic2.core.util.StackUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import org.apache.commons.lang3.mutable.MutableObject;

public class InvSlotConsumableLiquid extends InvSlotConsumable {
   private InvSlotConsumableLiquid.OpType opType;

   public InvSlotConsumableLiquid(IInventorySlotHolder<?> base1, String name1, int count) {
      this(base1, name1, InvSlot.Access.I, count, InvSlot.InvSide.TOP, InvSlotConsumableLiquid.OpType.Drain);
   }

   public InvSlotConsumableLiquid(
      IInventorySlotHolder<?> base1, String name1, InvSlot.Access access1, int count, InvSlot.InvSide preferredSide1, InvSlotConsumableLiquid.OpType opType1
   ) {
      super(base1, name1, access1, count, preferredSide1);
      this.opType = opType1;
   }

   @Override
   public boolean accepts(ItemStack stack) {
      if (StackUtil.isEmpty(stack)) {
         return false;
      } else if (!LiquidUtil.isFluidContainer(stack)) {
         return false;
      } else {
         if (this.opType == InvSlotConsumableLiquid.OpType.Drain || this.opType == InvSlotConsumableLiquid.OpType.Both) {
            FluidStack containerFluid = null;
            if (FluidTransfer.getFluidHandler(stack) != null) {
               ItemStack singleStack = StackUtil.copyWithSize(stack, 1);
               FluidTransfer handler = FluidTransfer.getFluidHandler(singleStack);
               if (handler != null) {
                  containerFluid = handler.drain(Integer.MAX_VALUE, false);
               }
            }

            if (containerFluid != null && containerFluid.getAmount() > 0 && this.acceptsLiquid(containerFluid.getFluid())) {
               return true;
            }
         }

         return (this.opType == InvSlotConsumableLiquid.OpType.Fill || this.opType == InvSlotConsumableLiquid.OpType.Both)
            && LiquidUtil.isFillableFluidContainer(stack, this.getPossibleFluids());
      }
   }

   public FluidStack drain(Fluid fluid, int maxAmount, MutableObject<ItemStack> output, boolean simulate) {
      output.setValue(null);
      if (fluid != null && !this.acceptsLiquid(fluid)) {
         return null;
      } else if (this.opType != InvSlotConsumableLiquid.OpType.Drain && this.opType != InvSlotConsumableLiquid.OpType.Both) {
         return null;
      } else {
         ItemStack stack = this.get();
         if (StackUtil.isEmpty(stack)) {
            return null;
         } else {
            LiquidUtil.FluidOperationResult result = LiquidUtil.drainContainer(stack, fluid, maxAmount, FluidContainerOutputMode.EmptyFullToOutput);
            if (result == null) {
               return null;
            } else if (fluid == null && !this.acceptsLiquid(result.fluidChange.getFluid())) {
               return null;
            } else {
               output.setValue(result.extraOutput);
               if (!simulate) {
                  this.put(result.inPlaceOutput);
               }

               return result.fluidChange;
            }
         }
      }
   }

   public int fill(FluidStack fs, MutableObject<ItemStack> output, boolean simulate) {
      output.setValue(null);
      if (fs == null || fs.getAmount() <= 0) {
         return 0;
      } else if (this.opType != InvSlotConsumableLiquid.OpType.Fill && this.opType != InvSlotConsumableLiquid.OpType.Both) {
         return 0;
      } else {
         ItemStack stack = this.get();
         if (StackUtil.isEmpty(stack)) {
            return 0;
         } else {
            LiquidUtil.FluidOperationResult result = LiquidUtil.fillContainer(stack, fs, FluidContainerOutputMode.EmptyFullToOutput);
            if (result == null) {
               return 0;
            } else {
               output.setValue(result.extraOutput);
               if (!simulate) {
                  this.put(result.inPlaceOutput);
               }

               return result.fluidChange.getAmount();
            }
         }
      }
   }

   public boolean transferToTank(Ic2FluidTank tank, MutableObject<ItemStack> output, boolean simulate) {
      int space = tank.getCapacity();
      Fluid fluidRequired = null;
      FluidStack tankFluid = tank.getFluid();
      if (tankFluid != null) {
         space -= tankFluid.getAmount();
         fluidRequired = tankFluid.getFluid();
      }

      FluidStack fluid = this.drain(fluidRequired, space, output, true);
      if (fluid == null) {
         return false;
      } else {
         int amount = tank.fill(fluid, !simulate);
         if (amount <= 0) {
            return false;
         } else {
            if (!simulate) {
               this.drain(fluidRequired, amount, output, false);
            }

            return true;
         }
      }
   }

   public boolean transferFromTank(Ic2FluidTank tank, MutableObject<ItemStack> output, boolean simulate) {
      FluidStack tankFluid = tank.drain(tank.getFluidAmount(), false);
      if (tankFluid != null && tankFluid.getAmount() > 0) {
         int amount = this.fill(tankFluid, output, simulate);
         if (amount <= 0) {
            return false;
         } else {
            if (!simulate) {
               tank.drain(amount, true);
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public boolean processIntoTank(Ic2FluidTank tank, InvSlotOutput outputSlot) {
      if (this.isEmpty()) {
         return false;
      } else {
         MutableObject<ItemStack> output = new MutableObject();
         boolean wasChange = false;
         if (this.transferToTank(tank, output, true) && (StackUtil.isEmpty((ItemStack)output.getValue()) || outputSlot.canAdd((ItemStack)output.getValue()))) {
            wasChange = this.transferToTank(tank, output, false);
            if (!StackUtil.isEmpty((ItemStack)output.getValue())) {
               outputSlot.add((ItemStack)output.getValue());
            }
         }

         return wasChange;
      }
   }

   public boolean processFromTank(Ic2FluidTank tank, InvSlotOutput outputSlot) {
      if (!this.isEmpty() && tank.getFluidAmount() > 0) {
         MutableObject<ItemStack> output = new MutableObject();
         boolean wasChange = false;
         if (this.transferFromTank(tank, output, true) && (StackUtil.isEmpty((ItemStack)output.getValue()) || outputSlot.canAdd((ItemStack)output.getValue()))) {
            wasChange = this.transferFromTank(tank, output, false);
            if (!StackUtil.isEmpty((ItemStack)output.getValue())) {
               outputSlot.add((ItemStack)output.getValue());
            }
         }

         return wasChange;
      } else {
         return false;
      }
   }

   public void setOpType(InvSlotConsumableLiquid.OpType opType1) {
      this.opType = opType1;
   }

   protected boolean acceptsLiquid(Fluid fluid) {
      return true;
   }

   protected Iterable<Fluid> getPossibleFluids() {
      return null;
   }

   public static enum OpType {
      Drain,
      Fill,
      Both,
      None;
   }
}
