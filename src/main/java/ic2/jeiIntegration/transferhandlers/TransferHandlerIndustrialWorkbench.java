package ic2.jeiIntegration.transferhandlers;

import ic2.core.block.machine.container.ContainerIndustrialWorkbench;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

public class TransferHandlerIndustrialWorkbench implements IRecipeTransferHandler<ContainerIndustrialWorkbench, RecipeHolder<CraftingRecipe>> {
   private final IRecipeTransferHandlerHelper helper;

   public TransferHandlerIndustrialWorkbench(IRecipeTransferHandlerHelper helper) {
      this.helper = helper;
   }

   public Class<ContainerIndustrialWorkbench> getContainerClass() {
      return ContainerIndustrialWorkbench.class;
   }

   public Optional<MenuType<ContainerIndustrialWorkbench>> getMenuType() {
      return Optional.empty();
   }

   public IRecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
      return RecipeTypes.CRAFTING;
   }

   public IRecipeTransferError transferRecipe(
      ContainerIndustrialWorkbench container, RecipeHolder<CraftingRecipe> recipe, IRecipeSlotsView view, Player player, boolean max, boolean execute
   ) {
      List<IRecipeSlotView> inputs = view.getSlotViews(RecipeIngredientRole.INPUT).stream().filter(slot -> !slot.isEmpty()).toList();
      List<Slot> destination = new ArrayList<>();
      if (inputs.size() == 2) {
         for (int output : new int[]{container.indexOutputHammer, container.indexOutputCutter}) {
            Slot tool = container.getSlot(output - 2);
            Slot material = container.getSlot(output - 1);

            for (int index = 0; index < 2; index++) {
               ItemStack stack = inputs.get(index).getDisplayedItemStack().orElse(ItemStack.EMPTY);
               if (!stack.isEmpty() && tool.mayPlace(stack)) {
                  destination.add(index == 0 ? tool : material);
                  destination.add(index == 0 ? material : tool);
                  break;
               }
            }

            if (!destination.isEmpty()) {
               break;
            }
         }
      }

      if (destination.isEmpty()) {
         for (int i = container.indexGridStart; i < container.indexGridEnd; i++) {
            destination.add(container.getSlot(i));
         }
      }

      final List<Slot> targets = List.copyOf(destination);
      var info = new IRecipeTransferInfo<ContainerIndustrialWorkbench, RecipeHolder<CraftingRecipe>>() {
         {
            Objects.requireNonNull(TransferHandlerIndustrialWorkbench.this);
         }

         public Class<ContainerIndustrialWorkbench> getContainerClass() {
            return ContainerIndustrialWorkbench.class;
         }

         public Optional<MenuType<ContainerIndustrialWorkbench>> getMenuType() {
            return Optional.empty();
         }

         public IRecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
            return RecipeTypes.CRAFTING;
         }

         public boolean canHandle(ContainerIndustrialWorkbench menu, RecipeHolder<CraftingRecipe> recipex) {
            return true;
         }

         public List<Slot> getRecipeSlots(ContainerIndustrialWorkbench menu, RecipeHolder<CraftingRecipe> recipex) {
            return targets;
         }

         public List<Slot> getInventorySlots(ContainerIndustrialWorkbench menu, RecipeHolder<CraftingRecipe> recipex) {
            List<Slot> slots = new ArrayList<>();

            for (int i = 0; i < 36; i++) {
               slots.add(menu.getSlot(i));
            }

            for (int i = menu.indexBufferStart; i < menu.indexBufferEnd; i++) {
               slots.add(menu.getSlot(i));
            }

            return slots;
         }
      };
      return this.helper.createUnregisteredRecipeTransferHandler(info).transferRecipe(container, recipe, view, player, max, execute);
   }
}
