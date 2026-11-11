package ic2.core.item;

import ic2.core.item.armor.ArmorProtection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab.TabVisibility;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

@EventBusSubscriber(modid = "ic2")
public final class ItemDefaults {
   private static final Map<Item, Integer> stackSizes = new IdentityHashMap<>();
   private static final Map<Item, Integer> durability = new IdentityHashMap<>();
   private static final Map<Item, Boolean> repairDisabled = new IdentityHashMap<>();
   private static final Map<Item, Boolean> variants = new IdentityHashMap<>();
   private static final Set<Item> hidden = Collections.newSetFromMap(new IdentityHashMap<>());

   public static void hideFromCreative(Item item) {
      hidden.add(item);
   }

   public static boolean isHidden(Item item) {
      return hidden.contains(item);
   }

   @SubscribeEvent
   public static void creative(BuildCreativeModeTabContentsEvent event) {
      event.removeIf(stack -> hidden.contains(stack.getItem()), TabVisibility.PARENT_AND_SEARCH_TABS);
   }

   private ItemDefaults() {
   }

   public static Item stackSize(Item item, int size) {
      if (size >= 1 && size <= 99) {
         stackSizes.put(item, size);
         return item;
      } else {
         throw new IllegalArgumentException("Invalid stack size " + size);
      }
   }

   public static Item durability(Item item, int damage) {
      if (damage < 0) {
         throw new IllegalArgumentException("Negative durability");
      } else {
         durability.put(item, damage);
         return item;
      }
   }

   public static Item noRepair(Item item) {
      repairDisabled.put(item, true);
      return item;
   }

   public static Item variants(Item item, boolean enabled) {
      variants.put(item, enabled);
      return item;
   }

   public static boolean hasVariants(Item item) {
      Boolean configured = variants.get(item);
      if (configured != null) {
         return configured;
      } else {
         CustomData data = (CustomData)item.components().get(DataComponents.CUSTOM_DATA);
         return data != null && data.copyTag().contains("ic2:variant");
      }
   }

   @SubscribeEvent
   public static void initialize(ModifyDefaultComponentsEvent event) {
      for (Item item : BuiltInRegistries.ITEM) {
         if (item instanceof ArmorProtection) {
            event.modify(item, (builder, context, target) -> builder.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY));
         }
      }

      stackSizes.forEach((itemx, size) -> event.modify(itemx, (builder, context, target) -> builder.set(DataComponents.MAX_STACK_SIZE, size)));
      durability.forEach((itemx, damage) -> event.modify(itemx, (builder, context, target) -> {
         if (damage == 0) {
            builder.set(DataComponents.MAX_DAMAGE, null);
            builder.set(DataComponents.DAMAGE, null);
         } else {
            builder.set(DataComponents.MAX_STACK_SIZE, 1);
            builder.set(DataComponents.MAX_DAMAGE, damage);
            builder.set(DataComponents.DAMAGE, 0);
         }
      }));
      repairDisabled.keySet().forEach(itemx -> event.modify(itemx, (builder, context, target) -> builder.set(DataComponents.REPAIRABLE, null)));
      variants.forEach((itemx, enabled) -> event.modify(itemx, (builder, context, target) -> {
         if (enabled) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("ic2:variant", 0);
            builder.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
         }
      }));
   }
}
