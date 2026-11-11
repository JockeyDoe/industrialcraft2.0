package ic2.api.recipe;

import java.util.Collection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public interface IBasicMachineRecipeManager extends IMachineRecipeManager<IRecipeInput, Collection<ItemStack>, ItemStack> {
   boolean addRecipe(IRecipeInput var1, CompoundTag var2, boolean var3, ItemStack... var4);

   @Deprecated
   RecipeOutput getOutputFor(ItemStack var1, boolean var2);
}
