package ic2.core.block;

import ic2.core.PointExplosion;
import ic2.core.entity.Ic2Entity;
import ic2.core.entity.Ic2EntityTypes;
import ic2.core.ref.ItemName;
import ic2.core.util.Util;
import ic2.core.util.Vector3;
import ic2.core.util.WorldQueries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class EntityDynamite extends Ic2Entity implements ItemSupplier {
   public boolean sticky = false;
   public static final int netId = 142;
   public BlockPos stickPos;
   public int fuse = 100;
   private boolean inGround = false;
   public LivingEntity owner;
   private int ticksInGround;

   public ItemStack getItem() {
      return ItemName.dynamite.getItemStack();
   }

   public EntityDynamite(Level world, double x, double y, double z) {
      this(Ic2EntityTypes.get("dynamite"), world, x, y, z);
   }

   protected EntityDynamite(EntityType<?> type, Level world, double x, double y, double z) {
      super(type, world);
      this.func_70105_a(0.5F, 0.5F);
      this.setPos(x, y, z);
   }

   public EntityDynamite(Level world) {
      this(world, 0.0, 0.0, 0.0);
   }

   public EntityDynamite(Level world, LivingEntity owner) {
      this(Ic2EntityTypes.get("dynamite"), world, owner);
   }

   protected EntityDynamite(EntityType<?> type, Level world, LivingEntity owner) {
      super(type, world);
      this.owner = owner;
      this.func_70105_a(0.5F, 0.5F);
      Vector3 eyePos = Util.getEyePosition(owner);
      this.snapTo(eyePos.x, eyePos.y, eyePos.z, owner.getYRot(), owner.getXRot());
      this.setPos(this.getX() - Math.cos(Math.toRadians(this.getYRot())) * 0.16, this.getY(), this.getZ());
      this.setPos(this.getX(), this.getY() - 0.1, this.getZ());
      this.setPos(this.getX(), this.getY(), this.getZ() - Math.sin(Math.toRadians(this.getYRot())) * 0.16);
      this.setPos(this.getX(), this.getY(), this.getZ());
      this.setDeltaMovement(
         new Vec3(-Math.sin(Math.toRadians(this.getYRot())) * Math.cos(Math.toRadians(this.getXRot())), this.getDeltaMovement().y, this.getDeltaMovement().z)
      );
      this.setDeltaMovement(
         new Vec3(this.getDeltaMovement().x, this.getDeltaMovement().y, Math.cos(Math.toRadians(this.getYRot())) * Math.cos(Math.toRadians(this.getXRot())))
      );
      this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, -Math.sin(Math.toRadians(this.getXRot())), this.getDeltaMovement().z));
      this.func_70186_c(this.getDeltaMovement().x, this.getDeltaMovement().y, this.getDeltaMovement().z, 1.0F, 1.0F);
   }

   @Override
   protected void func_70088_a() {
   }

   public void func_70186_c(double x, double y, double z, float velocity, float inaccuracy) {
      double len = Math.sqrt(x * x + y * y + z * z);
      x /= len;
      y /= len;
      z /= len;
      x += this.getRandom().nextGaussian() * 0.0075 * inaccuracy;
      y += this.getRandom().nextGaussian() * 0.0075 * inaccuracy;
      z += this.getRandom().nextGaussian() * 0.0075 * inaccuracy;
      x *= velocity;
      y *= velocity;
      z *= velocity;
      this.setDeltaMovement(new Vec3(x, this.getDeltaMovement().y, this.getDeltaMovement().z));
      this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, y, this.getDeltaMovement().z));
      this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, this.getDeltaMovement().y, z));
      double hLen = Math.sqrt(x * x + z * z);
      this.yRotO = (float)(Math.atan2(x, z) * 180.0 / Math.PI);
      this.setYRot(this.yRotO);
      this.xRotO = (float)(Math.atan2(y, hLen) * 180.0 / Math.PI);
      this.setXRot(this.xRotO);
      this.ticksInGround = 0;
   }

   public void setDeltaMovement(double x, double y, double z) {
      this.setDeltaMovement(new Vec3(x, this.getDeltaMovement().y, this.getDeltaMovement().z));
      this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, y, this.getDeltaMovement().z));
      this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, this.getDeltaMovement().y, z));
      if (this.xRotO == 0.0F && this.yRotO == 0.0F) {
         double h = Math.sqrt(x * x + z * z);
         this.yRotO = (float)(Math.atan2(x, z) * 180.0 / Math.PI);
         this.setYRot(this.yRotO);
         this.xRotO = (float)(Math.atan2(y, h) * 180.0 / Math.PI);
         this.setXRot(this.xRotO);
         this.xRotO = this.getXRot();
         this.yRotO = this.getYRot();
         this.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
         this.ticksInGround = 0;
      }
   }

   public void tick() {
      super.tick();
      if (this.xRotO == 0.0F && this.yRotO == 0.0F) {
         double hLen = Math.sqrt(this.getDeltaMovement().x * this.getDeltaMovement().x + this.getDeltaMovement().z * this.getDeltaMovement().z);
         this.yRotO = (float)Math.toDegrees(Math.atan2(this.getDeltaMovement().x, this.getDeltaMovement().z));
         this.setYRot(this.yRotO);
         this.xRotO = (float)Math.toDegrees(Math.atan2(this.getDeltaMovement().y, hLen));
         this.setXRot(this.xRotO);
      }

      Level world = this.level();
      if (this.fuse-- <= 0) {
         this.discard();
         if (!world.isClientSide()) {
            this.explode();
         }
      } else if (this.fuse < 100 && this.fuse % 2 == 0) {
         world.addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5, this.getZ(), 0.0, 0.0, 0.0);
      }

      if (this.inGround) {
         this.ticksInGround++;
         if (this.ticksInGround >= 200) {
            this.discard();
         }

         if (this.sticky) {
            this.fuse -= 3;
            this.setDeltaMovement(new Vec3(0.0, this.getDeltaMovement().y, this.getDeltaMovement().z));
            this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, 0.0, this.getDeltaMovement().z));
            this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, this.getDeltaMovement().y, 0.0));
            if (!world.isEmptyBlock(this.stickPos)) {
               return;
            }
         }
      }

      Vec3 start = this.position();
      Vec3 end = new Vec3(this.getX() + this.getDeltaMovement().x, this.getY() + this.getDeltaMovement().y, this.getZ() + this.getDeltaMovement().z);
      HitResult result = WorldQueries.clip(world, start, end, false);
      if (result != null) {
         float remainX = (float)(result.getLocation().x - this.getX());
         float remainY = (float)(result.getLocation().y - this.getY());
         float remainZ = (float)(result.getLocation().z - this.getZ());
         double remDist = Math.sqrt(remainX * remainX + remainY * remainY + remainZ * remainZ);
         this.stickPos = ((BlockHitResult)result).getBlockPos();
         this.setPos(this.getX() - remainX / remDist * 0.05, this.getY(), this.getZ());
         this.setPos(this.getX(), this.getY() - remainY / remDist * 0.05, this.getZ());
         this.setPos(this.getX(), this.getY(), this.getZ() - remainZ / remDist * 0.05);
         this.setPos(this.getX() + remainX, this.getY(), this.getZ());
         this.setPos(this.getX(), this.getY() + remainY, this.getZ());
         this.setPos(this.getX(), this.getY(), this.getZ() + remainZ);
         this.setDeltaMovement(
            new Vec3(this.getDeltaMovement().x * (0.75F - this.getRandom().nextFloat()), this.getDeltaMovement().y, this.getDeltaMovement().z)
         );
         this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, this.getDeltaMovement().y * -0.3F, this.getDeltaMovement().z));
         this.setDeltaMovement(
            new Vec3(this.getDeltaMovement().x, this.getDeltaMovement().y, this.getDeltaMovement().z * (0.75F - this.getRandom().nextFloat()))
         );
         this.inGround = true;
      } else {
         this.setPos(this.getX() + this.getDeltaMovement().x, this.getY(), this.getZ());
         this.setPos(this.getX(), this.getY() + this.getDeltaMovement().y, this.getZ());
         this.setPos(this.getX(), this.getY(), this.getZ() + this.getDeltaMovement().z);
         this.inGround = false;
      }

      double hMotion = Math.sqrt(this.getDeltaMovement().x * this.getDeltaMovement().x + this.getDeltaMovement().z * this.getDeltaMovement().z);
      this.setYRot((float)Math.toDegrees(Math.atan2(this.getDeltaMovement().x, this.getDeltaMovement().z)));
      this.setXRot((float)Math.toDegrees(Math.atan2(this.getDeltaMovement().y, hMotion)));

      while (this.getXRot() - this.xRotO < -180.0F) {
         this.xRotO -= 360.0F;
      }

      while (this.getXRot() - this.xRotO >= 180.0F) {
         this.xRotO += 360.0F;
      }

      while (this.getYRot() - this.yRotO < -180.0F) {
         this.yRotO -= 360.0F;
      }

      while (this.getYRot() - this.yRotO >= 180.0F) {
         this.yRotO += 360.0F;
      }

      this.setXRot(this.xRotO + (this.getXRot() - this.xRotO) * 0.2F);
      this.setYRot(this.yRotO + (this.getYRot() - this.yRotO) * 0.2F);
      float f3 = 0.98F;
      float f5 = 0.04F;
      if (this.isInWater()) {
         this.fuse += 2000;

         for (int i1 = 0; i1 < 4; i1++) {
            float f6 = 0.25F;
            world.addParticle(
               ParticleTypes.BUBBLE,
               this.getX() - this.getDeltaMovement().x * f6,
               this.getY() - this.getDeltaMovement().y * f6,
               this.getZ() - this.getDeltaMovement().z * f6,
               this.getDeltaMovement().x,
               this.getDeltaMovement().y,
               this.getDeltaMovement().z
            );
         }

         f3 = 0.75F;
      }

      this.setDeltaMovement(new Vec3(this.getDeltaMovement().x * f3, this.getDeltaMovement().y, this.getDeltaMovement().z));
      this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, this.getDeltaMovement().y * f3, this.getDeltaMovement().z));
      this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, this.getDeltaMovement().y, this.getDeltaMovement().z * f3));
      this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, this.getDeltaMovement().y - f5, this.getDeltaMovement().z));
      this.setPos(this.getX(), this.getY(), this.getZ());
   }

   @Override
   public void func_70014_b(CompoundTag nbttagcompound) {
      nbttagcompound.putByte("inGround", (byte)(this.inGround ? 1 : 0));
   }

   @Override
   public void func_70037_a(CompoundTag nbttagcompound) {
      this.inGround = nbttagcompound.getByteOr("inGround", (byte)0) == 1;
   }

   public void explode() {
      PointExplosion explosion = new PointExplosion(this.level(), this, this.owner, this.getX(), this.getY(), this.getZ(), 1.0F, 1.0F, 20);
      explosion.func_77278_a();
      explosion.func_77279_a(true);
   }
}
