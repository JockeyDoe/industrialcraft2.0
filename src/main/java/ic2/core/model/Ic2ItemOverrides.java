package ic2.core.model;

import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class Ic2ItemOverrides {
   public static final Ic2ItemOverrides EMPTY = new Ic2ItemOverrides(List.of());

   public Ic2ItemOverrides(List<?> entries) {
      if (!entries.isEmpty()) {
         throw new IllegalArgumentException("Use native item model predicates for JSON overrides");
      }
   }

   public Ic2Geometry handleItemState(Ic2Geometry original, ItemStack stack, Level world, LivingEntity entity) {
      return original;
   }
}
