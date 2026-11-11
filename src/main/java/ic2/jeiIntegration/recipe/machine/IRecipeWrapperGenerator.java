package ic2.jeiIntegration.recipe.machine;

import ic2.api.item.IBlockCuttingBlade;
import ic2.api.recipe.IBasicMachineRecipeManager;
import ic2.api.recipe.ICannerBottleRecipeManager;
import ic2.api.recipe.ICannerEnrichRecipeManager;
import ic2.api.recipe.IElectrolyzerRecipeManager;
import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.MachineRecipe;
import ic2.core.item.type.BlockCuttingBladeType;
import ic2.core.item.type.CraftingItemType;
import ic2.core.ref.BlockName;
import ic2.core.ref.ItemName;
import ic2.core.ref.TeBlock;
import ic2.core.util.FluidKeys;
import ic2.core.util.StackUtil;
import ic2.jeiIntegration.RecipePresentation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Map.Entry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

public interface IRecipeWrapperGenerator<T> {
   IRecipeWrapperGenerator<IBasicMachineRecipeManager> basicMachine = new IRecipeWrapperGenerator<IBasicMachineRecipeManager>() {
      @Override
      public List<RecipePresentation.IRecipeWrapper> getRecipeList(IORecipeCategory<IBasicMachineRecipeManager> category) {
         List<RecipePresentation.IRecipeWrapper> recipes = new ArrayList<>();

         for (MachineRecipe<IRecipeInput, Collection<ItemStack>> container : category.recipeManager.getRecipes()) {
            recipes.add(new IORecipeWrapper(container, category));
         }

         return recipes;
      }
   };
   IRecipeWrapperGenerator<IBasicMachineRecipeManager> recycler = new IRecipeWrapperGenerator<IBasicMachineRecipeManager>() {
      @Override
      public List<RecipePresentation.IRecipeWrapper> getRecipeList(IORecipeCategory<IBasicMachineRecipeManager> category) {
         IRecipeInput input = new IRecipeInput() {
            {
               Objects.requireNonNull(<VAR_NAMELESS_ENCLOSURE>);
            }

            @Override
            public boolean matches(ItemStack subject) {
               return StackUtil.checkItemEquality(subject, BlockName.te.getItemStack(TeBlock.recycler));
            }

            @Override
            public List<ItemStack> getInputs() {
               return Collections.singletonList(BlockName.te.getItemStack(TeBlock.recycler));
            }

            @Override
            public int getAmount() {
               return 1;
            }
         };
         return Collections.singletonList(
            new IORecipeWrapper(new MachineRecipe<>(input, Collections.singletonList(ItemName.crafting.getItemStack(CraftingItemType.scrap))), category)
         );
      }
   };
   IRecipeWrapperGenerator<IBasicMachineRecipeManager> blockCutter = new IRecipeWrapperGenerator<IBasicMachineRecipeManager>() {
      private final List<ItemStack> candidates = Arrays.asList(
         ItemName.block_cutting_blade.getItemStack(BlockCuttingBladeType.iron),
         ItemName.block_cutting_blade.getItemStack(BlockCuttingBladeType.steel),
         ItemName.block_cutting_blade.getItemStack(BlockCuttingBladeType.diamond)
      );

      @Override
      public List<RecipePresentation.IRecipeWrapper> getRecipeList(IORecipeCategory<IBasicMachineRecipeManager> category) {
         List<RecipePresentation.IRecipeWrapper> list = new ArrayList<>();

         for (MachineRecipe<IRecipeInput, Collection<ItemStack>> container : category.recipeManager.getRecipes()) {
            list.add(new AdvancedIORecipeWrapper(container, this.getInput(this.getHardness(container.getMetaData())), category));
         }

         return list;
      }

      private int getHardness(CompoundTag metadata) {
         return metadata == null ? Integer.MAX_VALUE : metadata.getIntOr("hardness", 0);
      }

      private IRecipeInput getInput(final int hardness) {
         return new IRecipeInput() {
            {
               Objects.requireNonNull(<VAR_NAMELESS_ENCLOSURE>);
            }

            @Override
            public boolean matches(ItemStack subject) {
               return subject != null
                  && subject.getItem() instanceof IBlockCuttingBlade
                  && ((IBlockCuttingBlade)subject.getItem()).getHardness(subject) > hardness;
            }

            @Override
            public List<ItemStack> getInputs() {
               List<ItemStack> list = new ArrayList<>(candidates.size());

               for (ItemStack stack : candidates) {
                  if (((IBlockCuttingBlade)stack.getItem()).getHardness(stack) >= hardness) {
                     list.add(stack);
                  }
               }

               return list;
            }

            @Override
            public int getAmount() {
               return 1;
            }
         };
      }
   };
   IRecipeWrapperGenerator<IElectrolyzerRecipeManager> electrolyzer = new IRecipeWrapperGenerator<IElectrolyzerRecipeManager>() {
      @Override
      public List<RecipePresentation.IRecipeWrapper> getRecipeList(IORecipeCategory<IElectrolyzerRecipeManager> category) {
         List<RecipePresentation.IRecipeWrapper> recipes = new ArrayList<>();

         for (Entry<String, IElectrolyzerRecipeManager.ElectrolyzerRecipe> recipe : category.recipeManager.getRecipeMap().entrySet()) {
            Fluid input = FluidKeys.find(recipe.getKey());
            if (input != null) {
               recipes.add(new ElectrolyzerWrapper(new FluidStack(input, recipe.getValue().inputAmount), recipe.getValue().outputs, category));
            }
         }

         return recipes;
      }
   };
   IRecipeWrapperGenerator<ICannerEnrichRecipeManager> cannerEnrichment = new IRecipeWrapperGenerator<ICannerEnrichRecipeManager>() {
      @Override
      public List<RecipePresentation.IRecipeWrapper> getRecipeList(IORecipeCategory<ICannerEnrichRecipeManager> category) {
         List<RecipePresentation.IRecipeWrapper> recipes = new ArrayList<>();

         for (MachineRecipe<ICannerEnrichRecipeManager.Input, FluidStack> recipe : category.recipeManager.getRecipes()) {
            recipes.add(new CannerEnrichmentWrapper(recipe.getInput(), recipe.getOutput(), category));
         }

         return recipes;
      }
   };
   IRecipeWrapperGenerator<ICannerBottleRecipeManager> cannerBottling = new IRecipeWrapperGenerator<ICannerBottleRecipeManager>() {
      @Override
      public List<RecipePresentation.IRecipeWrapper> getRecipeList(IORecipeCategory<ICannerBottleRecipeManager> category) {
         List<RecipePresentation.IRecipeWrapper> recipes = new ArrayList<>();

         for (MachineRecipe<ICannerBottleRecipeManager.Input, ItemStack> recipe : category.recipeManager.getRecipes()) {
            recipes.add(new CannerCanningWrapper(recipe.getInput(), recipe.getOutput(), category));
         }

         return recipes;
      }
   };

   List<RecipePresentation.IRecipeWrapper> getRecipeList(IORecipeCategory<T> var1);
}
