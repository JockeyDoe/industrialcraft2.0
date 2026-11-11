package ic2.core.block.state;

import java.util.List;
import java.util.Optional;
import net.minecraft.world.level.block.state.properties.Property;

public class SkippedBooleanProperty extends Property<Boolean> implements ISkippableProperty {
   public SkippedBooleanProperty(String name) {
      super(name, Boolean.class);
   }

   public List<Boolean> getPossibleValues() {
      return List.of(true, false);
   }

   public String getName(Boolean value) {
      return value.toString();
   }

   public Optional<Boolean> getValue(String value) {
      return switch (value) {
         case "true" -> Optional.of(true);
         case "false" -> Optional.of(false);
         default -> Optional.empty();
      };
   }

   public int getInternalIndex(Boolean value) {
      return value ? 0 : 1;
   }
}
