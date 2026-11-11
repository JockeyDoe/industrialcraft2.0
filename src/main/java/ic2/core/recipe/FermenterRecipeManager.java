package ic2.core.recipe;

import ic2.api.recipe.IFermenterRecipeManager;
import ic2.core.util.FluidKeys;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

public class FermenterRecipeManager implements IFermenterRecipeManager {
   private final Map<String, IFermenterRecipeManager.FermentationProperty> fluidMap = new HashMap<>();

   @Override
   public void addRecipe(String input, int inputAmount, int heat, String output, int outputAmount) {
      if (this.fluidMap.containsKey(input)) {
         throw new RuntimeException("The fluid " + input + " already has an output assigned.");
      } else {
         this.fluidMap.put(input, new IFermenterRecipeManager.FermentationProperty(inputAmount, heat, output, outputAmount));
      }
   }

   @Override
   public IFermenterRecipeManager.FermentationProperty getFermentationInformation(Fluid fluid) {
      return fluid == null ? null : this.fluidMap.get(FluidKeys.name(fluid));
   }

   @Override
   public FluidStack getOutput(Fluid input) {
      IFermenterRecipeManager.FermentationProperty fp = this.getFermentationInformation(input);
      if (fp == null) {
         return null;
      } else {
         return FluidKeys.find(fp.output) == null ? null : new FluidStack(FluidKeys.find(fp.output), fp.outputAmount);
      }
   }

   @Override
   public boolean acceptsFluid(Fluid fluid) {
      return fluid != null && this.fluidMap.containsKey(FluidKeys.name(fluid));
   }

   @Override
   public Set<Fluid> getAcceptedFluids() {
      Set<Fluid> ret = new HashSet<>(this.fluidMap.size() * 2, 0.5F);

      for (String fluidName : this.fluidMap.keySet()) {
         Fluid fluid = FluidKeys.find(fluidName);
         if (fluid != null) {
            ret.add(fluid);
         }
      }

      return ret;
   }

   @Override
   public Map<String, IFermenterRecipeManager.FermentationProperty> getRecipeMap() {
      return Collections.unmodifiableMap(this.fluidMap);
   }
}
