package ic2.core.recipe;

import ic2.api.item.ICustomDamageItem;
import ic2.api.item.IElectricItemManager;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;

public final class CraftingAlgorithms {
   private CraftingAlgorithms() {
   }

   public static ItemStack shaped(
      CraftingContainer grid,
      int[] masks,
      int[] mirroredMasks,
      List<Predicate<ItemStack>> inputs,
      List<Predicate<ItemStack>> mirroredInputs,
      ItemStack output,
      IElectricItemManager electric
   ) {
      int mask = 0;

      for (int i = 0; i < grid.getContainerSize(); i++) {
         mask = mask << 1 | (grid.getItem(i).isEmpty() ? 0 : 1);
      }

      if (grid.getContainerSize() == 4) {
         mask = (mask & 12) << 5 | (mask & 3) << 4;
      }

      if (contains(masks, mask)) {
         ItemStack result = ordered(grid, inputs, output, electric);
         if (!result.isEmpty()) {
            return result;
         }
      }

      return mirroredMasks != null && contains(mirroredMasks, mask) ? ordered(grid, mirroredInputs, output, electric) : ItemStack.EMPTY;
   }

   private static boolean contains(int[] masks, int mask) {
      for (int candidate : masks) {
         if (candidate == mask) {
            return true;
         }
      }

      return false;
   }

   private static ItemStack ordered(CraftingContainer grid, List<Predicate<ItemStack>> inputs, ItemStack output, IElectricItemManager electric) {
      double charge = 0.0;
      int next = 0;

      for (ItemStack offered : grid.getItems()) {
         if (!offered.isEmpty()) {
            if (next >= inputs.size() || !inputs.get(next++).test(offered)) {
               return ItemStack.EMPTY;
            }

            charge += electric.getCharge(offered.copyWithCount(1));
         }
      }

      return next != inputs.size() ? ItemStack.EMPTY : chargedCopy(output, charge, electric);
   }

   public static ItemStack shapeless(CraftingContainer grid, List<Predicate<ItemStack>> inputs, ItemStack output, IElectricItemManager electric) {
      if (grid.getContainerSize() < inputs.size()) {
         return ItemStack.EMPTY;
      } else {
         List<Predicate<ItemStack>> unmatched = new ArrayList<>(inputs);
         double charge = 0.0;

         for (ItemStack offered : grid.getItems()) {
            if (!offered.isEmpty()) {
               int matching = -1;

               for (int j = 0; j < unmatched.size(); j++) {
                  if (unmatched.get(j).test(offered)) {
                     matching = j;
                     break;
                  }
               }

               if (matching < 0) {
                  return ItemStack.EMPTY;
               }

               unmatched.remove(matching);
               charge += electric.getCharge(offered.copyWithCount(1));
            }
         }

         return unmatched.isEmpty() ? chargedCopy(output, charge, electric) : ItemStack.EMPTY;
      }
   }

   private static ItemStack chargedCopy(ItemStack output, double charge, IElectricItemManager electric) {
      ItemStack copy = output.copy();
      electric.charge(copy, charge, Integer.MAX_VALUE, true, false);
      return copy;
   }

   public static ItemStack gradual(CraftingContainer grid, ICustomDamageItem item, Predicate<ItemStack> filler, int amount) {
      ItemStack target = null;
      int materials = 0;

      for (ItemStack offered : grid.getItems()) {
         if (!offered.isEmpty()) {
            if (target == null && offered.getItem() == item) {
               target = offered;
            } else {
               if (!filler.test(offered)) {
                  return ItemStack.EMPTY;
               }

               materials++;
            }
         }
      }

      if (target != null && materials != 0) {
         ItemStack result = target.copy();
         item.setCustomDamage(result, Math.max(0, Math.min(item.getMaxCustomDamage(result), item.getCustomDamage(result) - amount * materials)));
         return result;
      } else {
         return ItemStack.EMPTY;
      }
   }
}
