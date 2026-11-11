package ic2.core.block.comp;

import ic2.core.block.TileEntityBlock;
import java.util.Set;
import net.minecraft.core.Direction;

public class Kinetic extends TileEntityComponent {
   private Set<Direction> sinkDirections;
   private Set<Direction> sourceDirections;

   public Kinetic(
      TileEntityBlock parent, double capacity, Set<Direction> sinkDirections, Set<Direction> sourceDirections, int sinkTier, int sourceTier, boolean fullEnergy
   ) {
      super(parent);
   }
}
