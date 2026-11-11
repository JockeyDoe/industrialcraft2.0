package ic2.jeiIntegration.transferhandlers;

import ic2.core.IC2;
import ic2.core.block.machine.container.ContainerBatchCrafter;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

public class TransferHandlerBatchCrafter implements IRecipeTransferHandler<ContainerBatchCrafter, RecipeHolder<CraftingRecipe>> {
   public Class<ContainerBatchCrafter> getContainerClass() {
      return ContainerBatchCrafter.class;
   }

   public Optional<MenuType<ContainerBatchCrafter>> getMenuType() {
      return Optional.empty();
   }

   public IRecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
      return RecipeTypes.CRAFTING;
   }

   public IRecipeTransferError transferRecipe(
      ContainerBatchCrafter container, RecipeHolder<CraftingRecipe> recipe, IRecipeSlotsView slots, Player player, boolean max, boolean execute
   ) {
      if (execute) {
         List<IRecipeSlotView> inputs = slots.getSlotViews(RecipeIngredientRole.INPUT);

         for (int i = 0; i < 9; i++) {
            container.base.craftingGrid[i] = i < inputs.size()
               ? inputs.get(i).getDisplayedItemStack().<ItemStack>map(ItemStack::copy).orElse(ItemStack.EMPTY)
               : ItemStack.EMPTY;
         }

         IC2.network.get(false).updateTileEntityField(container.base, "craftingGrid");
         IC2.network.get(false).initiateClientTileEntityEvent(container.base, 0);
      }

      return null;
   }
}
