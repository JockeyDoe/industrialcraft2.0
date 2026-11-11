package ic2.core.block.steam;

import net.minecraft.core.Direction;

public interface IKineticProvider {
   int getProvidedPower(Direction var1);

   int getMaxPower();
}
