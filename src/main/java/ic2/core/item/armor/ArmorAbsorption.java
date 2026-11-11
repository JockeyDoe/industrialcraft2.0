package ic2.core.item.armor;

public final class ArmorAbsorption {
   private ArmorAbsorption() {
   }

   public static double[] allocate(double damage, double[] ratios, double[] limits) {
      if (ratios.length != limits.length) {
         throw new IllegalArgumentException("Mismatched armor limits");
      } else {
         double[] absorbed = new double[ratios.length];
         double total = 0.0;

         for (double ratio : ratios) {
            total += Math.max(0.0, ratio);
         }

         if (damage <= 0.0 || total == 0.0) {
            return absorbed;
         } else if (total <= 1.0) {
            for (int i = 0; i < ratios.length; i++) {
               absorbed[i] = Math.min(damage * Math.max(0.0, ratios[i]), Math.max(0.0, limits[i]));
            }

            return absorbed;
         } else {
            boolean[] capped = new boolean[ratios.length];
            double remaining = damage;

            while (total > 0.0 && remaining > 0.0) {
               boolean changed = false;

               for (int i = 0; i < ratios.length; i++) {
                  if (!capped[i] && remaining * Math.max(0.0, ratios[i]) / total > Math.max(0.0, limits[i])) {
                     absorbed[i] = Math.max(0.0, limits[i]);
                     capped[i] = true;
                     changed = true;
                  }
               }

               if (!changed) {
                  for (int ix = 0; ix < ratios.length; ix++) {
                     if (!capped[ix]) {
                        absorbed[ix] = remaining * Math.max(0.0, ratios[ix]) / total;
                     }
                  }
                  break;
               }

               remaining = damage;
               total = 0.0;

               for (int ixx = 0; ixx < ratios.length; ixx++) {
                  if (capped[ixx]) {
                     remaining -= absorbed[ixx];
                  } else {
                     total += Math.max(0.0, ratios[ixx]);
                  }
               }
            }

            return absorbed;
         }
      }
   }
}
