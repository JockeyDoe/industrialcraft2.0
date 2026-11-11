package ic2.core.recipe.dynamic;

import ic2.core.util.LiquidUtil;
import net.neoforged.neoforge.fluids.FluidStack;

public class RecipeOutputFluidStack extends RecipeOutputIngredient<FluidStack> {
   public static RecipeOutputFluidStack of(FluidStack ingredient) {
      return new RecipeOutputFluidStack(ingredient);
   }

   protected RecipeOutputFluidStack(FluidStack ingredient) {
      super(ingredient);
   }

   @Override
   public RecipeOutputIngredient<FluidStack> copy() {
      return of(this.ingredient.copy());
   }

   @Override
   public boolean isEmpty() {
      return this.ingredient.getAmount() <= 0;
   }

   @Override
   public boolean matches(Object other) {
      return !(other instanceof FluidStack) ? false : FluidStack.matches(this.ingredient, (FluidStack)other);
   }

   @Override
   public boolean matchesStrict(Object other) {
      return this.matches(other);
   }

   @Override
   public String toStringSafe() {
      return LiquidUtil.toStringSafe(this.ingredient);
   }
}
