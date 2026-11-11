package ic2.core.block.transport.cover;

import java.util.Collection;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.fluids.FluidStack;

public interface ICoverItem {
   boolean isSuitableFor(ItemStack var1, Set<CoverProperty> var2);

   boolean onTick(ItemStack var1, ICoverHolder var2);

   boolean allowsInput(ItemStack var1);

   boolean allowsInput(FluidStack var1);

   boolean allowsOutput(ItemStack var1);

   boolean allowsOutput(FluidStack var1);

   Collection<? extends BlockCapability<?, Direction>> getProvidedCapabilities();
}
