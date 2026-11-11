package ic2.jeiIntegration.recipe.crafting;

import ic2.jeiIntegration.RecipePresentation;

public class JetpackRecipeHandler implements RecipePresentation.IRecipeHandler<JetpackRecipeWrapper> {
   @Override
   public Class<JetpackRecipeWrapper> getRecipeClass() {
      return JetpackRecipeWrapper.class;
   }

   public String getRecipeCategoryUid(JetpackRecipeWrapper recipe) {
      return "minecraft.crafting";
   }

   public RecipePresentation.IRecipeWrapper getRecipeWrapper(JetpackRecipeWrapper wrapper) {
      return wrapper;
   }

   public boolean isRecipeValid(JetpackRecipeWrapper recipe) {
      return true;
   }
}
