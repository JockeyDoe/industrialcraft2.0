package ic2.core.block;

import ic2.core.ExplosionIC2;
import ic2.core.IC2;
import ic2.core.entity.Ic2Entity;
import ic2.core.entity.Ic2EntityTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class EntityIC2Explosive extends Ic2Entity {
   public DamageSource damageSource;
   public LivingEntity igniter;
   public int fuse = 80;
   public float explosivePower = 4.0F;
   public int radiationRange = 0;
   public float dropRate = 0.3F;
   public float damageVsEntitys = 1.0F;
   public BlockState renderBlockState = Blocks.DIRT.defaultBlockState();

   public EntityIC2Explosive(Level world) {
      this(Ic2EntityTypes.get("itnt"), world);
   }

   protected EntityIC2Explosive(EntityType<?> type, Level world) {
      super(type, world);
      this.blocksBuilding = true;
      this.func_70105_a(0.98F, 0.98F);
   }

   public EntityIC2Explosive(
      EntityType<?> type,
      Level world,
      double x,
      double y,
      double z,
      int fuse,
      float power,
      float dropRate,
      float damage,
      BlockState renderBlockState,
      int radiationRange
   ) {
      this(type, world);
      this.setPos(x, y, z);
      float f = (float)(Math.random() * (float) Math.PI * 2.0);
      this.setDeltaMovement(new Vec3(-Mth.sin(f * 3.141593F / 180.0F) * 0.02F, this.getDeltaMovement().y, this.getDeltaMovement().z));
      this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, 0.2F, this.getDeltaMovement().z));
      this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, this.getDeltaMovement().y, -Mth.cos(f * 3.141593F / 180.0F) * 0.02F));
      this.xo = x;
      this.yo = y;
      this.zo = z;
      this.fuse = fuse;
      this.explosivePower = power;
      this.radiationRange = radiationRange;
      this.dropRate = dropRate;
      this.damageVsEntitys = damage;
      this.renderBlockState = renderBlockState;
   }

   @Override
   protected void func_70088_a() {
   }

   protected boolean func_70041_e_() {
      return false;
   }

   @Override
   public boolean func_70067_L() {
      return !this.isRemoved();
   }

   public void tick() {
      this.xo = this.getX();
      this.yo = this.getY();
      this.zo = this.getZ();
      this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, this.getDeltaMovement().y - 0.04, this.getDeltaMovement().z));
      this.func_70091_d(MoverType.SELF, this.getDeltaMovement().x, this.getDeltaMovement().y, this.getDeltaMovement().z);
      this.setDeltaMovement(new Vec3(this.getDeltaMovement().x * 0.98, this.getDeltaMovement().y, this.getDeltaMovement().z));
      this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, this.getDeltaMovement().y * 0.98, this.getDeltaMovement().z));
      this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, this.getDeltaMovement().y, this.getDeltaMovement().z * 0.98));
      if (this.onGround()) {
         this.setDeltaMovement(new Vec3(this.getDeltaMovement().x * 0.7, this.getDeltaMovement().y, this.getDeltaMovement().z));
         this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, this.getDeltaMovement().y, this.getDeltaMovement().z * 0.7));
         this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, this.getDeltaMovement().y * -0.5, this.getDeltaMovement().z));
      }

      if (this.fuse-- <= 0) {
         this.discard();
         if (IC2.platform.isSimulating()) {
            this.explode();
         }
      } else {
         this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5, this.getZ(), 0.0, 0.0, 0.0);
      }
   }

   private void explode() {
      ExplosionIC2 explosion = new ExplosionIC2(
         this.level(),
         this,
         this.getX(),
         this.getY(),
         this.getZ(),
         this.explosivePower,
         this.dropRate,
         this.radiationRange > 0 ? ExplosionIC2.Type.Nuclear : ExplosionIC2.Type.Normal,
         this.igniter,
         this.radiationRange
      );
      explosion.doExplosion();
   }

   @Override
   protected void func_70014_b(CompoundTag nbttagcompound) {
      nbttagcompound.putInt("Fuse", this.fuse);
      nbttagcompound.putFloat("Power", this.explosivePower);
      nbttagcompound.putInt("RadiationRange", this.radiationRange);
      nbttagcompound.putFloat("DropRate", this.dropRate);
      nbttagcompound.putFloat("EntityDamage", this.damageVsEntitys);
   }

   @Override
   protected void func_70037_a(CompoundTag nbttagcompound) {
      this.fuse = nbttagcompound.getIntOr("Fuse", this.fuse);
      this.explosivePower = nbttagcompound.getFloatOr("Power", this.explosivePower);
      this.radiationRange = nbttagcompound.getIntOr("RadiationRange", this.radiationRange);
      this.dropRate = nbttagcompound.getFloatOr("DropRate", this.dropRate);
      this.damageVsEntitys = nbttagcompound.getFloatOr("EntityDamage", this.damageVsEntitys);
   }

   public EntityIC2Explosive setIgniter(LivingEntity igniter1) {
      this.igniter = igniter1;
      return this;
   }
}
