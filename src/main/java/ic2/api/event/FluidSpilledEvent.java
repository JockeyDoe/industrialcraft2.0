package ic2.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.fluids.FluidStack;

public final class FluidSpilledEvent extends Event {
   private final FluidStack fluid;
   private final Level level;
   private final BlockPos position;

   public FluidSpilledEvent(FluidStack fluid, Level level, BlockPos position) {
      this.fluid = fluid.copy();
      this.level = level;
      this.position = position.immutable();
   }

   public FluidStack getFluid() {
      return this.fluid.copy();
   }

   public Level getLevel() {
      return this.level;
   }

   public BlockPos getPos() {
      return this.position;
   }
}
