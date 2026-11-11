package ic2.core.recipe;

import ic2.core.init.MainConfig;
import ic2.core.init.Rezepte;
import ic2.core.util.StackUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class ColourCarryingRecipe extends AdvRecipe {
   public static void addAndRegister(ItemStack result, Object... args) {
      try {
         Rezepte.registerRecipe(new ColourCarryingRecipe(result, args));
      } catch (RuntimeException var3) {
         if (!MainConfig.ignoreInvalidRecipes) {
            throw var3;
         }
      }
   }

   public ColourCarryingRecipe(ItemStack result, Object... args) {
      super(result, args);
   }

   @Override
   public RecipeSerializer<? extends CraftingRecipe> getSerializer() {
      return RecipeCodecs.COLOUR;
   }

   @Override
   public ItemStack func_77572_b(CraftingContainer craftingInv) {
      ItemStack initialResult = super.func_77572_b(craftingInv);
      if (!StackUtil.isEmpty(initialResult) && ArmorDyeingRecipe.isArmor(initialResult)) {
         int colour = -1;

         for (int slot = 0; slot < craftingInv.getContainerSize(); slot++) {
            ItemStack offer = craftingInv.getItem(slot);
            if (!StackUtil.isEmpty(initialResult) && ArmorDyeingRecipe.isArmor(offer)) {
               colour = DyedItemColor.getOrDefault(offer, -1);
               break;
            }
         }

         if (colour != -1) {
            initialResult.set(DataComponents.DYED_COLOR, new DyedItemColor(colour & 16777215));
         }
      }

      return initialResult;
   }
}
