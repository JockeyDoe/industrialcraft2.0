package ic2.core.block.machine.tileentity;

import ic2.core.IC2;
import ic2.core.IC2DamageSource;
import ic2.core.block.TileEntityBlock;
import ic2.core.block.comp.Energy;
import ic2.core.block.comp.Redstone;
import ic2.core.item.armor.ItemArmorHazmat;
import ic2.core.util.WorldQueries;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class TileEntityTesla extends TileEntityBlock {
   protected final Redstone redstone;
   protected final Energy energy;
   private int ticker = IC2.random.nextInt(32);

   public TileEntityTesla() {
      this.redstone = this.addComponent(new Redstone(this));
      this.energy = this.addComponent(Energy.asBasicSink(this, 10000.0, 2));
   }

   @Override
   protected void updateEntityServer() {
      super.updateEntityServer();
      if (this.redstone.hasRedstoneInput() && this.energy.useEnergy(1.0) && ++this.ticker % 32 == 0) {
         int damage = (int)this.energy.getEnergy() / 400;
         if (damage > 0 && this.shock(damage)) {
            this.energy.useEnergy(damage * 400);
         }
      }
   }

   protected boolean shock(int damage) {
      int r = 4;
      Level world = this.getLevel();

      for (LivingEntity entity : world.getEntitiesOfClass(
         LivingEntity.class,
         new AABB(
            this.getBlockPos().getX() - 4,
            this.getBlockPos().getY() - 4,
            this.getBlockPos().getZ() - 4,
            this.getBlockPos().getX() + 4 + 1,
            this.getBlockPos().getY() + 4 + 1,
            this.getBlockPos().getZ() + 4 + 1
         )
      )) {
         if (!ItemArmorHazmat.hasCompleteHazmat(entity) && WorldQueries.hurt(entity, IC2DamageSource.electricity, damage)) {
            if (world instanceof ServerLevel worldServer) {
               RandomSource rnd = world.getRandom();

               for (int i = 0; i < damage; i++) {
                  worldServer.sendParticles(
                     DustParticleOptions.REDSTONE,
                     true,
                     false,
                     entity.getX() + rnd.nextFloat() - 0.5,
                     entity.getY() + rnd.nextFloat() * 2.0F - 1.0,
                     entity.getZ() + rnd.nextFloat() - 0.5,
                     0,
                     0.1,
                     0.1,
                     1.0,
                     1.0
                  );
               }
            }

            return true;
         }
      }

      return false;
   }
}
