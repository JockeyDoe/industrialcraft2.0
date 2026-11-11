package ic2.core.recipe;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.CraftingInput.Positioned;
import net.minecraft.world.level.Level;

public final class NativeCrafting {
   private NativeCrafting() {
   }

   public static CraftingRecipe find(CraftingContainer grid, Level level) {
      return level instanceof ServerLevel server
         ? server.recipeAccess().getRecipeFor(RecipeType.CRAFTING, grid.asCraftInput(), server).map(holder -> (CraftingRecipe)holder.value()).orElse(null)
         : null;
   }

   public static boolean matches(Recipe<?> recipe, CraftingContainer grid, Level level) {
      return recipe instanceof LegacyCraftingRecipe legacy ? legacy.matches(grid, level) : recipe.matches(grid.asCraftInput(), level);
   }

   public static ItemStack assemble(Recipe<?> recipe, CraftingContainer grid) {
      return recipe instanceof LegacyCraftingRecipe legacy ? legacy.func_77572_b(grid) : recipe.assemble(grid.asCraftInput());
   }

   public static ItemStack result(CraftingContainer grid, Level level) {
      CraftingRecipe recipe = find(grid, level);
      return recipe == null ? ItemStack.EMPTY : assemble(recipe, grid);
   }

   public static NonNullList<ItemStack> remaining(Recipe<?> recipe, CraftingContainer grid) {
      if (recipe instanceof LegacyCraftingRecipe legacy) {
         return legacy.func_179532_b(grid);
      } else {
         Positioned positioned = grid.asPositionedCraftInput();
         CraftingInput input = positioned.input();
         NonNullList<ItemStack> remainders = ((CraftingRecipe)recipe).getRemainingItems(input);
         NonNullList<ItemStack> result = NonNullList.withSize(grid.getContainerSize(), ItemStack.EMPTY);

         for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
               result.set(x + positioned.left() + (y + positioned.top()) * grid.getWidth(), (ItemStack)remainders.get(x + y * input.width()));
            }
         }

         return result;
      }
   }
}
