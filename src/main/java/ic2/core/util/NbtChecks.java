package ic2.core.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;

public final class NbtChecks {
   private NbtChecks() {
   }

   public static boolean hasType(CompoundTag compound, String key, int type) {
      Tag value = compound.get(key);
      return value != null && (type == 99 ? value instanceof NumericTag : value.getId() == type);
   }

   public static ListTag getList(CompoundTag compound, String key, int elementType) {
      ListTag list = compound.getListOrEmpty(key);

      for (Tag element : list) {
         if (element.getId() != elementType) {
            return new ListTag();
         }
      }

      return list;
   }
}
