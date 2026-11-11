package ic2.api.recipe;

import java.util.Map;
import net.minecraft.world.level.material.Fluid;

public interface ISemiFluidFuelManager extends ILiquidAcceptManager {
   void addFluid(String var1, long var2, long var4);

   void removeFluid(String var1);

   ISemiFluidFuelManager.FuelProperty getFuelProperty(Fluid var1);

   Map<String, ISemiFluidFuelManager.FuelProperty> getFuelProperties();

   public static final class FuelProperty {
      public final long energyPerMb;
      public final long energyPerTick;

      public FuelProperty(long energyPerMb, long energyPerTick) {
         this.energyPerMb = energyPerMb;
         this.energyPerTick = energyPerTick;
      }
   }
}
