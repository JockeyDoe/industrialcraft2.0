package ic2.jeiIntegration.recipe.machine;

import ic2.jeiIntegration.RecipePresentation;

public class ElectrolyzerRecipeHandler implements RecipePresentation.IRecipeHandler<ElectrolyzerWrapper> {
   @Override
   public Class<ElectrolyzerWrapper> getRecipeClass() {
      return ElectrolyzerWrapper.class;
   }

   public String getRecipeCategoryUid(ElectrolyzerWrapper recipe) {
      return recipe.category.getUid();
   }

   public RecipePresentation.IRecipeWrapper getRecipeWrapper(ElectrolyzerWrapper recipe) {
      return recipe;
   }

   public boolean isRecipeValid(ElectrolyzerWrapper recipe) {
      return recipe.getFluidInput() != null && !recipe.getFluidOutputs().isEmpty();
   }
}
