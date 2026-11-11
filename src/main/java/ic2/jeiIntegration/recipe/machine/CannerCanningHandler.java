package ic2.jeiIntegration.recipe.machine;

import ic2.core.util.StackUtil;
import ic2.jeiIntegration.RecipePresentation;

public class CannerCanningHandler implements RecipePresentation.IRecipeHandler<CannerCanningWrapper> {
   @Override
   public Class<CannerCanningWrapper> getRecipeClass() {
      return CannerCanningWrapper.class;
   }

   public String getRecipeCategoryUid(CannerCanningWrapper recipe) {
      return recipe.category.getUid();
   }

   public RecipePresentation.IRecipeWrapper getRecipeWrapper(CannerCanningWrapper recipe) {
      return recipe;
   }

   public boolean isRecipeValid(CannerCanningWrapper recipe) {
      return !recipe.getInput().isEmpty() && !recipe.getCan().isEmpty() && !StackUtil.isEmpty(recipe.getOutput());
   }
}
