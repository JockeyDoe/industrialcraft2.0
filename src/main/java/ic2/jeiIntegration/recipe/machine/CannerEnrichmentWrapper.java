package ic2.jeiIntegration.recipe.machine;

import ic2.api.recipe.ICannerEnrichRecipeManager;
import ic2.api.recipe.IRecipeInput;
import ic2.jeiIntegration.RecipePresentation;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public class CannerEnrichmentWrapper extends RecipePresentation.BlankRecipeWrapper {
   private final FluidStack input;
   private final FluidStack output;
   private final IRecipeInput additive;
   final IORecipeCategory<ICannerEnrichRecipeManager> category;

   CannerEnrichmentWrapper(ICannerEnrichRecipeManager.Input input, FluidStack output, IORecipeCategory<ICannerEnrichRecipeManager> category) {
      this.input = input.fluid;
      this.additive = input.additive;
      this.output = output;
      this.category = category;
   }

   public FluidStack getInput() {
      return this.input;
   }

   public List<ItemStack> getAdditives() {
      return this.additive.getInputs();
   }

   public FluidStack getOutput() {
      return this.output;
   }

   @Override
   public void getIngredients(RecipePresentation.IIngredients ingredients) {
      ingredients.setInput(FluidStack.class, this.getInput());
      ingredients.setInputs(ItemStack.class, this.getAdditives());
      ingredients.setOutput(FluidStack.class, this.getOutput());
   }
}
