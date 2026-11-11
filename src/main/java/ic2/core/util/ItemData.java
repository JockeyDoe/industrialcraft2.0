package ic2.core.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class ItemData {
   private static final ReferenceQueue<CompoundTag> queue = new ReferenceQueue<>();
   private static final Map<ItemData.TagReference, CompoundTag> snapshots = new HashMap<>();

   private static synchronized CompoundTag snapshot(CompoundTag tag) {
      Object ref;
      while ((ref = queue.poll()) != null) {
         snapshots.remove(ref);
      }

      snapshots.put(new ItemData.TagReference(tag), tag.copy());
      return tag;
   }

   private ItemData() {
   }

   public static boolean has(ItemStack stack) {
      return stack.has(DataComponents.CUSTOM_DATA);
   }

   public static CompoundTag read(ItemStack stack) {
      CustomData data = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
      return data == null ? null : snapshot(data.copyTag());
   }

   public static CompoundTag readOrEmpty(ItemStack stack) {
      return snapshot(((CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag());
   }

   public static void store(ItemStack stack, CompoundTag data) {
      if (data != null && !data.isEmpty()) {
         CustomData.set(DataComponents.CUSTOM_DATA, stack, data);
      } else {
         stack.remove(DataComponents.CUSTOM_DATA);
      }

      if (data != null) {
         snapshot(data);
      }
   }

   public static synchronized void commit(ItemStack stack, CompoundTag data) {
      CompoundTag before = snapshots.get(new ItemData.TagReference(data));
      if (before == null) {
         store(stack, data);
      } else {
         update(stack, current -> applyChanges(current, before, data));
         snapshot(data);
      }
   }

   public static void applyChanges(CompoundTag current, CompoundTag before, CompoundTag changed) {
      for (String key : before.keySet()) {
         if (!changed.contains(key)) {
            current.remove(key);
         }
      }

      for (String keyx : changed.keySet()) {
         if (!Objects.equals(before.get(keyx), changed.get(keyx))) {
            current.put(keyx, changed.get(keyx).copy());
         }
      }
   }

   public static void update(ItemStack stack, Consumer<CompoundTag> change) {
      CustomData.update(DataComponents.CUSTOM_DATA, stack, change);
   }

   public static void put(ItemStack stack, String key, Tag value) {
      update(stack, data -> data.put(key, value.copy()));
   }

   public static void remove(ItemStack stack, String key) {
      update(stack, data -> data.remove(key));
   }

   private static final class TagReference extends WeakReference<CompoundTag> {
      private final int hash;

      TagReference(CompoundTag tag) {
         super(tag, ItemData.queue);
         this.hash = System.identityHashCode(tag);
      }

      @Override
      public int hashCode() {
         return this.hash;
      }

      @Override
      public boolean equals(Object other) {
         return this == other || other instanceof ItemData.TagReference ref && this.get() != null && this.get() == ref.get();
      }
   }
}
