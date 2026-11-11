package ic2.jeiIntegration.recipe.crafting;

import ic2.core.init.Localization;
import ic2.core.recipe.GradualRecipe;
import ic2.core.util.Util;
import ic2.jeiIntegration.RecipePresentation;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

public class GradualRecipeWrapper extends RecipePresentation.BlankRecipeWrapper {
   private final GradualRecipe recipe;

   public GradualRecipeWrapper(GradualRecipe recipe) {
      this.recipe = recipe;
   }

   public List<ItemStack> getInputs() {
      List<ItemStack> ret = new ArrayList<>(2);
      ret.add(this.recipe.chargeMaterial);
      ItemStack repairItem = this.recipe.func_77571_b();
      this.recipe.item.setCustomDamage(repairItem, this.recipe.amount);
      ret.add(repairItem);
      return ret;
   }

   @Override
   public void drawInfo(GuiGraphicsExtractor minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
      assert this.recipe.item.getMaxCustomDamage(this.recipe.func_77571_b()) > 0;

      String effectiveness = Localization.translate(
         "ic2.jei.condenser", Util.limit((float)this.recipe.amount / this.recipe.item.getMaxCustomDamage(this.recipe.func_77571_b()) * 100.0F, 0.0F, 100.0F)
      );
      int width = Minecraft.getInstance().font.width(effectiveness);
      if (143 - width < 55) {
         minecraft.text(Minecraft.getInstance().font, effectiveness, 55, 88, Color.darkGray.getRGB(), false);
      } else {
         minecraft.text(Minecraft.getInstance().font, effectiveness, (55 + (143 - width)) / 2, 42, Color.darkGray.getRGB());
      }
   }

   @Override
   public void getIngredients(RecipePresentation.IIngredients ingredients) {
      ingredients.setInputs(ItemStack.class, this.getInputs());
      ingredients.setOutput(ItemStack.class, this.recipe.func_77571_b());
   }
}
