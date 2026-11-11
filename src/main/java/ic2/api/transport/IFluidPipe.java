package ic2.api.transport;

import ic2.core.block.comp.Ic2FluidTank;

public interface IFluidPipe extends IPipe {
   int getTransferRate();

   Ic2FluidTank getTank();

   int getCurrentInnerCapacity();

   int getMaxInnerCapacity();
}
