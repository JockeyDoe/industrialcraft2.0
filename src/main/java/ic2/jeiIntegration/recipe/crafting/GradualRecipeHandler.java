package ic2.jeiIntegration.recipe.crafting;

import ic2.core.recipe.GradualRecipe;
import ic2.jeiIntegration.RecipePresentation;

public class GradualRecipeHandler implements RecipePresentation.IRecipeHandler<GradualRecipe> {
   @Override
   public Class<GradualRecipe> getRecipeClass() {
      return GradualRecipe.class;
   }

   public String getRecipeCategoryUid(GradualRecipe recipe) {
      return "minecraft.crafting";
   }

   public RecipePresentation.IRecipeWrapper getRecipeWrapper(GradualRecipe recipe) {
      return new GradualRecipeWrapper(recipe);
   }

   public boolean isRecipeValid(GradualRecipe recipe) {
      return recipe.canShow() && recipe.chargeMaterial != null;
   }
}
