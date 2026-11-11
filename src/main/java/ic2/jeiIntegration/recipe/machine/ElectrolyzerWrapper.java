package ic2.jeiIntegration.recipe.machine;

import ic2.api.recipe.IElectrolyzerRecipeManager;
import ic2.jeiIntegration.RecipePresentation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.neoforged.neoforge.fluids.FluidStack;

public class ElectrolyzerWrapper extends RecipePresentation.BlankRecipeWrapper {
   private final FluidStack input;
   private final List<FluidStack> outputs;
   final IORecipeCategory<IElectrolyzerRecipeManager> category;

   ElectrolyzerWrapper(FluidStack input, IElectrolyzerRecipeManager.ElectrolyzerOutput[] outputs, IORecipeCategory<IElectrolyzerRecipeManager> category) {
      this.input = input;
      List<FluidStack> temp = new ArrayList<>(outputs.length);

      for (IElectrolyzerRecipeManager.ElectrolyzerOutput output : outputs) {
         temp.add(output.getOutput());
      }

      this.outputs = Collections.unmodifiableList(temp);
      this.category = category;
   }

   public FluidStack getFluidInput() {
      return this.input;
   }

   public List<FluidStack> getFluidOutputs() {
      return this.outputs;
   }

   @Override
   public void getIngredients(RecipePresentation.IIngredients ingredients) {
      ingredients.setInput(FluidStack.class, this.getFluidInput());
      ingredients.setOutputs(FluidStack.class, this.getFluidOutputs());
   }
}
