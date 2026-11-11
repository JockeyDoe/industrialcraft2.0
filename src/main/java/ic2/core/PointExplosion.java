package ic2.core;

import ic2.api.event.ExplosionEvent;
import ic2.core.util.Util;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion.BlockInteraction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

public class PointExplosion implements Explosion {
   private final Level world;
   private final Entity entity;
   private final float dropRate;
   private final int entityDamage;
   private float explosionSize;
   private final Vec3 center;
   private final LivingEntity igniter;
   private final List<BlockPos> targets = new ArrayList<>();

   public PointExplosion(Level world1, Entity entity, LivingEntity exploder, double x, double y, double z, float power, float dropRate1, int entityDamage1) {
      this.center = new Vec3(x, y, z);
      this.igniter = exploder;
      this.world = world1;
      this.entity = entity;
      this.dropRate = dropRate1;
      this.entityDamage = entityDamage1;
      this.explosionSize = power;
   }

   public ServerLevel level() {
      return (ServerLevel)this.world;
   }

   public BlockInteraction getBlockInteraction() {
      return this.dropRate > 0.0F ? BlockInteraction.DESTROY_WITH_DECAY : BlockInteraction.DESTROY;
   }

   public LivingEntity getIndirectSourceEntity() {
      return this.igniter;
   }

   public Entity getDirectSourceEntity() {
      return this.entity;
   }

   public float radius() {
      return this.explosionSize;
   }

   public Vec3 center() {
      return this.center;
   }

   public Vec3 getPosition() {
      return this.center;
   }

   public LivingEntity func_94613_c() {
      return this.igniter;
   }

   public List<BlockPos> func_180343_e() {
      return this.targets;
   }

   public boolean canTriggerBlocks() {
      return false;
   }

   public boolean shouldAffectBlocklikeEntities() {
      return true;
   }

   public void func_77279_a(boolean particles) {
      for (BlockPos pos : this.targets) {
         this.world.getBlockState(pos).onExplosionHit(this.level(), pos, this, (stack, dropPos) -> {
            if (this.dropRate > 0.0F) {
               Block.popResource(this.world, dropPos, stack);
            }
         });
      }

      if (particles) {
         this.level().sendParticles(ParticleTypes.EXPLOSION, this.center.x, this.center.y, this.center.z, 1, 0.0, 0.0, 0.0, 0.0);
      }

      this.world.playSound(null, this.center.x, this.center.y, this.center.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F, 1.0F);
   }

   public void func_77278_a() {
      double explosionX = this.getPosition().x;
      double explosionY = this.getPosition().y;
      double explosionZ = this.getPosition().z;
      ExplosionEvent event = new ExplosionEvent(this.world, this.entity, this.getPosition(), this.explosionSize, this.func_94613_c(), 0, 1.0);
      if (!((ExplosionEvent)NeoForge.EVENT_BUS.post(event)).isCanceled()) {
         for (int x = Util.roundToNegInf(explosionX) - 1; x <= Util.roundToNegInf(explosionX) + 1; x++) {
            for (int y = Util.roundToNegInf(explosionY) - 1; y <= Util.roundToNegInf(explosionY) + 1; y++) {
               for (int z = Util.roundToNegInf(explosionZ) - 1; z <= Util.roundToNegInf(explosionZ) + 1; z++) {
                  BlockPos pos = new BlockPos(x, y, z);
                  BlockState block = this.world.getBlockState(pos);
                  if (block.getExplosionResistance(this.world, pos, this) < this.explosionSize * 10.0F) {
                     this.func_180343_e().add(pos);
                  }
               }
            }
         }

         for (Entity entity : this.world
            .getEntities(
               this.func_94613_c(), new AABB(explosionX - 2.0, explosionY - 2.0, explosionZ - 2.0, explosionX + 2.0, explosionY + 2.0, explosionZ + 2.0)
            )) {
            entity.hurtServer(this.level(), this.world.damageSources().explosion(this), this.entityDamage);
         }

         this.explosionSize = this.dropRate > 0.0F ? 1.0F / this.dropRate : Float.MAX_VALUE;
      }
   }
}
