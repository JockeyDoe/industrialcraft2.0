package ic2.api.recipe;

import net.minecraft.nbt.CompoundTag;

public interface IMachineRecipeManager<RI, RO, I> {
   boolean addRecipe(RI var1, RO var2, CompoundTag var3, boolean var4);

   MachineRecipeResult<RI, RO, I> apply(I var1, boolean var2);

   Iterable<? extends MachineRecipe<RI, RO>> getRecipes();

   boolean isIterable();
}
