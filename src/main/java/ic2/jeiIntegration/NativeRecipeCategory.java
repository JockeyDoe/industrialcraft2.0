package ic2.jeiIntegration;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class NativeRecipeCategory implements IRecipeCategory<RecipePresentation.IRecipeWrapper> {
   private final RecipePresentation.IRecipeCategory<RecipePresentation.IRecipeWrapper> layout;
   private final IRecipeType<RecipePresentation.IRecipeWrapper> type;

   public NativeRecipeCategory(RecipePresentation.IRecipeCategory<RecipePresentation.IRecipeWrapper> layout) {
      this.layout = layout;
      this.type = IRecipeType.create("ic2", layout.getUid().replace("ic2.", ""), RecipePresentation.IRecipeWrapper.class);
   }

   public IRecipeType<RecipePresentation.IRecipeWrapper> getRecipeType() {
      return this.type;
   }

   public Component getTitle() {
      return Component.literal(this.layout.getTitle());
   }

   public int getWidth() {
      return this.layout.getBackground().getWidth();
   }

   public int getHeight() {
      return this.layout.getBackground().getHeight();
   }

   public IDrawable getIcon() {
      return this.layout.getIcon();
   }

   public void setRecipe(IRecipeLayoutBuilder builder, RecipePresentation.IRecipeWrapper recipe, IFocusGroup focuses) {
      RecipePresentation.IIngredients data = new RecipePresentation.IIngredients();
      recipe.getIngredients(data);
      this.layout.setRecipe(new RecipePresentation.IRecipeLayout(builder), recipe, data);
   }

   public void draw(RecipePresentation.IRecipeWrapper recipe, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double x, double y) {
      this.layout.getBackground().draw(graphics);
      this.layout.drawExtras(graphics);
      recipe.drawInfo(graphics, this.getWidth(), this.getHeight(), (int)x, (int)y);
   }
}
