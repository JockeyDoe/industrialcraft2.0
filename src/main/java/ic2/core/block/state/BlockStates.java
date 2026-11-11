package ic2.core.block.state;

import ic2.core.block.BlockBase;
import ic2.core.block.Ic2Leaves;
import java.util.Arrays;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.Property;

public final class BlockStates {
   private BlockStates() {
   }

   public static StateDefinition<Block, BlockState> definition(Block block, Property<?>[] properties) {
      return new Builder(block)
         .add(Arrays.stream(properties).filter(property -> property.getPossibleValues().size() > 1).toArray(Property[]::new))
         .create(Block::defaultBlockState, BlockState::new);
   }

   public static <T extends Comparable<T>> T value(BlockState state, Property<T> property) {
      return (T)(property.getPossibleValues().size() == 1 ? property.getPossibleValues().iterator().next() : state.getValue(property));
   }

   public static <T extends Comparable<T>> BlockState with(BlockState state, Property<T> property, T value) {
      if (property.getPossibleValues().size() == 1) {
         if (!property.getPossibleValues().contains(value)) {
            throw new IllegalArgumentException("Invalid constant property value: " + value);
         } else {
            return state;
         }
      } else {
         return (BlockState)state.setValue(property, value);
      }
   }

   public static int metadata(BlockState state) {
      return state.getBlock() instanceof BlockBase base
         ? base.func_176201_c(state)
         : (state.getBlock() instanceof Ic2Leaves leaves ? leaves.func_176201_c(state) : 0);
   }

   public static BlockState state(Block block, int meta) {
      return block instanceof BlockBase base
         ? base.func_176203_a(meta)
         : (block instanceof Ic2Leaves leaves ? leaves.func_176203_a(meta) : block.defaultBlockState());
   }
}
