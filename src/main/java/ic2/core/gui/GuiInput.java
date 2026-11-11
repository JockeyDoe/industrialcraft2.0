package ic2.core.gui;

import com.mojang.blaze3d.platform.InputConstants;

public final class GuiInput {
   private GuiInput() {
   }

   public static boolean control() {
      return InputConstants.isKeyDown(224) || InputConstants.isKeyDown(228);
   }

   public static boolean shift() {
      return InputConstants.isKeyDown(225) || InputConstants.isKeyDown(229);
   }
}
