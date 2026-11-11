package ic2.jeiIntegration.recipe.crafting;

import ic2.api.recipe.IRecipeInput;
import ic2.core.recipe.AdvShapelessRecipe;
import ic2.jeiIntegration.RecipePresentation;
import javax.annotation.Nonnull;

public class AdvShapelessRecipeHandler implements RecipePresentation.IRecipeHandler<AdvShapelessRecipe> {
   @Nonnull
   @Override
   public Class<AdvShapelessRecipe> getRecipeClass() {
      return AdvShapelessRecipe.class;
   }

   @Nonnull
   public String getRecipeCategoryUid(AdvShapelessRecipe recipe) {
      return "minecraft.crafting";
   }

   @Nonnull
   public RecipePresentation.IRecipeWrapper getRecipeWrapper(@Nonnull AdvShapelessRecipe recipe) {
      return new AdvShapelessRecipeWrapper(recipe);
   }

   public boolean isRecipeValid(@Nonnull AdvShapelessRecipe recipe) {
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
