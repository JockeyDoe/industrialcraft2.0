package ic2.core.recipe;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.CraftingInput;

final class RecipeGrid extends SimpleContainer implements CraftingContainer {
   RecipeGrid(CraftingInput input) {
      this(input, 0, 0);
   }

   RecipeGrid(CraftingInput input, int left, int top) {
      super(9);
      if (input.width() <= 3 && input.height() <= 3) {
         for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
               this.setItem(left + x + (top + y) * 3, input.getItem(x, y));
            }
         }
      } else {
         throw new IllegalArgumentException("IC2 crafting grid exceeds 3x3");
      }
   }

   public int getWidth() {
      return 3;
   }

   public int getHeight() {
      return 3;
   }

   public void fillStackedContents(StackedItemContents contents) {
      this.getItems().forEach(contents::accountSimpleStack);
   }
}
