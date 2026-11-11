package ic2.core.recipe;

import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.Recipes;
import ic2.core.item.armor.ItemArmorIC2;
import ic2.core.util.Ic2Color;
import ic2.core.util.ItemStacks;
import ic2.core.util.StackUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;

public class ArmorDyeingRecipe implements LegacyCraftingRecipe {
   private static final Map<IRecipeInput, int[]> stackToRGB = buildDyeMap();
   protected final IRecipeInput armour;
   private Identifier name;

   @Override
   public LegacyCraftingRecipe setRegistryName(Identifier name) {
      this.name = name;
      return this;
   }

   @Override
   public Identifier getRegistryName() {
      return this.name;
   }

   @Override
   public ItemStack func_77571_b() {
      return ItemStack.EMPTY;
   }

   public RecipeSerializer<? extends CraftingRecipe> getSerializer() {
      return RecipeCodecs.DYE;
   }

   private static Map<IRecipeInput, int[]> buildDyeMap() {
      Map<IRecipeInput, int[]> ret = new HashMap<>();

      for (Ic2Color colour : Ic2Color.values) {
         int dyeMap = colour.mcColor.getTextureDiffuseColor();
         ret.put(Recipes.inputFactory.forOreDict(colour.oreDictDyeName), new int[]{dyeMap >> 16 & 0xFF, dyeMap >> 8 & 0xFF, dyeMap & 0xFF});
      }

      return ret;
   }

   public ArmorDyeingRecipe(ItemStack armour) {
      this(Recipes.inputFactory.forStack(armour));
      if (StackUtil.isEmpty(armour) || !isArmor(armour)) {
         throw new IllegalArgumentException("Invalid input stack: " + StackUtil.toStringSafe(armour));
      }
   }

   public ArmorDyeingRecipe(Class<? extends Item> type) {
      this(new ArmorDyeingRecipe.RecipeInputClass(type));
      if (type == null || !Item.class.isAssignableFrom(type)) {
         throw new IllegalArgumentException("Invalid input class: " + type);
      }
   }

   public ArmorDyeingRecipe(IRecipeInput input) {
      this.armour = input;
   }

   public static boolean isArmor(ItemStack stack) {
      Equippable equipment = (Equippable)stack.get(DataComponents.EQUIPPABLE);
      return stack.getItem() instanceof ItemArmorIC2 || equipment != null && equipment.slot().isArmor();
   }

   public static boolean isDye(ItemStack stack) {
      for (IRecipeInput input : stackToRGB.keySet()) {
         if (input.matches(stack)) {
            return true;
         }
      }

      return false;
   }

   public static int[] getColourForStack(ItemStack stack) {
      for (Entry<IRecipeInput, int[]> entry : stackToRGB.entrySet()) {
         if (entry.getKey().matches(stack)) {
            return entry.getValue();
         }
      }

      return null;
   }

   @Override
   public boolean matches(CraftingContainer craftingInv, Level world) {
      ItemStack Qsuit = null;

      for (int slot = 0; slot < craftingInv.getContainerSize(); slot++) {
         ItemStack stack = craftingInv.getItem(slot);
         if (!StackUtil.isEmpty(stack)) {
            if (this.armour.matches(stack)) {
               if (Qsuit != null) {
                  return false;
               }

               Qsuit = stack;
            } else if (getColourForStack(stack) == null) {
               return false;
            }
         }
      }

      return Qsuit != null;
   }

   @Override
   public ItemStack func_77572_b(CraftingContainer craftingInv) {
      ItemStack armourStack = null;
      boolean foundArmor = false;
      int[] newRBG = new int[3];
      int totalColour = 0;
      int numberOfDyes = 0;

      for (int slot = 0; slot < craftingInv.getContainerSize(); slot++) {
         ItemStack stack = craftingInv.getItem(slot);
         if (!StackUtil.isEmpty(stack)) {
            if (this.armour.matches(stack)) {
               foundArmor = true;
               if (!StackUtil.isEmpty(armourStack)) {
                  return StackUtil.emptyStack;
               }

               armourStack = StackUtil.copyWithSize(stack, 1);
               if (stack.has(DataComponents.DYED_COLOR)) {
                  int oldColour = ((DyedItemColor)armourStack.get(DataComponents.DYED_COLOR)).rgb();
                  int r = oldColour >> 16 & 0xFF;
                  int g = oldColour >> 8 & 0xFF;
                  int b = oldColour & 0xFF;
                  totalColour += Math.max(r, Math.max(g, b));
                  newRBG[0] += r;
                  newRBG[1] += g;
                  newRBG[2] += b;
                  numberOfDyes++;
               }
            } else {
               int[] dyeRGB = getColourForStack(stack);
               if (dyeRGB == null) {
                  return StackUtil.emptyStack;
               }

               int r = dyeRGB[0];
               int g = dyeRGB[1];
               int b = dyeRGB[2];
               totalColour += Math.max(r, Math.max(g, b));
               newRBG[0] += r;
               newRBG[1] += g;
               newRBG[2] += b;
               numberOfDyes++;
            }
         }
      }

      if (foundArmor && numberOfDyes != 0) {
         if (armourStack.has(DataComponents.DYED_COLOR) && numberOfDyes == 1) {
            armourStack.remove(DataComponents.DYED_COLOR);
         } else {
            int averageRed = newRBG[0] / numberOfDyes;
            int averageGreen = newRBG[1] / numberOfDyes;
            int averageBlue = newRBG[2] / numberOfDyes;
            float gain = (float)totalColour / numberOfDyes;
            float averageMax = Math.max(averageRed, Math.max(averageGreen, averageBlue));
            averageRed = (int)(averageRed * gain / averageMax);
            averageGreen = (int)(averageGreen * gain / averageMax);
            averageBlue = (int)(averageBlue * gain / averageMax);
            int finalColour = (averageRed << 8) + averageGreen;
            finalColour = (finalColour << 8) + averageBlue;
            armourStack.set(DataComponents.DYED_COLOR, new DyedItemColor(finalColour));
         }

         return armourStack;
      } else {
         return StackUtil.emptyStack;
      }
   }

   public static class RecipeInputClass extends RecipeInputBase implements IRecipeInput {
      protected final Class<?> type;
      protected final int amount;

      public RecipeInputClass(Class<?> type) {
         this(type, 1);
      }

      public RecipeInputClass(Class<?> type, int amount) {
         this.type = type;
         this.amount = amount;
      }

      @Override
      public boolean matches(ItemStack subject) {
         return this.matches(subject.getItem());
      }

      protected boolean matches(Item item) {
         return this.type.isInstance(item);
      }

      @Override
      public int getAmount() {
         return this.amount;
      }

      @Override
      public List<ItemStack> getInputs() {
         List<ItemStack> ret = new ArrayList<>();

         for (Item item : BuiltInRegistries.ITEM) {
            if (this.matches(item)) {
               ret.add(ItemStacks.create(item, 1, 32767));
            }
         }

         return ret;
      }

      @Override
      public String toString() {
         return "RInputClass<" + this.type + ", " + this.amount + ">";
      }

      @Override
      public boolean equals(Object obj) {
         ArmorDyeingRecipe.RecipeInputClass other;
         return obj != null
            && this.getClass() == obj.getClass()
            && (other = (ArmorDyeingRecipe.RecipeInputClass)obj).type == this.type
            && other.amount == this.amount;
      }
   }
}
