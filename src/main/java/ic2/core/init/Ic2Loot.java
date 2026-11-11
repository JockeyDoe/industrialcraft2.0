package ic2.core.init;

import ic2.core.IC2;
import ic2.core.util.LogCategory;

public final class Ic2Loot {
   private Ic2Loot() {
   }

   public static void init() {
      if (Ic2Loot.class.getResource("/data/ic2/loot_modifiers/chest_simple_dungeon.json") == null) {
         throw new IllegalStateException("IC2 chest loot modifiers are missing");
      } else {
         IC2.log.debug(LogCategory.General, "IC2 chest loot uses native global loot modifiers.");
      }
   }
}
