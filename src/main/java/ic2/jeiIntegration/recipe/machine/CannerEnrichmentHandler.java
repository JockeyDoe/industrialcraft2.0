package ic2.jeiIntegration.recipe.machine;

import ic2.jeiIntegration.RecipePresentation;

public class CannerEnrichmentHandler implements RecipePresentation.IRecipeHandler<CannerEnrichmentWrapper> {
   @Override
   public Class<CannerEnrichmentWrapper> getRecipeClass() {
      return CannerEnrichmentWrapper.class;
   }

   public String getRecipeCategoryUid(CannerEnrichmentWrapper recipe) {
      return recipe.category.getUid();
   }

   public RecipePresentation.IRecipeWrapper getRecipeWrapper(CannerEnrichmentWrapper recipe) {
      return recipe;
   }

   public boolean isRecipeValid(CannerEnrichmentWrapper recipe) {
      return !recipe.getAdditives().isEmpty() && recipe.getInput() != null && recipe.getOutput() != null;
   }
}
