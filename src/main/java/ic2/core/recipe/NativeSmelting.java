package ic2.core.recipe;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;

public final class NativeSmelting {
   private NativeSmelting() {
   }

   public static ItemStack result(Level level, ItemStack ingredient) {
      if (level instanceof ServerLevel server && !ingredient.isEmpty()) {
         SingleRecipeInput input = new SingleRecipeInput(ingredient);
         return server.recipeAccess()
            .getRecipeFor(RecipeType.SMELTING, input, server)
            .map(holder -> ((SmeltingRecipe)holder.value()).assemble(input))
            .orElse(ItemStack.EMPTY);
      } else {
         return ItemStack.EMPTY;
      }
   }
}
