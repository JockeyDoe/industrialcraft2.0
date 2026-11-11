package ic2.core.block.transport.cover;

import java.util.Collection;
import java.util.Collections;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities.Fluid;

public interface IFluidConsumingCover extends ICoverItem {
   @Override
   default Collection<? extends BlockCapability<?, Direction>> getProvidedCapabilities() {
      return Collections.singleton(Fluid.BLOCK);
   }
}
