package ic2.core.block.state;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.Property.Value;

public class Ic2BlockState extends StateDefinition<Block, BlockState> {
   public Ic2BlockState(Block block, Property<?>... properties) {
      super(Block::defaultBlockState, block, Ic2BlockState.Ic2BlockStateInstance::new, properties(properties));
   }

   private static Map<String, Property<?>> properties(Property<?>[] properties) {
      Map<String, Property<?>> result = new TreeMap<>();

      for (Property<?> property : properties) {
         if (result.put(property.getName(), property) != null) {
            throw new IllegalArgumentException("Duplicate property " + property.getName());
         }
      }

      return result;
   }

   public static Ic2BlockState.Ic2BlockStateInstance wrap(BlockState state) {
      return state instanceof Ic2BlockState.Ic2BlockStateInstance extended ? extended : new Ic2BlockState.Ic2BlockStateInstance(state, Collections.emptyMap());
   }

   public static class Ic2BlockStateInstance extends BlockState {
      private final BlockState canonical;
      private final Property<?>[] keys;
      private final Comparable<?>[] values;
      private final Map<UnlistedProperty<?>, Object> extraProperties;

      private Ic2BlockStateInstance(Block block, Property<?>[] keys, Comparable<?>[] values) {
         super(block, keys, values);
         this.canonical = this;
         this.keys = keys;
         this.values = values;
         this.extraProperties = Collections.emptyMap();
      }

      private Ic2BlockStateInstance(BlockState state, Map<UnlistedProperty<?>, Object> extras) {
         super(state.getBlock(), state.getProperties().toArray(Property[]::new), state.getValues().map(Value::value).toArray(Comparable[]::new));
         this.canonical = state instanceof Ic2BlockState.Ic2BlockStateInstance extended ? extended.canonical : state;
         this.keys = state.getProperties().toArray(Property[]::new);
         this.values = state.getValues().map(Value::value).toArray(Comparable[]::new);
         this.extraProperties = Collections.unmodifiableMap(new IdentityHashMap<>(extras));
      }

      public <T extends Comparable<T>, V extends T> Ic2BlockState.Ic2BlockStateInstance setValue(Property<T> property, V value) {
         BlockState next = this.canonical == this ? (BlockState)super.setValue(property, value) : (BlockState)this.canonical.setValue(property, value);
         return next == this.canonical ? this : new Ic2BlockState.Ic2BlockStateInstance(next, this.extraProperties);
      }

      public <T extends Comparable<T>, V extends T> Ic2BlockState.Ic2BlockStateInstance withProperty(Property<T> property, V value) {
         return this.setValue(property, value);
      }

      public <T> Ic2BlockState.Ic2BlockStateInstance withProperty(UnlistedProperty<T> property, T value) {
         if (!property.isValid(value)) {
            throw new IllegalArgumentException("Invalid value for " + property);
         } else if (this.extraProperties.containsKey(property) && this.extraProperties.get(property) == value) {
            return this;
         } else {
            Map<UnlistedProperty<?>, Object> next = new IdentityHashMap<>(this.extraProperties);
            next.put(property, value);
            return new Ic2BlockState.Ic2BlockStateInstance(this.canonical, next);
         }
      }

      public Ic2BlockState.Ic2BlockStateInstance withProperties(Object... properties) {
         if (properties.length % 2 != 0) {
            throw new IllegalArgumentException("Property pairs expected");
         } else {
            Map<UnlistedProperty<?>, Object> next = new IdentityHashMap<>(this.extraProperties);

            for (int i = 0; i < properties.length; i += 2) {
               UnlistedProperty<?> property = (UnlistedProperty<?>)properties[i];
               Object value = properties[i + 1];
               if (value != null && !property.getType().isInstance(value)) {
                  throw new IllegalArgumentException("Invalid value for " + property);
               }

               next.put(property, value);
            }

            return next.equals(this.extraProperties) ? this : new Ic2BlockState.Ic2BlockStateInstance(this.canonical, next);
         }
      }

      public boolean hasValue(UnlistedProperty<?> property) {
         return this.extraProperties.containsKey(property);
      }

      public <T> T getValue(UnlistedProperty<T> property) {
         return property.getType().cast(this.extraProperties.get(property));
      }

      public boolean hasExtraProperties() {
         return !this.extraProperties.isEmpty();
      }

      public String toString() {
         return this.extraProperties.isEmpty() ? super.toString() : super.toString() + this.extraProperties;
      }
   }
}
