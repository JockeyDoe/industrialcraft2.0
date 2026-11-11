package ic2.core.init;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = "ic2")
public final class NativeRegistration {
   private static final List<NativeRegistration.Entry<?>> entries = new ArrayList<>();
   private static final Map<Object, Identifier> names = new IdentityHashMap<>();

   private NativeRegistration() {
   }

   public static <T, U extends T> U add(ResourceKey<? extends Registry<T>> registry, Identifier id, U value) {
      if (names.putIfAbsent(value, id) != null) {
         throw new IllegalArgumentException("IC2 object registered twice: " + id);
      } else {
         entries.add(new NativeRegistration.Entry<>(registry, id, value));
         return value;
      }
   }

   public static Identifier name(Object value) {
      return names.get(value);
   }

   @SubscribeEvent
   public static void register(RegisterEvent event) {
      for (NativeRegistration.Entry<?> entry : List.copyOf(entries)) {
         if (entry.registry.equals(event.getRegistryKey())) {
            entry.register(event);
         }
      }
   }

   private record Entry<T>(ResourceKey<? extends Registry<T>> registry, Identifier id, T value) {
      void register(RegisterEvent event) {
         event.register(this.registry, this.id, () -> this.value);
      }
   }
}
