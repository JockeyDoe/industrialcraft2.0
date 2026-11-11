package ic2.core.ref;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public interface IFluidModelProvider {
   @OnlyIn(Dist.CLIENT)
   void registerModels(FluidName var1);
}
