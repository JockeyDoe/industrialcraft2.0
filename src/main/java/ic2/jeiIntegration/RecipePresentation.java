package ic2.jeiIntegration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public final class RecipePresentation {
   private RecipePresentation() {
   }

   public abstract static class BlankRecipeCategory<T extends RecipePresentation.IRecipeWrapper> implements RecipePresentation.IRecipeCategory<T> {
   }

   public abstract static class BlankRecipeWrapper implements RecipePresentation.IRecipeWrapper {
   }

   public static class IGuiFluidStackGroup {
      private final IRecipeLayoutBuilder builder;
      private final Map<Integer, IRecipeSlotBuilder> slots = new HashMap<>();

      IGuiFluidStackGroup(IRecipeLayoutBuilder builder) {
         this.builder = builder;
      }

      public void init(int id, boolean input, int x, int y, int width, int height, int capacity, boolean showCapacity, IDrawable overlay) {
         IRecipeSlotBuilder slot = this.builder
            .addSlot(input ? RecipeIngredientRole.INPUT : RecipeIngredientRole.OUTPUT, x, y)
            .setFluidRenderer(capacity, showCapacity, width, height);
         if (overlay != null) {
            slot.setOverlay(overlay, 0, 0);
         }

         this.slots.put(id, slot);
      }

      public void set(int id, List<FluidStack> stacks) {
         for (FluidStack stack : stacks) {
            this.slots.get(id).addFluidStack(stack.getFluid(), stack.getAmount(), stack.getComponentsPatch());
         }
      }

      public void set(int id, FluidStack stack) {
         this.set(id, List.of(stack));
      }
   }

   public static class IGuiItemStackGroup {
      private final IRecipeLayoutBuilder builder;
      private final Map<Integer, IRecipeSlotBuilder> slots = new HashMap<>();

      IGuiItemStackGroup(IRecipeLayoutBuilder builder) {
         this.builder = builder;
      }

      public void init(int id, boolean input, int x, int y) {
         this.slots.put(id, this.builder.addSlot(input ? RecipeIngredientRole.INPUT : RecipeIngredientRole.OUTPUT, x, y));
      }

      public void set(int id, List<ItemStack> stacks) {
         this.slots.get(id).addItemStacks(stacks);
      }

      public void set(int id, ItemStack stack) {
         this.set(id, List.of(stack));
      }
   }

   public static class IIngredients {
      private final Map<Class<?>, List<?>> inputs = new HashMap<>();
      private final Map<Class<?>, List<?>> outputs = new HashMap<>();

      public <T> List<List<T>> getInputs(Class<T> type) {
         return (List<List<T>>)this.inputs.getOrDefault(type, List.of());
      }

      public <T> List<List<T>> getOutputs(Class<T> type) {
         return (List<List<T>>)this.outputs.getOrDefault(type, List.of());
      }

      public <T> void setInputLists(Class<T> type, List<List<T>> values) {
         this.inputs.put(type, values);
      }

      public <T> void setOutputLists(Class<T> type, List<List<T>> values) {
         this.outputs.put(type, values);
      }

      public <T> void setInputs(Class<T> type, List<T> values) {
         this.setInputLists(type, values.stream().map(List::of).toList());
      }

      public <T> void setOutputs(Class<T> type, List<T> values) {
         this.setOutputLists(type, values.stream().map(List::of).toList());
      }

      public <T> void setInput(Class<T> type, T value) {
         this.setInputs(type, List.of(value));
      }

      public <T> void setOutput(Class<T> type, T value) {
         this.setOutputs(type, List.of(value));
      }
   }

   public interface IRecipeCategory<T extends RecipePresentation.IRecipeWrapper> {
      String getUid();

      String getTitle();

      IDrawable getBackground();

      default IDrawable getIcon() {
         return null;
      }

      default String getModName() {
         return "ic2";
      }

      default void drawExtras(GuiGraphicsExtractor graphics) {
      }

      void setRecipe(RecipePresentation.IRecipeLayout var1, T var2, RecipePresentation.IIngredients var3);
   }

   public interface IRecipeHandler<T> {
      Class<T> getRecipeClass();

      String getRecipeCategoryUid(T var1);

      RecipePresentation.IRecipeWrapper getRecipeWrapper(T var1);

      boolean isRecipeValid(T var1);
   }

   public static class IRecipeLayout {
      private final RecipePresentation.IGuiItemStackGroup items;
      private final RecipePresentation.IGuiFluidStackGroup fluids;

      public IRecipeLayout(IRecipeLayoutBuilder builder) {
         this.items = new RecipePresentation.IGuiItemStackGroup(builder);
         this.fluids = new RecipePresentation.IGuiFluidStackGroup(builder);
      }

      public RecipePresentation.IGuiItemStackGroup getItemStacks() {
         return this.items;
      }

      public RecipePresentation.IGuiFluidStackGroup getFluidStacks() {
         return this.fluids;
      }
   }

   public interface IRecipeWrapper {
      void getIngredients(RecipePresentation.IIngredients var1);

      default void drawInfo(GuiGraphicsExtractor graphics, int width, int height, int mouseX, int mouseY) {
      }
   }

   public interface IShapedCraftingRecipeWrapper extends RecipePresentation.IRecipeWrapper {
      int getWidth();

      int getHeight();
   }
}
