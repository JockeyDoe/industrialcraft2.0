package ic2.core.block.machine.tileentity;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public interface IWeightedDistributor extends Container {
   Direction getFacing();

   @OnlyIn(Dist.CLIENT)
   List<Direction> getPriority();

   void updatePriority(boolean var1);
}
