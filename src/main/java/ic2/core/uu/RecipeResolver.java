package ic2.core.uu;

import ic2.core.recipe.NativeRecipePolicy;
import ic2.core.recipe.NativeRecipes;
import ic2.core.util.StackUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

public class RecipeResolver implements IRecipeResolver {
   private static final double transformCost = 1.0;

   @Override
   public List<RecipeTransformation> getTransformations() {
      List<RecipeTransformation> ret = new ArrayList<>();

      for (Recipe<?> irecipe : NativeRecipes.all()) {
         if (irecipe instanceof CraftingRecipe && !irecipe.isSpecial()) {
            List<Ingredient> inputs = irecipe.placementInfo().ingredients();
            ItemStack output = NativeRecipePolicy.output(irecipe);
            if (!StackUtil.isEmpty(output) && !inputs.isEmpty()) {
               ret.add(new RecipeTransformation(1.0, toDoubleStackList(inputs), new LeanItemStack(output)));
            }
         }
      }

      return ret;
   }

   private static List<List<LeanItemStack>> toDoubleStackList(List<Ingredient> list) {
      List<List<LeanItemStack>> ret = new ArrayList<>(list.size());

      for (Ingredient ingredient : list) {
         List<ItemStack> arr = NativeRecipes.examples(ingredient);
         List<LeanItemStack> toAdd = new ArrayList<>(arr.size());

         for (ItemStack stack : arr) {
            toAdd.add(new LeanItemStack(stack));
         }

         ret.add(toAdd);
      }

      return ret;
   }
}
