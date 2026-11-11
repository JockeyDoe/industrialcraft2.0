package ic2.core.block;

import ic2.core.entity.Ic2EntityTypes;
import ic2.core.ref.BlockName;
import ic2.core.ref.TeBlock;
import net.minecraft.world.level.Level;

public class EntityItnt extends EntityIC2Explosive {
   public EntityItnt(Level world, double x, double y, double z) {
      super(Ic2EntityTypes.get("itnt"), world, x, y, z, 60, 5.5F, 0.9F, 0.3F, BlockName.te.getBlockState(TeBlock.itnt), 0);
   }

   public EntityItnt(Level world) {
      this(world, 0.0, 0.0, 0.0);
   }
}
