package ic2.core.block.state;

public class UnlistedBooleanProperty extends UnlistedProperty<Boolean> {
   private final String name;

   public UnlistedBooleanProperty(String name) {
      super(name, Boolean.class);
      this.name = name;
   }

   @Override
   public String getName() {
      return this.name;
   }

   public boolean isValid(Boolean value) {
      return true;
   }

   @Override
   public Class<Boolean> getType() {
      return Boolean.class;
   }

   public String valueToString(Boolean value) {
      return value.toString();
   }

   @Override
   public String toString() {
      return this.getClass().getSimpleName() + "{name=" + this.name + "}";
   }
}
