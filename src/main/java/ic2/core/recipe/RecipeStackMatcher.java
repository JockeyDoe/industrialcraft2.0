package ic2.core.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ic2.core.util.ItemData;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class RecipeStackMatcher {
   private RecipeStackMatcher() {
   }

   public static int meta(ItemStack stack) {
      CompoundTag data = ItemData.read(stack);
      return data != null && data.contains("ic2:variant") ? data.getIntOr("ic2:variant", 0) : stack.getDamageValue();
   }

   public static boolean matches(ItemStack offered, ItemStack required, boolean exact) {
      if (offered != null && !offered.isEmpty() && offered.getItem() == required.getItem()) {
         int wanted = meta(required);
         if (!exact && wanted == 32767) {
            return true;
         } else if (meta(offered) != wanted) {
            return false;
         } else {
            CompoundTag subject = ItemData.read(offered);
            CompoundTag target = ItemData.read(required);
            if (exact) {
               return Objects.equals(subject, target);
            } else if (target == null) {
               return true;
            } else if (subject == null) {
               return target.isEmpty();
            } else {
               for (String key : target.keySet()) {
                  if (!Objects.equals(target.get(key), subject.get(key))) {
                     return false;
                  }
               }

               return true;
            }
         }
      } else {
         return false;
      }
   }

   public record Definition(ItemStack stack, int amount) {
      public static final Codec<RecipeStackMatcher.Definition> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               RecipeStackCodec.CODEC.fieldOf("stack").forGetter(RecipeStackMatcher.Definition::stack),
               Codec.intRange(1, Integer.MAX_VALUE).fieldOf("amount").forGetter(RecipeStackMatcher.Definition::amount)
            )
            .apply(i, RecipeStackMatcher.Definition::new)
      );
   }
}
