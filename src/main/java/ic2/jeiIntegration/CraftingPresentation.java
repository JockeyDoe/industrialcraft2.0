package ic2.jeiIntegration;

import ic2.core.init.Localization;
import java.util.List;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.world.item.ItemStack;

public final class CraftingPresentation implements RecipePresentation.IRecipeCategory<RecipePresentation.IRecipeWrapper> {
   private final IDrawable background;

   public CraftingPresentation(IGuiHelper helper) {
      this.background = helper.createBlankDrawable(116, 54);
   }

   @Override
   public String getUid() {
      return "minecraft.crafting";
   }

   @Override
   public String getTitle() {
      return Localization.translate("ic2.crafting");
   }

   @Override
   public IDrawable getBackground() {
      return this.background;
   }

   @Override
   public void setRecipe(RecipePresentation.IRecipeLayout layout, RecipePresentation.IRecipeWrapper recipe, RecipePresentation.IIngredients data) {
      List<List<ItemStack>> input = data.getInputs(ItemStack.class);
      RecipePresentation.IGuiItemStackGroup group = layout.getItemStacks();
      int width = recipe instanceof RecipePresentation.IShapedCraftingRecipeWrapper shaped ? shaped.getWidth() : 3;

      for (int i = 0; i < input.size(); i++) {
         group.init(i, true, i % width * 18, i / width * 18);
         group.set(i, input.get(i));
      }

      List<List<ItemStack>> output = data.getOutputs(ItemStack.class);

      for (int i = 0; i < output.size(); i++) {
         group.init(9 + i, false, 94, 18 + i * 18);
         group.set(9 + i, output.get(i));
      }
   }
}
