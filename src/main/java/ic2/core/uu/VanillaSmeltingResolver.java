package ic2.core.uu;

import ic2.core.IC2;
import ic2.core.recipe.NativeRecipePolicy;
import ic2.core.recipe.NativeRecipes;
import ic2.core.util.LogCategory;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;

public class VanillaSmeltingResolver implements IRecipeResolver {
   private static final double transformCost = 14.0;

   @Override
   public List<RecipeTransformation> getTransformations() {
      List<RecipeTransformation> ret = new ArrayList<>();

      for (Recipe<?> recipe : NativeRecipes.all()) {
         if (recipe instanceof SmeltingRecipe smelting) {
            try {
               List<List<LeanItemStack>> inputs = RecipeUtil.convertIngredients(NativeRecipes.examples(smelting.input()));
               LeanItemStack output = new LeanItemStack(NativeRecipePolicy.output(smelting));
               ret.add(new RecipeTransformation(14.0, inputs, output));
            } catch (IllegalArgumentException var7) {
               IC2.log.warn(LogCategory.Uu, var7, "invalid recipe");
            }
         }
      }

      return ret;
   }
}
