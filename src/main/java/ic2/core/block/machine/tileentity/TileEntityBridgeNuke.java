package ic2.core.block.machine.tileentity;

import ic2.core.IC2;
import ic2.core.block.EntityIC2Explosive;
import ic2.core.block.EntityNuke;
import ic2.core.init.MainConfig;
import ic2.core.ref.TeBlock;
import ic2.core.util.ConfigUtil;
import ic2.core.util.LogCategory;
import ic2.core.util.Util;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.Level;

@TeBlock.Delegated(current = TileEntityNuke.class, old = TileEntityBridgeNuke.TileEntityClassicNuke.class)
public abstract class TileEntityBridgeNuke extends Explosive {
   @Override
   public void onPlaced(ItemStack stack, LivingEntity placer, Direction facing) {
      super.onPlaced(stack, placer, facing);
      if (placer instanceof Player player) {
         String playerName = player.getGameProfile().name() + "/" + player.getGameProfile().id();
         IC2.log.log(LogCategory.PlayerActivity, Level.INFO, "Player %s placed a nuke at %s.", playerName, Util.formatPosition(this));
      }
   }

   public abstract float getNukeExplosivePower();

   public abstract int getRadiationRange();

   @Override
   protected EntityIC2Explosive getEntity(LivingEntity igniter) {
      if (!ConfigUtil.getBool(MainConfig.get(), "protection/enableNuke")) {
         return null;
      } else {
         float power = this.getNukeExplosivePower();
         if (power < 0.0F) {
            return null;
         } else {
            int radiationRange = this.getRadiationRange();
            return new EntityNuke(
               this.getLevel(), this.getBlockPos().getX() + 0.5, this.getBlockPos().getY() + 0.5, this.getBlockPos().getZ() + 0.5, power, radiationRange
            );
         }
      }
   }

   @Override
   protected void onIgnite(LivingEntity igniter) {
      String cause = igniter == null ? "indirectly" : "by " + igniter.getClass().getSimpleName() + " " + igniter.getName();
      IC2.log.log(LogCategory.PlayerActivity, Level.INFO, "Nuke at %s was ignited %s.", Util.formatPosition(this), cause);
   }

   public static class TileEntityClassicNuke extends TileEntityBridgeNuke {
      private static final float POWER = 35.0F;

      @Override
      public float getNukeExplosivePower() {
         return Math.min(35.0F, ConfigUtil.getFloat(MainConfig.get(), "protection/nukeExplosionPowerLimit"));
      }

      @Override
      public int getRadiationRange() {
         return 1;
      }
   }
}
