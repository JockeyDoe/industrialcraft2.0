package ic2.core;

import ic2.api.recipe.ILiquidAcceptManager;
import ic2.api.recipe.ILiquidHeatExchangerManager;
import ic2.api.recipe.Recipes;
import ic2.core.init.MainConfig;
import ic2.core.util.FluidKeys;
import ic2.core.util.LogCategory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.world.level.material.Fluid;

public class LiquidHeatExchangerManager implements ILiquidHeatExchangerManager {
   private final boolean heatup;
   private final LiquidHeatExchangerManager.SingleDirectionManager singleDirectionManager;
   private Map<String, ILiquidHeatExchangerManager.HeatExchangeProperty> map = new HashMap<>();

   public LiquidHeatExchangerManager(boolean heatup) {
      this.heatup = heatup;
      this.singleDirectionManager = new LiquidHeatExchangerManager.SingleDirectionManager();
   }

   @Override
   public boolean acceptsFluid(Fluid fluid) {
      return this.map.containsKey(FluidKeys.name(fluid));
   }

   @Override
   public Set<Fluid> getAcceptedFluids() {
      Set<Fluid> fluidSet = new HashSet<>();

      for (String fluidName : this.map.keySet()) {
         fluidSet.add(FluidKeys.find(fluidName));
      }

      return fluidSet;
   }

   @Override
   public void addFluid(String fluidName, String fluidOutput, int huPerMB) {
      if (this.map.containsKey(fluidName)) {
         this.displayError("The fluid " + fluidName + " does already have a HeatExchangerProperty assigned.");
      } else if (huPerMB == 0) {
         this.displayError("A mod tried to register a Fluid for the HeatExchanging recipe, without having an Energy value. Ignoring...");
      } else {
         Fluid liquid1 = FluidKeys.find(fluidName);
         Fluid liquid2 = FluidKeys.find(fluidOutput);
         if (liquid1 != null && liquid2 != null) {
            if (this.heatup) {
               if (liquid1.getFluidType().getTemperature() >= liquid2.getFluidType().getTemperature()) {
                  this.displayError("Cannot heat up a warm liquid into a cold one. " + fluidName + " -> " + fluidOutput);
               }
            } else if (liquid1.getFluidType().getTemperature() <= liquid2.getFluidType().getTemperature()) {
               this.displayError("Cannot cool down a cold liquid into a warm one. " + fluidName + " -> " + fluidOutput);
            }

            this.map.put(fluidName, new ILiquidHeatExchangerManager.HeatExchangeProperty(FluidKeys.find(fluidOutput), Math.abs(huPerMB)));
         } else {
            this.displayError("Could not get both fluids for " + fluidName + " and " + fluidOutput + ".");
         }
      }
   }

   @Override
   public ILiquidHeatExchangerManager.HeatExchangeProperty getHeatExchangeProperty(Fluid fluid) {
      return this.map.containsKey(FluidKeys.name(fluid)) ? this.map.get(FluidKeys.name(fluid)) : null;
   }

   @Override
   public Map<String, ILiquidHeatExchangerManager.HeatExchangeProperty> getHeatExchangeProperties() {
      return this.map;
   }

   private void displayError(String msg) {
      if (MainConfig.ignoreInvalidRecipes) {
         IC2.log.warn(LogCategory.Recipe, msg);
      } else {
         throw new RuntimeException(msg);
      }
   }

   @Override
   public ILiquidAcceptManager getSingleDirectionLiquidManager() {
      return this.singleDirectionManager;
   }

   public ILiquidHeatExchangerManager getOpposite() {
      return this.heatup ? Recipes.liquidCooldownManager : Recipes.liquidHeatupManager;
   }

   public class SingleDirectionManager implements ILiquidAcceptManager {
      public SingleDirectionManager() {
         Objects.requireNonNull(LiquidHeatExchangerManager.this);
         super();
      }

      @Override
      public boolean acceptsFluid(Fluid fluid) {
         if (!LiquidHeatExchangerManager.this.acceptsFluid(fluid)) {
            return false;
         } else {
            ILiquidHeatExchangerManager.HeatExchangeProperty property = LiquidHeatExchangerManager.this.getHeatExchangeProperty(fluid);
            return !LiquidHeatExchangerManager.this.getOpposite().acceptsFluid(property.outputFluid);
         }
      }

      @Override
      public Set<Fluid> getAcceptedFluids() {
         Set<Fluid> ret = new HashSet<>();
         ILiquidHeatExchangerManager opposite = LiquidHeatExchangerManager.this.getOpposite();

         for (Entry<String, ILiquidHeatExchangerManager.HeatExchangeProperty> e : LiquidHeatExchangerManager.this.map.entrySet()) {
            if (!opposite.acceptsFluid(e.getValue().outputFluid)) {
               ret.add(FluidKeys.find(e.getKey()));
            }
         }

         return ret;
      }
   }
}
