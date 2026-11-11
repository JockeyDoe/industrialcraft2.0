package ic2.jeiIntegration.recipe.misc;

import ic2.jeiIntegration.RecipePresentation;

public class ScrapboxRecipeHandler implements RecipePresentation.IRecipeHandler<ScrapboxRecipeWrapper> {
   @Override
   public Class<ScrapboxRecipeWrapper> getRecipeClass() {
      return ScrapboxRecipeWrapper.class;
   }

   public String getRecipeCategoryUid(ScrapboxRecipeWrapper recipe) {
      return "ic2.scrapbox";
   }

   public RecipePresentation.IRecipeWrapper getRecipeWrapper(ScrapboxRecipeWrapper recipe) {
      return recipe;
   }

   public boolean isRecipeValid(ScrapboxRecipeWrapper recipe) {
      return true;
   }
}
