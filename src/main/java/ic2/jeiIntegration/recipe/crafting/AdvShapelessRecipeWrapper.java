package ic2.jeiIntegration.recipe.crafting;

import ic2.api.recipe.IRecipeInput;
import ic2.core.recipe.AdvShapelessRecipe;
import ic2.core.ref.ItemName;
import ic2.core.util.Ic2Color;
import ic2.core.util.ItemStacks;
import ic2.jeiIntegration.RecipePresentation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.world.item.ItemStack;

public class AdvShapelessRecipeWrapper extends RecipePresentation.BlankRecipeWrapper {
   private final AdvShapelessRecipe recipe;

   public AdvShapelessRecipeWrapper(AdvShapelessRecipe recipe) {
      this.recipe = recipe;
   }

   public List<List<ItemStack>> getInputs() {
      List<List<ItemStack>> ret = new ArrayList<>(this.recipe.input.length);

      for (IRecipeInput input : this.recipe.input) {
         ret.add(input.getInputs());
      }

      if (ret.size() == 1 && ret.get(0).size() == 1) {
         ItemStack stack = ret.get(0).get(0);
         if (stack.getItem() == ItemName.painter.getInstance() && ItemStacks.rawMeta(stack) == 32767) {
            ret.set(0, Arrays.stream(Ic2Color.values).map(ItemName.painter::getItemStack).collect(Collectors.toList()));
         }
      }

      return ret;
   }

   @Override
   public void getIngredients(RecipePresentation.IIngredients ingredients) {
      ingredients.setInputLists(ItemStack.class, this.getInputs());
      ingredients.setOutput(ItemStack.class, this.recipe.func_77571_b());
   }
}
