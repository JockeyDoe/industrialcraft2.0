package ic2.core.gui;

import net.minecraft.resources.Identifier;

public final class GuiTextures {
   private static final ThreadLocal<Identifier> selected = new ThreadLocal<>();

   private GuiTextures() {
   }

   public static void select(Identifier texture) {
      selected.set(texture);
   }

   public static Identifier current() {
      Identifier texture = selected.get();
      if (texture == null) {
         throw new IllegalStateException("No GUI texture selected");
      } else {
         return texture;
      }
   }

   public static void clear() {
      selected.remove();
   }
}
