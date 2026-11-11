package ic2.core.block;

import ic2.core.entity.Ic2EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class EntityStickyDynamite extends EntityDynamite {
   public EntityStickyDynamite(Level world) {
      super(Ic2EntityTypes.get("sticky_dynamite"), world, 0.0, 0.0, 0.0);
      this.sticky = true;
   }

   public EntityStickyDynamite(Level world, LivingEntity entityliving) {
      super(Ic2EntityTypes.get("sticky_dynamite"), world, entityliving);
      this.sticky = true;
   }

   public EntityStickyDynamite(Level world, double x, double y, double z) {
      super(Ic2EntityTypes.get("sticky_dynamite"), world, x, y, z);
      this.sticky = true;
   }
}
