package ic2.core.recipe.dynamic;

import ic2.core.util.LiquidUtil;
import net.neoforged.neoforge.fluids.FluidStack;

public class RecipeInputFluidStack extends RecipeInputIngredient<FluidStack> {
   public static RecipeInputFluidStack of(FluidStack ingredient) {
      return new RecipeInputFluidStack(ingredient);
   }

   protected RecipeInputFluidStack(FluidStack ingredient) {
      super(ingredient);
   }

   @Override
   public Object getUnspecific() {
      return this.ingredient.getFluid();
   }

   @Override
   public RecipeInputIngredient<FluidStack> copy() {
      return of(this.ingredient.copy());
   }

   @Override
   public boolean isEmpty() {
      return this.ingredient.getAmount() <= 0;
   }

   @Override
   public int getCount() {
      return this.ingredient.getAmount();
   }

   @Override
   public void shrink(int amount) {
      this.ingredient.setAmount(this.ingredient.getAmount() - amount);
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
