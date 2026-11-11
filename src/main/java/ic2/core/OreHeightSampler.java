package ic2.core;

import java.util.Random;

public final class OreHeightSampler {
   private final int min;
   private final double[] cumulative;

   public OreHeightSampler(OreHeightSampler.Range range, int favoredMin, int favoredMaxExclusive) {
      if (!range.isEmpty() && favoredMaxExclusive > favoredMin) {
         this.min = range.min();
         int first = Math.clamp((long)favoredMin, this.min, range.maxExclusive() - 1);
         int last = Math.clamp((long)(favoredMaxExclusive - 1), first, range.maxExclusive() - 1);
         this.cumulative = new double[range.maxExclusive() - this.min];
         double total = 0.0;

         for (int i = 0; i < this.cumulative.length; i++) {
            int y = this.min + i;
            double weight = y < first
               ? (y - this.min + 1.0) / (first - this.min + 1.0)
               : (y > last ? (double)(range.maxExclusive() - y) / (range.maxExclusive() - last) : 1.0);
            this.cumulative[i] = total += weight;
         }
      } else {
         throw new IllegalArgumentException("Empty ore range or favored band");
      }
   }

   public int sample(Random random) {
      double target = random.nextDouble() * this.cumulative[this.cumulative.length - 1];
      int low = 0;
      int high = this.cumulative.length - 1;

      while (low < high) {
         int middle = low + high >>> 1;
         if (target < this.cumulative[middle]) {
            high = middle;
         } else {
            low = middle + 1;
         }
      }

      return this.min + low;
   }

   public record Range(int min, int maxExclusive) {
      public static OreHeightSampler.Range clipped(int min, int maxExclusive, int scale, int worldMin, int worldMaxExclusive) {
         if (maxExclusive > min && scale > 0 && worldMaxExclusive > worldMin) {
            int lower = Math.clamp((long)min * scale / 64L, worldMin, worldMaxExclusive);
            int upper = Math.clamp((long)maxExclusive * scale / 64L, worldMin, worldMaxExclusive);
            return new OreHeightSampler.Range(lower, upper);
         } else {
            throw new IllegalArgumentException("Invalid ore height range or world bounds");
         }
      }

      public boolean isEmpty() {
         return this.maxExclusive <= this.min;
      }
   }
}
