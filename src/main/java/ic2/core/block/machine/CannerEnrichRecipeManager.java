package ic2.core.block.machine;

import ic2.api.recipe.ICannerEnrichRecipeManager;
import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.MachineRecipe;
import ic2.api.recipe.MachineRecipeResult;
import ic2.api.recipe.RecipeOutput;
import ic2.core.util.ItemStacks;
import ic2.core.util.LiquidUtil;
import ic2.core.util.StackUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public class CannerEnrichRecipeManager implements ICannerEnrichRecipeManager {
   private final List<MachineRecipe<ICannerEnrichRecipeManager.Input, FluidStack>> recipes = new ArrayList<>();

   public boolean addRecipe(ICannerEnrichRecipeManager.Input input, FluidStack output, CompoundTag metadata, boolean replace) {
      if (input.fluid == null) {
         throw new NullPointerException("The fluid recipe input is null.");
      } else if (input.additive == null) {
         throw new NullPointerException("The additive recipe input is null.");
      } else if (output == null) {
         throw new NullPointerException("The recipe output is null.");
      } else if (!LiquidUtil.check(input.fluid)) {
         throw new IllegalArgumentException("The fluid recipe input is invalid.");
      } else if (!LiquidUtil.check(output)) {
         throw new IllegalArgumentException("The fluid recipe output is invalid.");
      } else {
         for (ItemStack stack : input.additive.getInputs()) {
            MachineRecipe<ICannerEnrichRecipeManager.Input, FluidStack> recipe = this.getRecipe(input.fluid, stack, true);
            if (recipe != null) {
               if (!replace) {
                  return false;
               }

               this.recipes.remove(recipe);
            }
         }

         this.recipes.add(new MachineRecipe<>(input, output));
         return true;
      }
   }

   @Override
   public void addRecipe(FluidStack fluid, IRecipeInput additive, FluidStack output) {
      if (!this.addRecipe(new ICannerEnrichRecipeManager.Input(fluid, additive), output, null, false)) {
         throw new RuntimeException("ambiguous recipe: [" + fluid + "+" + additive.getInputs() + " -> " + output + "]");
      }
   }

   public MachineRecipeResult<ICannerEnrichRecipeManager.Input, FluidStack, ICannerEnrichRecipeManager.RawInput> apply(
      ICannerEnrichRecipeManager.RawInput input, boolean acceptTest
   ) {
      MachineRecipe<ICannerEnrichRecipeManager.Input, FluidStack> recipe = this.getRecipe(input.fluid, input.additive, acceptTest);
      if (recipe == null) {
         return null;
      } else {
         FluidStack remainingFluid;
         if (input.fluid == null) {
            remainingFluid = null;
         } else {
            remainingFluid = input.fluid.copy();
            remainingFluid.setAmount(remainingFluid.getAmount() - recipe.getInput().fluid.getAmount());
            if (remainingFluid.getAmount() <= 0) {
               remainingFluid = null;
            }
         }

         return recipe.getResult(
            new ICannerEnrichRecipeManager.RawInput(remainingFluid, StackUtil.copyShrunk(input.additive, recipe.getInput().additive.getAmount()))
         );
      }
   }

   private MachineRecipe<ICannerEnrichRecipeManager.Input, FluidStack> getRecipe(FluidStack fluid, ItemStack additive, boolean acceptTest) {
      if (acceptTest || fluid != null && !StackUtil.isEmpty(additive)) {
         for (MachineRecipe<ICannerEnrichRecipeManager.Input, FluidStack> recipe : this.recipes) {
            if ((
                  fluid == null
                     || FluidStack.isSameFluidSameComponents(fluid, recipe.getInput().fluid)
                        && (acceptTest || recipe.getInput().fluid.getAmount() <= fluid.getAmount())
               )
               && (
                  additive == null
                     || recipe.getInput().additive.matches(additive) && (acceptTest || recipe.getInput().additive.getAmount() <= StackUtil.getSize(additive))
               )) {
               return recipe;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   @Override
   public RecipeOutput getOutputFor(FluidStack fluid, ItemStack additive, boolean adjustInput, boolean acceptTest) {
      MachineRecipeResult<ICannerEnrichRecipeManager.Input, FluidStack, ICannerEnrichRecipeManager.RawInput> result = this.apply(
         new ICannerEnrichRecipeManager.RawInput(fluid, additive), acceptTest
      );
      if (result == null) {
         return null;
      } else {
         if (adjustInput) {
            fluid.setAmount(result.getAdjustedInput().fluid == null ? 0 : result.getAdjustedInput().fluid.getAmount());
            additive.setCount(StackUtil.isEmpty(result.getAdjustedInput().additive) ? 0 : StackUtil.getSize(result.getAdjustedInput().additive));
         }

         CompoundTag output = new CompoundTag();
         output.merge(
            (CompoundTag)FluidStack.OPTIONAL_CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, ItemStacks.registries()), result.getOutput()).getOrThrow()
         );
         return new RecipeOutput(output);
      }
   }

   @Override
   public Iterable<? extends MachineRecipe<ICannerEnrichRecipeManager.Input, FluidStack>> getRecipes() {
      return this.recipes;
   }

   @Override
   public boolean isIterable() {
      return true;
   }
}
