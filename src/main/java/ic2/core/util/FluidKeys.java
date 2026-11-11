package ic2.core.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

public final class FluidKeys {
   private FluidKeys() {
   }

   public static Fluid find(String name) {
      Identifier key = Identifier.tryParse(name);
      if (key == null) {
         return null;
      } else {
         if (name.indexOf(58) < 0 && !name.equals("water") && !name.equals("lava") && !name.equals("empty")) {
            String path = name.startsWith("ic2") ? name.substring(3) : name;
            Fluid own = (Fluid)BuiltInRegistries.FLUID.getOptional(Identifier.fromNamespaceAndPath("ic2", path)).orElse(null);
            if (own != null) {
               return own;
            }
         }

         return (Fluid)BuiltInRegistries.FLUID.getOptional(key).orElse(null);
      }
   }

   public static Fluid source(Fluid fluid) {
      return fluid instanceof FlowingFluid flowing ? flowing.getSource() : fluid;
   }

   public static String name(Fluid fluid) {
      Identifier key = BuiltInRegistries.FLUID.getKey(fluid);
      if (key == null) {
         throw new IllegalArgumentException("Unregistered fluid: " + fluid);
      } else {
         return key.getNamespace().equals("ic2") ? "ic2" + key.getPath() : (key.getNamespace().equals("minecraft") ? key.getPath() : key.toString());
      }
   }

   public static String name(FluidStack stack) {
      return name(stack.getFluid());
   }
}
