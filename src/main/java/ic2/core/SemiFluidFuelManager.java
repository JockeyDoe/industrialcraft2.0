package ic2.core;

import ic2.api.recipe.ISemiFluidFuelManager;
import ic2.core.util.FluidKeys;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.level.material.Fluid;

public class SemiFluidFuelManager implements ISemiFluidFuelManager {
   private final Map<String, ISemiFluidFuelManager.FuelProperty> fuelProperties = new HashMap<>();

   @Override
   public void addFluid(String fluidName, long energyPerMb, long energyPerTick) {
      if (this.fuelProperties.containsKey(fluidName)) {
         throw new RuntimeException("The fluid " + fluidName + " does already have a fuel property assigned.");
      } else {
         this.fuelProperties.put(fluidName, new ISemiFluidFuelManager.FuelProperty(energyPerMb, energyPerTick));
      }
   }

   @Override
   public void removeFluid(String fluidName) {
      this.fuelProperties.remove(fluidName);
   }

   @Override
   public ISemiFluidFuelManager.FuelProperty getFuelProperty(Fluid fluid) {
      return fluid == null ? null : this.fuelProperties.get(FluidKeys.name(fluid));
   }

   @Override
   public boolean acceptsFluid(Fluid fluid) {
      return fluid != null && this.fuelProperties.containsKey(FluidKeys.name(fluid));
   }

   @Override
   public Set<Fluid> getAcceptedFluids() {
      Set<Fluid> ret = new HashSet<>(this.fuelProperties.size() * 2, 0.5F);

      for (String fluidName : this.fuelProperties.keySet()) {
         Fluid fluid = FluidKeys.find(fluidName);
         if (fluid != null) {
            ret.add(fluid);
         }
      }

      return ret;
   }

   @Override
   public Map<String, ISemiFluidFuelManager.FuelProperty> getFuelProperties() {
      return Collections.unmodifiableMap(this.fuelProperties);
   }
}
