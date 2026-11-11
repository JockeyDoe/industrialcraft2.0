package ic2.core.block.machine.tileentity;

import ic2.core.block.EntityIC2Explosive;
import ic2.core.block.EntityItnt;
import net.minecraft.world.entity.LivingEntity;

public class ITnt extends Explosive {
   @Override
   protected boolean explodeOnRemoval() {
      return true;
   }

   @Override
   protected EntityIC2Explosive getEntity(LivingEntity igniter) {
      return new EntityItnt(this.getLevel(), this.getBlockPos().getX() + 0.5, this.getBlockPos().getY() + 0.5, this.getBlockPos().getZ() + 0.5);
   }
}
