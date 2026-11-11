package ic2.jeiIntegration.recipe.misc;

import ic2.core.init.Localization;
import ic2.jeiIntegration.RecipePresentation;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public class ScrapboxRecipeCategory extends RecipePresentation.BlankRecipeCategory<RecipePresentation.IRecipeWrapper> {
   public static final String UID = "ic2.scrapbox";
   private final IDrawable background;

   public ScrapboxRecipeCategory(IGuiHelper guiHelper) {
      this.background = guiHelper.createDrawable(Identifier.parse("ic2:textures/gui/scrapboxrecipes.png"), 55, 30, 82, 26);
   }

   @Override
   public String getUid() {
      return "ic2.scrapbox";
   }

   @Override
   public String getTitle() {
      return Localization.translate("ic2.crafting.scrap_box");
   }

   @Override
   public IDrawable getBackground() {
      return this.background;
   }

   @Override
   public void setRecipe(
      RecipePresentation.IRecipeLayout recipeLayout, RecipePresentation.IRecipeWrapper recipeWrapper, RecipePresentation.IIngredients ingredients
   ) {
      RecipePresentation.IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();
      itemStacks.init(0, true, 0, 4);
      itemStacks.init(1, false, 60, 4);
      itemStacks.set(0, ingredients.getInputs(ItemStack.class).get(0));
      itemStacks.set(1, ingredients.getOutputs(ItemStack.class).get(0));
   }

   @Override
   public String getModName() {
      return "ic2";
   }
}
