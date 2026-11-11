package ic2.jeiIntegration.recipe.crafting;

import ic2.api.recipe.IRecipeInput;
import ic2.core.recipe.AdvRecipe;
import ic2.jeiIntegration.RecipePresentation;
import javax.annotation.Nonnull;

public class AdvRecipeHandler implements RecipePresentation.IRecipeHandler<AdvRecipe> {
   @Nonnull
   @Override
   public Class<AdvRecipe> getRecipeClass() {
      return AdvRecipe.class;
   }

   @Nonnull
   public String getRecipeCategoryUid(AdvRecipe recipe) {
      return "minecraft.crafting";
   }

   @Nonnull
   public RecipePresentation.IRecipeWrapper getRecipeWrapper(@Nonnull AdvRecipe recipe) {
      return new AdvRecipeWrapper(recipe);
   }

   public boolean isRecipeValid(@Nonnull AdvRecipe recipe) {
      if (!recipe.canShow()) {
         return false;
      } else {
         for (IRecipeInput input : recipe.input) {
            if (input.getInputs().isEmpty()) {
               return false;
            }
         }

         return true;
      }
   }
}
