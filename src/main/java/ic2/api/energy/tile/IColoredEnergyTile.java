package ic2.api.energy.tile;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;

public interface IColoredEnergyTile extends IEnergyTile {
   DyeColor getColor(Direction var1);
}
