package ic2.core.init;

import java.lang.reflect.Field;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class VanillaBlockPolicy {
   private static final Field resistance;

   private VanillaBlockPolicy() {
   }

   public static void resistance(Block block, float legacyResistance) {
      try {
         resistance.setFloat(block, legacyResistance * 3.0F);
      } catch (IllegalAccessException var3) {
         throw new IllegalStateException("Cannot configure block resistance", var3);
      }
   }

   static {
      try {
         resistance = BlockBehaviour.class.getDeclaredField("explosionResistance");
         resistance.setAccessible(true);
      } catch (ReflectiveOperationException var1) {
         throw new ExceptionInInitializerError(var1);
      }
   }
}
