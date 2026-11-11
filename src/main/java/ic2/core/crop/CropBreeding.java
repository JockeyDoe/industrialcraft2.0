package ic2.core.crop;

import ic2.api.crops.CropProperties;

public final class CropBreeding {
   private CropBreeding() {
   }

   public static int ratio(boolean same, CropProperties next, CropProperties previous, String[] nextAttributes, String[] previousAttributes) {
      if (same) {
         return 500;
      } else {
         int value = 0;
         int[] old = previous.getAllProperties();
         int[] fresh = next.getAllProperties();

         for (int i = 0; i < old.length; i++) {
            value += 2 - Math.abs(old[i] - fresh[i]);
         }

         for (String attribute : nextAttributes) {
            for (String other : previousAttributes) {
               if (attribute.equalsIgnoreCase(other)) {
                  value += 5;
               }
            }
         }

         int diff = next.getTier() - previous.getTier();
         if (diff > 1) {
            value -= 2 * diff;
         }

         if (diff < -3) {
            value += diff;
         }

         return Math.max(value, 0);
      }
   }

   public static int select(int[] cumulative, int roll) {
      if (cumulative.length != 0 && roll >= 0 && roll < cumulative[cumulative.length - 1]) {
         int low = 0;
         int high = cumulative.length - 1;

         while (low < high) {
            int middle = low + high >>> 1;
            if (roll < cumulative[middle]) {
               high = middle;
            } else {
               low = middle + 1;
            }
         }

         return low;
      } else {
         throw new IllegalArgumentException("Crop roll outside weighted range");
      }
   }
}
