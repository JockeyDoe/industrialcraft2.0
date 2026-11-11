package ic2.jeiIntegration.recipe.machine;

import ic2.core.block.ITeBlock;
import ic2.core.block.TeBlockRegistry;
import ic2.jeiIntegration.RecipePresentation;
import ic2.jeiIntegration.SlotPosition;
import java.util.List;
import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

public abstract class IORecipeCategory<T> implements RecipePresentation.IRecipeCategory<RecipePresentation.IRecipeWrapper> {
   protected final ITeBlock block;
   final T recipeManager;

   public IORecipeCategory(ITeBlock block, T recipeManager) {
      this.block = block;
      this.recipeManager = recipeManager;
   }

   @Override
   public String getUid() {
      return this.block.getName();
   }

   @Override
   public String getTitle() {
      return this.getBlockStack().getHoverName().getString();
   }

   @Override
   public void drawExtras(GuiGraphicsExtractor minecraft) {
   }

   protected abstract List<SlotPosition> getInputSlotPos();

   protected abstract List<SlotPosition> getOutputSlotPos();

   protected List<List<ItemStack>> getInputStacks(RecipePresentation.IIngredients ingredients) {
      return ingredients.getInputs(ItemStack.class);
   }

   protected List<List<ItemStack>> getOutputStacks(RecipePresentation.IIngredients ingredients) {
      return ingredients.getOutputs(ItemStack.class);
   }

   @Override
   public void setRecipe(
      RecipePresentation.IRecipeLayout recipeLayout, RecipePresentation.IRecipeWrapper recipeWrapper, RecipePresentation.IIngredients ingredients
   ) {
      RecipePresentation.IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();
      List<SlotPosition> inputSlots = this.getInputSlotPos();
      List<List<ItemStack>> inputStacks = this.getInputStacks(ingredients);

      int idx;
      for (idx = 0; idx < inputSlots.size(); idx++) {
         SlotPosition pos = inputSlots.get(idx);
         itemStacks.init(idx, true, pos.getX(), pos.getY());
         if (idx < inputStacks.size()) {
            itemStacks.set(idx, inputStacks.get(idx));
         }
      }

      List<SlotPosition> outputSlots = this.getOutputSlotPos();
      List<List<ItemStack>> outputStacks = this.getOutputStacks(ingredients);

      for (int i = 0; i < outputSlots.size(); idx++) {
         SlotPosition pos = outputSlots.get(i);
         itemStacks.init(idx, false, pos.getX(), pos.getY());
         if (i < outputStacks.size()) {
            itemStacks.set(idx, outputStacks.get(i));
         }

         i++;
      }
   }

   public ItemStack getBlockStack() {
      return TeBlockRegistry.get(this.block.getIdentifier()).getItemStack(this.block);
   }

   @Override
   public IDrawable getIcon() {
      return null;
   }

   @Override
   public String getModName() {
      return "ic2";
   }
}
