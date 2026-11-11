package ic2.core;

import ic2.core.ref.FluidName;
import ic2.core.ref.IFluidModelProvider;
import java.util.Objects;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LiquidBlock;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.BaseFlowingFluid.Flowing;
import net.neoforged.neoforge.fluids.BaseFlowingFluid.Properties;
import net.neoforged.neoforge.fluids.BaseFlowingFluid.Source;

public class Ic2Fluid extends Source implements IFluidModelProvider {
   private final Ic2Fluid.Definition definition;

   public Ic2Fluid(FluidName name) {
      this(new Ic2Fluid.Definition(name));
   }

   private Ic2Fluid(Ic2Fluid.Definition data) {
      super(data.properties);
      this.definition = data;
      data.source = this;
   }

   public Ic2Fluid setDensity(int value) {
      this.definition.density = value;
      return this;
   }

   public Ic2Fluid setViscosity(int value) {
      this.definition.viscosity = value;
      return this;
   }

   public Ic2Fluid setLuminosity(int value) {
      this.definition.light = value;
      return this;
   }

   public Ic2Fluid setTemperature(int value) {
      this.definition.temperature = value;
      return this;
   }

   public Ic2Fluid setGaseous(boolean value) {
      this.definition.gas = value;
      return this;
   }

   public void setBlock(LiquidBlock block) {
      this.definition.block = block;
   }

   public LiquidBlock getBlock() {
      return this.definition.block;
   }

   public String getUnlocalizedName() {
      return "ic2.fluid." + this.definition.name.name();
   }

   @Override
   public void registerModels(FluidName name) {
      if (name.getInstance() != this) {
         throw new IllegalArgumentException("Wrong IC2 fluid model name");
      }
   }

   public int getTickDelay(LevelReader world) {
      return Math.max(1, this.definition.viscosity / 200);
   }

   public Flowing getFlowing() {
      return this.definition.flowing();
   }

   public Ic2Fluid getSource() {
      return this;
   }

   public FluidType getFluidType() {
      return this.definition.type;
   }

   public static final class Definition {
      final FluidName name;
      Ic2Fluid source;
      Flowing flowing;
      LiquidBlock block;
      int density = 1000;
      int viscosity = 1000;
      int light;
      int temperature = 300;
      boolean gas;
      final FluidType type;
      final Properties properties;

      Definition(final FluidName name) {
         this.name = name;
         this.type = new FluidType(net.neoforged.neoforge.fluids.FluidType.Properties.create()) {
            {
               Objects.requireNonNull(Definition.this);
            }

            public int getDensity() {
               return Definition.this.gas ? -Math.max(1, Math.abs(Definition.this.density)) : Definition.this.density;
            }

            public int getViscosity() {
               return Definition.this.viscosity;
            }

            public int getTemperature() {
               return Definition.this.temperature;
            }

            public int getLightLevel() {
               return Definition.this.light;
            }

            public String getDescriptionId() {
               return "ic2.fluid." + name.name();
            }

            public Component getDescription() {
               return Component.translatable(this.getDescriptionId());
            }
         };
         this.properties = new Properties(() -> this.type, () -> this.source, this::flowing).block(() -> this.block).bucket(() -> Items.AIR);
      }

      Flowing flowing() {
         if (this.flowing == null) {
            this.flowing = new Flowing(this.properties);
         }

         return this.flowing;
      }
   }
}
