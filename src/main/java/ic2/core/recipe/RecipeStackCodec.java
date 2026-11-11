package ic2.core.recipe;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class RecipeStackCodec {
   public static final Codec<ItemStack> CODEC = new Codec<ItemStack>() {
      public <T> DataResult<Pair<ItemStack, T>> decode(DynamicOps<T> ops, T input) {
         return ItemStack.CODEC.decode(ops, input);
      }

      public <T> DataResult<T> encode(ItemStack stack, DynamicOps<T> ops, T prefix) {
         DataResult<T> encoded = ItemStack.CODEC.encode(stack, ops, prefix);
         CustomData custom = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
         return custom == null
            ? encoded
            : encoded.flatMap(
               value -> ops.getMap(value)
                  .flatMap(
                     root -> {
                        T components = (T)root.get("components");
                        return components == null
                           ? DataResult.success(value)
                           : ops.mergeToMap(
                                 ops.remove(components, "minecraft:custom_data"),
                                 ops.createString("minecraft:custom_data"),
                                 ops.createString(custom.copyTag().toString())
                              )
                              .flatMap(updated -> ops.mergeToMap(ops.remove(value, "components"), ops.createString("components"), updated));
                     }
                  )
            );
      }
   };

   private RecipeStackCodec() {
   }
}
