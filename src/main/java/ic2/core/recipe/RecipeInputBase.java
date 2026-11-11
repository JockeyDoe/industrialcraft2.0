package ic2.core.recipe;

import ic2.api.recipe.IRecipeInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public abstract class RecipeInputBase implements IRecipeInput {
   @Override
   public int getAmount() {
      return 1;
   }

   @Override
   public Ingredient getIngredient() {
      return new IngredientRecipeInput(this).toVanilla();
   }

   public ItemStack[] func_193365_a() {
      return this.getInputs().toArray(ItemStack[]::new);
   }

   public boolean apply(ItemStack stack) {
      return stack != null && !stack.isEmpty() && this.matches(stack);
   }

   public void invalidate() {
   }
}
