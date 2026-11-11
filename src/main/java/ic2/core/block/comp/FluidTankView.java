package ic2.core.block.comp;

import net.neoforged.neoforge.fluids.FluidStack;

public interface FluidTankView {
   FluidStack getFluid();

   int getFluidAmount();

   int getCapacity();
}
