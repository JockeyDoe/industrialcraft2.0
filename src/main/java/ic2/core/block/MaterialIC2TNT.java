package ic2.core.block;

import ic2.core.ref.BlockMaterial;
import net.minecraft.world.level.material.MapColor;

public class MaterialIC2TNT extends BlockMaterial {
   public static final BlockMaterial instance = new MaterialIC2TNT();

   public MaterialIC2TNT() {
      super("ic2_tnt", MapColor.FIRE, true, false, false, true);
   }
}
