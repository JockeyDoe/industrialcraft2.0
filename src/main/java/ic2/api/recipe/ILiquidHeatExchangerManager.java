package ic2.api.recipe;

import java.util.Map;
import net.minecraft.world.level.material.Fluid;

public interface ILiquidHeatExchangerManager extends ILiquidAcceptManager {
   void addFluid(String var1, String var2, int var3);

   ILiquidHeatExchangerManager.HeatExchangeProperty getHeatExchangeProperty(Fluid var1);

   Map<String, ILiquidHeatExchangerManager.HeatExchangeProperty> getHeatExchangeProperties();

   ILiquidAcceptManager getSingleDirectionLiquidManager();

   public static class HeatExchangeProperty {
      public final Fluid outputFluid;
      public final int huPerMB;

      public HeatExchangeProperty(Fluid outputFluid, int huPerMB) {
         this.outputFluid = outputFluid;
         this.huPerMB = huPerMB;
      }
   }
}
