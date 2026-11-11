package ic2.core.recipe;

import ic2.api.recipe.IRecipeInput;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public class RecipeInputIngredient implements IRecipeInput {
   private Ingredient ingredient;

   RecipeInputIngredient(Ingredient ingredient) {
      this.ingredient = ingredient;
   }

   @Override
   public boolean matches(ItemStack subject) {
      return this.ingredient.test(subject);
   }

   @Override
   public List<ItemStack> getInputs() {
      return this.ingredient.items().map(holder -> new ItemStack(holder)).toList();
   }

   @Override
   public int getAmount() {
      return 1;
   }

   @Override
   public Ingredient getIngredient() {
      return this.ingredient;
   }
}
