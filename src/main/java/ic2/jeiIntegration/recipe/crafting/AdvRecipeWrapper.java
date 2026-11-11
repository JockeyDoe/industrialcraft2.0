package ic2.jeiIntegration.recipe.crafting;

import ic2.api.item.IElectricItem;
import ic2.api.recipe.IRecipeInput;
import ic2.core.recipe.AdvRecipe;
import ic2.core.util.StackUtil;
import ic2.jeiIntegration.RecipePresentation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import net.minecraft.world.item.ItemStack;

public class AdvRecipeWrapper extends RecipePresentation.BlankRecipeWrapper implements RecipePresentation.IShapedCraftingRecipeWrapper {
   private final AdvRecipe recipe;

   public AdvRecipeWrapper(AdvRecipe recipe) {
      this.recipe = recipe;
   }

   public List<List<ItemStack>> getInputs() {
      int mask = this.recipe.masks[0];
      int itemIndex = 0;
      List<IRecipeInput> ret = new ArrayList<>();

      for (int i = 0; i < 9; i++) {
         if (i % 3 < this.recipe.inputWidth && i / 3 < this.recipe.inputHeight) {
            if ((mask >>> 8 - i & 1) != 0) {
               ret.add(this.recipe.input[itemIndex++]);
            } else {
               ret.add(null);
            }
         }
      }

      return replaceRecipeInputs(ret);
   }

   @Override
   public int getWidth() {
      return this.recipe.inputWidth;
   }

   @Override
   public int getHeight() {
      return this.recipe.inputHeight;
   }

   public static List<List<ItemStack>> replaceRecipeInputs(List<IRecipeInput> list) {
      List<List<ItemStack>> out = new ArrayList<>(list.size());

      for (IRecipeInput recipe : list) {
         if (recipe == null) {
            out.add(Collections.emptyList());
         } else {
            List<ItemStack> replace = new ArrayList<>(recipe.getInputs());
            ListIterator<ItemStack> it = replace.listIterator();

            while (it.hasNext()) {
               ItemStack stack = it.next();
               if (stack != null && stack.getItem() instanceof IElectricItem) {
                  it.set(StackUtil.copyWithWildCard(stack));
               }
            }

            out.add(replace);
         }
      }

      return out;
   }

   @Override
   public void getIngredients(RecipePresentation.IIngredients ingredients) {
      ingredients.setInputLists(ItemStack.class, this.getInputs());
      ingredients.setOutput(ItemStack.class, this.recipe.func_77571_b());
   }
}
