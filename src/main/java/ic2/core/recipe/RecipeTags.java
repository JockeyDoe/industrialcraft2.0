package ic2.core.recipe;

import ic2.core.util.ItemStacks;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import net.minecraft.core.HolderSet.Named;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

@EventBusSubscriber(modid = "ic2")
public final class RecipeTags {
   public static final List<ItemStack> EMPTY_LIST = List.of();
   private static final Map<String, List<ItemStack>> declared = new LinkedHashMap<>();
   private static final List<String> names = new ArrayList<>();
   private static boolean nativeTagsAvailable;
   private static final List<Runnable> pending = new ArrayList<>();
   private static final Map<String, String> prefixes = Map.ofEntries(
      Map.entry("ingot", "ingots"),
      Map.entry("nugget", "nuggets"),
      Map.entry("ore", "ores"),
      Map.entry("dust", "dusts"),
      Map.entry("gem", "gems"),
      Map.entry("plate", "plates"),
      Map.entry("block", "storage_blocks"),
      Map.entry("stick", "rods"),
      Map.entry("gear", "gears"),
      Map.entry("crushed", "crushed_ores"),
      Map.entry("dye", "dyes")
   );

   public static void deferOre(String name, Supplier<ItemStack> stack) {
      pending.add(() -> registerOre(name, stack.get()));
   }

   public static void initializeDeclaredEntries() {
      for (Runnable entry : List.copyOf(pending)) {
         entry.run();
      }

      pending.clear();
   }

   public static TagKey<Item> tag(String name) {
      if (name.contains(":")) {
         return TagKey.create(Registries.ITEM, Identifier.parse(name));
      } else if (name.equals("logWood")) {
         return ItemTags.LOGS;
      } else if (name.equals("plankWood")) {
         return ItemTags.PLANKS;
      } else if (name.equals("treeLeaves")) {
         return ItemTags.LEAVES;
      } else if (name.equals("treeSapling")) {
         return ItemTags.SAPLINGS;
      } else if (name.equals("chestWood")) {
         return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "chests/wooden"));
      } else if (name.equals("stickWood")) {
         return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "rods/wooden"));
      } else {
         for (Entry<String, String> entry : prefixes.entrySet()) {
            if (name.startsWith(entry.getKey()) && name.length() > entry.getKey().length()) {
               String material = name.substring(entry.getKey().length()).replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
               return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", entry.getValue() + "/" + material));
            }
         }

         return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("ic2", "legacy/" + name.toLowerCase(Locale.ROOT)));
      }
   }

   private static synchronized int id(String name) {
      int id = names.indexOf(name);
      if (id < 0) {
         id = names.size();
         names.add(name);
      }

      return id;
   }

   public static void registerOre(String name, ItemLike item) {
      registerOre(name, new ItemStack(item));
   }

   public static void registerOre(String name, ItemStack stack) {
      if (stack.isEmpty()) {
         throw new IllegalArgumentException("Empty ingredient: " + name);
      } else {
         id(name);
         declared.computeIfAbsent(name, key -> new ArrayList<>()).add(stack.copy());
         NeoForge.EVENT_BUS.post(new RecipeTags.EntryAdded(name, stack.copy()));
      }
   }

   public static List<ItemStack> getOres(String name) {
      id(name);
      List<ItemStack> result = new ArrayList<>();

      for (ItemStack stack : declared.getOrDefault(name, List.of())) {
         result.add(stack.copy());
      }

      if (nativeTagsAvailable) {
         BuiltInRegistries.ITEM.get(tag(name)).ifPresent(holders -> holders.forEach(holder -> {
            if (result.stream().noneMatch(stackx -> stackx.is((Item)holder.value()))) {
               result.add(new ItemStack(holder));
            }
         }));
      }

      return result.isEmpty() ? EMPTY_LIST : Collections.unmodifiableList(result);
   }

   public static String[] getOreNames() {
      if (nativeTagsAvailable) {
         BuiltInRegistries.ITEM.getTags().<TagKey>map(Named::key).forEach(key -> {
            Identifier loc = key.location();
            String[] path = loc.getPath().split("/", 2);
            if (loc.getNamespace().equals("c") && path.length == 2) {
               prefixes.forEach((legacy, modern) -> {
                  if (path[0].equals(modern)) {
                     StringBuilder name = new StringBuilder(legacy);

                     for (String word : path[1].split("_")) {
                        if (!word.isEmpty()) {
                           name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
                        }
                     }

                     id(name.toString());
                  }
               });
            }
         });
      }

      return names.toArray(String[]::new);
   }

   public static String getOreName(int id) {
      return names.get(id);
   }

   public static int[] getOreIDs(ItemStack stack) {
      if (stack.isEmpty()) {
         return new int[0];
      } else {
         getOreNames();
         return IntStream.range(0, names.size())
            .filter(
               index -> {
                  String name = names.get(index);
                  return nativeTagsAvailable && stack.is(tag(name))
                     ? true
                     : declared.getOrDefault(name, List.of())
                        .stream()
                        .anyMatch(
                           candidate -> candidate.getItem() == stack.getItem()
                              && (ItemStacks.rawMeta(candidate) == 32767 || ItemStacks.rawMeta(candidate) == ItemStacks.rawMeta(stack))
                        );
               }
            )
            .toArray();
      }
   }

   @SubscribeEvent
   public static void tagsUpdated(TagsUpdatedEvent event) {
      nativeTagsAvailable = true;

      for (String name : getOreNames()) {
         for (ItemStack stack : getOres(name)) {
            NeoForge.EVENT_BUS.post(new RecipeTags.EntryAdded(name, stack));
         }
      }
   }

   public static final class EntryAdded extends Event {
      private final String name;
      private final ItemStack stack;

      public EntryAdded(String name, ItemStack stack) {
         this.name = name;
         this.stack = stack;
      }

      public String getName() {
         return this.name;
      }

      public ItemStack getOre() {
         return this.stack;
      }
   }
}
