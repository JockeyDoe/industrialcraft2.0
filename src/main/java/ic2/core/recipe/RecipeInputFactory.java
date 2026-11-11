package ic2.core.recipe;

import com.google.common.collect.Iterables;
import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.IRecipeInputFactory;
import ic2.core.util.ItemStacks;
import java.util.Collection;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;

public class RecipeInputFactory implements IRecipeInputFactory {
   @Override
   public IRecipeInput forStack(ItemStack stack) {
      return new RecipeInputItemStack(stack);
   }

   @Override
   public IRecipeInput forStack(ItemStack stack, int amount) {
      return new RecipeInputItemStack(stack, amount);
   }

   @Override
   public IRecipeInput forExactStack(ItemStack stack) {
      return (IRecipeInput)(ItemStacks.rawMeta(stack) == 32767 ? this.forStack(stack) : new RecipeInputItemStackExact(stack));
   }

   @Override
   public IRecipeInput forExactStack(ItemStack stack, int amount) {
      return (IRecipeInput)(ItemStacks.rawMeta(stack) == 32767 ? this.forStack(stack, amount) : new RecipeInputItemStackExact(stack, amount));
   }

   @Override
   public IRecipeInput forOreDict(String name) {
      return new RecipeInputOreDict(name);
   }

   @Override
   public IRecipeInput forOreDict(String name, int amount) {
      return new RecipeInputOreDict(name, amount);
   }

   @Override
   public IRecipeInput forOreDict(String name, int amount, int metaOverride) {
      return new RecipeInputOreDict(name, amount, metaOverride);
   }

   @Override
   public IRecipeInput forFluidContainer(Fluid fluid) {
      return new RecipeInputFluidContainer(fluid);
   }

   @Override
   public IRecipeInput forFluidContainer(Fluid fluid, int amount) {
      return new RecipeInputFluidContainer(fluid, amount);
   }

   @Override
   public IRecipeInput forAny(IRecipeInput... options) {
      return new RecipeInputMultiple(options);
   }

   @Override
   public IRecipeInput forAny(Iterable<IRecipeInput> options) {
      return options instanceof Collection
         ? new RecipeInputMultiple(((Collection)options).toArray(new IRecipeInput[0]))
         : new RecipeInputMultiple((IRecipeInput[])Iterables.toArray(options, IRecipeInput.class));
   }

   @Override
   public Ingredient getIngredient(IRecipeInput input) {
      return new IngredientRecipeInput(input).toVanilla();
   }

   @Override
   public IRecipeInput forIngredient(Ingredient ingredient) {
      return new RecipeInputIngredient(ingredient);
   }
}
