package ic2.core.model;

import net.minecraft.resources.Identifier;

public record ModelKey(Identifier location, String variant) {
   public ModelKey(Identifier location, String variant) {
      if (location == null) {
         throw new NullPointerException("model location");
      } else {
         variant = variant == null ? "" : variant;
         this.location = location;
         this.variant = variant;
      }
   }

   public ModelKey(String location, String variant) {
      this(Identifier.parse(location), variant);
   }

   public String getNamespace() {
      return this.location.getNamespace();
   }

   public String getPath() {
      return this.location.getPath();
   }

   public String getVariant() {
      return this.variant;
   }

   @Override
   public String toString() {
      return this.location + (this.variant.isEmpty() ? "" : "#" + this.variant);
   }
}
