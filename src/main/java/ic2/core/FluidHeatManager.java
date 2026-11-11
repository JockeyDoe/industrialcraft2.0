package ic2.core;

import ic2.api.recipe.IFluidHeatManager;
import ic2.core.util.FluidKeys;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.level.material.Fluid;

public class FluidHeatManager implements IFluidHeatManager {
   private final Map<String, IFluidHeatManager.BurnProperty> burnProperties = new HashMap<>();

   @Override
   public void addFluid(String fluidName, int amount, int heat) {
      if (this.burnProperties.containsKey(fluidName)) {
         throw new RuntimeException("The fluid " + fluidName + " does already have a burn property assigned.");
      } else {
         this.burnProperties.put(fluidName, new IFluidHeatManager.BurnProperty(amount, heat));
      }
   }

   @Override
   public IFluidHeatManager.BurnProperty getBurnProperty(Fluid fluid) {
      return fluid == null ? null : this.burnProperties.get(FluidKeys.name(fluid));
   }

   @Override
   public boolean acceptsFluid(Fluid fluid) {
      return this.burnProperties.containsKey(FluidKeys.name(fluid));
   }

   @Override
   public Set<Fluid> getAcceptedFluids() {
      Set<Fluid> ret = new HashSet<>();

      for (String fluidName : this.burnProperties.keySet()) {
         Fluid fluid = FluidKeys.find(fluidName);
         if (fluid != null) {
            ret.add(fluid);
         }
      }

      return ret;
   }

   @Override
   public Map<String, IFluidHeatManager.BurnProperty> getBurnProperties() {
      return this.burnProperties;
   }
}
