package ic2.jeiIntegration.recipe.machine;

import ic2.jeiIntegration.RecipePresentation;
import javax.annotation.Nonnull;

public class IORecipeHandler implements RecipePresentation.IRecipeHandler<IORecipeWrapper> {
   @Override
   public Class<IORecipeWrapper> getRecipeClass() {
      return IORecipeWrapper.class;
   }

   @Nonnull
   public RecipePresentation.IRecipeWrapper getRecipeWrapper(@Nonnull IORecipeWrapper recipe) {
      return recipe;
   }

   public boolean isRecipeValid(@Nonnull IORecipeWrapper recipe) {
      return !recipe.getInputs().isEmpty();
   }

   public String getRecipeCategoryUid(IORecipeWrapper recipe) {
      return recipe.category.getUid();
   }
}
