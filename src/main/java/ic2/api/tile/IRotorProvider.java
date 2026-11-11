package ic2.api.tile;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

public interface IRotorProvider {
   int getRotorDiameter();

   Direction getFacing();

   float getAngle();

   Identifier getRotorRenderTexture();
}
