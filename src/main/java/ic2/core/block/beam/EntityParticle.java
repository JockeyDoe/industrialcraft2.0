package ic2.core.block.beam;

import ic2.core.entity.Ic2Entity;
import ic2.core.entity.Ic2EntityTypes;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class EntityParticle extends Ic2Entity {
   private static final double initialVelocity = 0.5;
   private static final double slowdown = 0.99;

   public EntityParticle(Level world) {
      super(Ic2EntityTypes.get("beam"), world);
      this.noPhysics = true;
   }

   public EntityParticle(TileEmitter emitter) {
      this(emitter.getLevel());
      Direction dir = emitter.getFacing();
      double x = emitter.getBlockPos().getX() + 0.5 + dir.getStepX() * 0.5;
      double y = emitter.getBlockPos().getY() + 0.5 + dir.getStepY() * 0.5;
      double z = emitter.getBlockPos().getZ() + 0.5 + dir.getStepZ() * 0.5;
      this.setPos(x, y, z);
      this.setDeltaMovement(new Vec3(dir.getStepX() * 0.5, this.getDeltaMovement().y, this.getDeltaMovement().z));
      this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, dir.getStepY() * 0.5, this.getDeltaMovement().z));
      this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, this.getDeltaMovement().y, dir.getStepZ() * 0.5));
      this.func_70105_a(0.2F, 0.2F);
   }

   @Override
   protected void func_70088_a() {
   }

   @Override
   protected void func_70037_a(CompoundTag nbttagcompound) {
   }

   @Override
   protected void func_70014_b(CompoundTag nbttagcompound) {
   }

   public void tick() {
      this.xo = this.getX();
      this.yo = this.getY();
      this.zo = this.getZ();
      this.func_70091_d(MoverType.SELF, this.getDeltaMovement().x, this.getDeltaMovement().y, this.getDeltaMovement().z);
      this.setDeltaMovement(new Vec3(this.getDeltaMovement().x * 0.99, this.getDeltaMovement().y, this.getDeltaMovement().z));
      this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, this.getDeltaMovement().y * 0.99, this.getDeltaMovement().z));
      this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, this.getDeltaMovement().y, this.getDeltaMovement().z * 0.99));
      if (this.getDeltaMovement().x * this.getDeltaMovement().x
            + this.getDeltaMovement().y * this.getDeltaMovement().y
            + this.getDeltaMovement().z * this.getDeltaMovement().z
         < 1.0E-4) {
         this.discard();
      }
   }
}
