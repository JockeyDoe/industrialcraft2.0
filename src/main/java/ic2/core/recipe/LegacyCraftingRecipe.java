package ic2.core.recipe;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.level.Level;

public interface LegacyCraftingRecipe extends CraftingRecipe {
   ItemStack func_77572_b(CraftingContainer var1);

   ItemStack func_77571_b();

   boolean matches(CraftingContainer var1, Level var2);

   LegacyCraftingRecipe setRegistryName(Identifier var1);

   Identifier getRegistryName();

   default NonNullList<ItemStack> func_179532_b(CraftingContainer input) {
      NonNullList<ItemStack> result = NonNullList.withSize(input.getContainerSize(), ItemStack.EMPTY);

      for (int i = 0; i < result.size(); i++) {
         ItemStackTemplate remainder = input.getItem(i).getCraftingRemainder();
         if (remainder != null) {
            result.set(i, remainder.create());
         }
      }

      return result;
   }

   default ItemStack assemble(CraftingInput input) {
      if (input.width() <= 3 && input.height() <= 3 && !input.isEmpty()) {
         for (int top = 0; top <= 3 - input.height(); top++) {
            for (int left = 0; left <= 3 - input.width(); left++) {
               ItemStack result = this.func_77572_b(new RecipeGrid(input, left, top));
               if (!result.isEmpty()) {
                  return result;
               }
            }
         }

         return ItemStack.EMPTY;
      } else {
         return ItemStack.EMPTY;
      }
   }

   default boolean matches(CraftingInput input, Level level) {
      if (input.width() <= 3 && input.height() <= 3 && !input.isEmpty()) {
         for (int top = 0; top <= 3 - input.height(); top++) {
            for (int left = 0; left <= 3 - input.width(); left++) {
               if (this.matches(new RecipeGrid(input, left, top), level)) {
                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   default NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
      RecipeGrid grid = new RecipeGrid(input);
      NonNullList<ItemStack> full = this.func_179532_b(grid);
      NonNullList<ItemStack> result = NonNullList.withSize(input.size(), ItemStack.EMPTY);

      for (int y = 0; y < input.height(); y++) {
         for (int x = 0; x < input.width(); x++) {
            result.set(x + y * input.width(), (ItemStack)full.get(x + y * grid.getWidth()));
         }
      }

      return result;
   }

   default CraftingBookCategory category() {
      return CraftingBookCategory.MISC;
   }

   default String group() {
      return "";
   }

   default boolean showNotification() {
      return false;
   }

   default PlacementInfo placementInfo() {
      return PlacementInfo.NOT_PLACEABLE;
   }

   default boolean isSpecial() {
      return true;
   }
}
