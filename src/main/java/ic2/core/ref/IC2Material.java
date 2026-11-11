package ic2.core.ref;

import net.minecraft.world.level.material.MapColor;

public class IC2Material extends BlockMaterial {
   public static final IC2Material MACHINE = new IC2Material("ic2_material_machine", true, true);
   public static final IC2Material PIPE = new IC2Material("ic2_material_pipe", true, true);
   public static final IC2Material CABLE = new IC2Material("ic2_material_cable", false, true);

   public IC2Material(String name, boolean requiresTool, boolean immovableMobility) {
      super(name, MapColor.METAL, true, false, false, false);
      this.requiresTool = requiresTool;
      this.immovable = immovableMobility;
   }
}
