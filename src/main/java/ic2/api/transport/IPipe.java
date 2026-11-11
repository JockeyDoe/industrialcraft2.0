package ic2.api.transport;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface IPipe {
   BlockEntity getTile();

   boolean isConnected(Direction var1);

   void flipConnection(Direction var1);
}
