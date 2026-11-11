package ic2.api.info;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.fml.ModList;

public class Info {
   public static IInfoProvider itemInfo;
   public static Object ic2ModInstance;
   public static DamageSource DMG_ELECTRIC;
   public static DamageSource DMG_NUKE_EXPLOSION;
   public static DamageSource DMG_RADIATION;
   public static MobEffect POTION_RADIATION;
   public static String MOD_ID = "ic2";
   private static Boolean ic2Available = null;

   public static boolean isIc2Available() {
      if (ic2Available != null) {
         return ic2Available;
      } else {
         boolean loaded = ModList.get() != null && ModList.get().isLoaded(MOD_ID);
         if (ModList.get() != null) {
            ic2Available = loaded;
         }

         return loaded;
      }
   }
}
