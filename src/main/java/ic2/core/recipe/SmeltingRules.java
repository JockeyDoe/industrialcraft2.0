package ic2.core.recipe;

import ic2.api.recipe.MachineRecipe;
import ic2.api.recipe.MachineRecipeResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.AbstractCookingRecipe.CookingBookInfo;
import net.minecraft.world.item.crafting.Recipe.CommonInfo;

public final class SmeltingRules {
   private SmeltingRules() {
   }

   public static SmeltingRecipe create(Ingredient input, ItemStack output, float experience) {
      if (!output.isEmpty() && !(experience < 0.0F)) {
         return new SmeltingRecipe(
            new CommonInfo(true), new CookingBookInfo(CookingBookCategory.MISC, ""), input, ItemStackTemplate.fromNonEmptyStack(output.copy()), experience, 200
         );
      } else {
         throw new IllegalArgumentException("Invalid smelting result or experience");
      }
   }

   public static MachineRecipeResult<ItemStack, ItemStack, ItemStack> apply(SmeltingRecipe recipe, ItemStack input) {
      if (!input.isEmpty() && recipe.input().test(input)) {
         ItemStack output = recipe.assemble(new SingleRecipeInput(input));
         if (output.isEmpty()) {
            return null;
         } else {
            CompoundTag metadata = new CompoundTag();
            metadata.putFloat("experience", recipe.experience() * output.getCount());
            return new MachineRecipe<>(input, output, metadata).getResult(input.copyWithCount(input.getCount() - 1));
         }
      } else {
         return null;
      }
   }
}
