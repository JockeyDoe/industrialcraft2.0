package ic2.core.util;

import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.Recipes;
import ic2.core.IC2;
import ic2.core.recipe.RecipeInputMultiple;
import ic2.core.ref.IMultiBlock;
import ic2.core.ref.IMultiItem;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ConfigUtil {
   public static List<String> asList(String str) {
      str = str.trim();
      return str.isEmpty() ? Collections.emptyList() : Arrays.asList(str.split("\\s*,\\s*"));
   }

   public static List<IRecipeInput> asRecipeInputList(Config config, String key) {
      Config.Value value = config.get(key);

      try {
         try {
            return asRecipeInputList(value.getString());
         } catch (ParseException var4) {
            throw new Config.ParseException("Invalid value", value, var4);
         }
      } catch (Config.ParseException var5) {
         displayError(var5, key);
         return null;
      }
   }

   public static List<ItemStack> asStackList(Config config, String key) {
      Config.Value value = config.get(key);

      try {
         try {
            return asStackList(value.getString());
         } catch (ParseException var4) {
            throw new Config.ParseException("Invalid value", value, var4);
         }
      } catch (Config.ParseException var5) {
         displayError(var5, key);
         return null;
      }
   }

   public static ItemStack asStack(Config config, String key) {
      Config.Value value = config.get(key);

      try {
         try {
            return asStack(value.getString());
         } catch (ParseException var4) {
            throw new Config.ParseException("Invalid value", value, var4);
         }
      } catch (Config.ParseException var5) {
         displayError(var5, key);
         return null;
      }
   }

   public static String getString(Config config, String key) {
      return config.get(key).getString();
   }

   public static boolean getBool(Config config, String key) {
      Config.Value value = config.get(key);

      try {
         return value.getBool();
      } catch (Config.ParseException var4) {
         displayError(var4, key);
         return false;
      }
   }

   public static int getInt(Config config, String key) {
      Config.Value value = config.get(key);

      try {
         return value.getInt();
      } catch (Config.ParseException var4) {
         displayError(var4, key);
         return 0;
      }
   }

   public static float getFloat(Config config, String key) {
      Config.Value value = config.get(key);

      try {
         return value.getFloat();
      } catch (Config.ParseException var4) {
         displayError(var4, key);
         return 0.0F;
      }
   }

   public static double getDouble(Config config, String key) {
      Config.Value value = config.get(key);

      try {
         return value.getDouble();
      } catch (Config.ParseException var4) {
         displayError(var4, key);
         return 0.0;
      }
   }

   public static int[] asIntArray(Config config, String key) {
      Config.Value value = config.get(key);

      try {
         return asList(value.getString()).stream().mapToInt(Integer::parseInt).toArray();
      } catch (NumberFormatException var4) {
         displayError(new Config.ParseException("Invalid value", value, var4), key);
         return new int[0];
      }
   }

   public static List<ItemStack> asStackList(String str) throws ParseException {
      List<String> parts = asList(str);
      List<ItemStack> ret = new ArrayList<>(parts.size());

      for (String part : parts) {
         ret.add(asStack(part));
      }

      return ret;
   }

   public static List<IRecipeInput> asRecipeInputList(String str) throws ParseException {
      return asRecipeInputList(str, false);
   }

   public static List<IRecipeInput> asRecipeInputList(String str, boolean allowNull) throws ParseException {
      List<String> parts = asList(str);
      List<IRecipeInput> ret = new ArrayList<>(parts.size());

      for (String part : parts) {
         IRecipeInput input = asRecipeInput(part);
         if (input == null && !allowNull) {
            throw new ParseException("There is no item matching " + part + ".", -1);
         }

         ret.add(input);
      }

      return ret;
   }

   private static ItemStack asStack(String str, boolean checkAmount) throws ParseException {
      String[] parts = str.split("(?=(@|#|\\*))");
      String itemName = parts[0];
      int namespaceEnd = itemName.indexOf(58);
      if (namespaceEnd >= 0) {
         itemName = itemName.substring(0, namespaceEnd).toLowerCase(Locale.ROOT) + itemName.substring(namespaceEnd);
      }

      int metadata = 0;

      for (int i = 1; i < parts.length; i++) {
         if (parts[i].startsWith("@")) {
            metadata = parts[i].equals("@") && i + 1 < parts.length && parts[i + 1].equals("*") ? 32767 : Integer.parseInt(parts[i].substring(1));
         }
      }

      Item converted = VanillaVariants.legacyItem(itemName, metadata == 32767 ? 0 : metadata);
      Item item = converted == null ? (Item)BuiltInRegistries.ITEM.getOptional(Identifier.parse(itemName)).orElse(null) : converted;
      if (item == null) {
         return null;
      } else {
         ItemStack stack = new ItemStack(item);
         int amount = 1;

         for (int ix = 1; ix < parts.length; ix++) {
            String tmp = parts[ix];
            if (tmp.startsWith("@")) {
               if (ix + 1 < parts.length && parts[ix + 1].equals("*")) {
                  stack = ItemStacks.create(item, 1, 32767);
                  ix++;
               } else {
                  stack = converted == null ? ItemStacks.create(item, 1, Integer.parseInt(tmp.substring(1))) : new ItemStack(item);
               }
            } else if (tmp.startsWith("#")) {
               if (item instanceof IMultiItem) {
                  stack = ((IMultiItem)item).getItemStack(tmp.substring(1));
               } else {
                  if (!(item instanceof BlockItem) || !(((BlockItem)item).getBlock() instanceof IMultiBlock)) {
                     throw new ParseException("# is not supported on non-IC2-Items: " + str, 0);
                  }

                  stack = ((IMultiBlock)((BlockItem)item).getBlock()).getItemStack(tmp.substring(1));
               }
            } else if (tmp.startsWith("*")) {
               if (!checkAmount) {
                  throw new ParseException("We do not support amount here.", -1);
               }

               amount = Integer.parseInt(tmp.substring(1));
            }
         }

         if (checkAmount) {
            stack = StackUtil.setSize(stack, amount);
         }

         return stack;
      }
   }

   public static ItemStack asStack(String str) throws ParseException {
      return asStack(str, false);
   }

   public static ItemStack asStackWithAmount(String str) throws ParseException {
      return asStack(str, true);
   }

   public static String fromStack(ItemStack stack) {
      return fromStack(stack, false);
   }

   private static String fromStack(ItemStack stack, boolean amount) {
      String ret = Util.getName(stack.getItem()).toString();
      if (amount) {
         ret = ret + "*" + StackUtil.getSize(stack);
      }

      if (stack.getItem() instanceof IMultiItem) {
         String variant = ((IMultiItem)stack.getItem()).getVariant(stack);
         if (variant != null) {
            ret = ret + "#" + variant;
         }
      } else if (stack.getItem() instanceof BlockItem && ((BlockItem)stack.getItem()).getBlock() instanceof IMultiBlock) {
         String variant = ((IMultiBlock)((BlockItem)stack.getItem()).getBlock()).getVariant(stack);
         if (variant != null) {
            ret = ret + "#" + variant;
         }
      } else if (ItemStacks.rawMeta(stack) == 32767) {
         ret = ret + "@*";
      } else if (ItemStacks.rawMeta(stack) != 0) {
         ret = ret + "@" + ItemStacks.rawMeta(stack);
      }

      return ret;
   }

   public static String fromStackWithAmount(ItemStack stack) {
      return fromStack(stack, true);
   }

   public static IRecipeInput asRecipeInput(Config.Value value) {
      try {
         return asRecipeInput(value.getString());
      } catch (ParseException var2) {
         throw new Config.ParseException("Invalid value", value, var2);
      }
   }

   private static IRecipeInput asRecipeInput(String str, boolean checkAmount) throws ParseException {
      String[] parts = str.split("(?=(@|#|\\*))");
      String itemName = parts[0];
      if (!itemName.startsWith("OreDict:") && !itemName.startsWith("Fluid:")) {
         ItemStack stack = asStack(str, checkAmount);
         if (stack != null && str.contains("@*")) {
            List<Item> variants = VanillaVariants.legacyItems(itemName);
            if (!variants.isEmpty()) {
               return new RecipeInputMultiple(
                  stack.getCount(),
                  variants.stream().map(item -> Recipes.inputFactory.forStack(new ItemStack(item, stack.getCount()))).toArray(IRecipeInput[]::new)
               );
            }
         }

         return stack == null ? null : Recipes.inputFactory.forStack(stack);
      } else {
         Integer amount = null;
         Integer meta = null;

         for (int i = 1; i < parts.length; i++) {
            String tmp = parts[i];
            if (tmp.startsWith("@")) {
               if (i + 1 < parts.length && parts[i + 1].equals("*")) {
                  meta = 32767;
                  i++;
               } else {
                  meta = Integer.parseInt(tmp.substring(1));
               }
            } else if (tmp.startsWith("*")) {
               if (!checkAmount) {
                  throw new ParseException("We do not support amount here.", -1);
               }

               amount = Integer.parseInt(tmp.substring(1));
            }
         }

         if (itemName.startsWith("OreDict:")) {
            if (amount == null) {
               amount = 1;
            }

            return meta == null
               ? Recipes.inputFactory.forOreDict(itemName.substring("OreDict:".length()), amount)
               : Recipes.inputFactory.forOreDict(itemName.substring("OreDict:".length()), amount, meta);
         } else if (itemName.startsWith("Fluid:")) {
            if (amount == null) {
               amount = 1000;
            }

            return Recipes.inputFactory.forFluidContainer(FluidKeys.find(itemName.substring("Fluid:".length())), amount);
         } else {
            return null;
         }
      }
   }

   public static IRecipeInput asRecipeInput(String str) throws ParseException {
      return asRecipeInput(str, false);
   }

   public static IRecipeInput asRecipeInputWithAmount(String str) throws ParseException {
      return asRecipeInput(str, true);
   }

   private static void displayError(Config.ParseException e, String key) {
      IC2.platform
         .displayError(
            "The IC2 config file contains an invalid entry for %s.\n\n%s%s",
            key,
            e.getMessage(),
            e.getCause() != null ? "\n\n" + e.getCause().getMessage() : ""
         );
   }
}
