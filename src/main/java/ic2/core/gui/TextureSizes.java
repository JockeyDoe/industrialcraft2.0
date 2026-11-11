package ic2.core.gui;

public final class TextureSizes {
   private TextureSizes() {
   }

   public static int canvasSize(int size) {
      if (size >= 1 && size <= 16384) {
         return size == 1 ? 1 : Integer.highestOneBit(size - 1) << 1;
      } else {
         throw new IllegalArgumentException("Invalid texture size " + size);
      }
   }
}
