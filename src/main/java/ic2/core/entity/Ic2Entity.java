package ic2.core.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Entity.MovementEmission;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public abstract class Ic2Entity extends Entity {
   private EntityDimensions ic2Dimensions;

   protected Ic2Entity(EntityType<?> type, Level level) {
      super(type, level);
   }

   protected void defineSynchedData(Builder builder) {
      this.func_70088_a();
   }

   protected void func_70088_a() {
   }

   protected void func_70037_a(CompoundTag tag) {
   }

   protected void func_70014_b(CompoundTag tag) {
   }

   protected void addAdditionalSaveData(ValueOutput output) {
      CompoundTag tag = new CompoundTag();
      this.func_70014_b(tag);
      EntityTagData.write(output, tag);
   }

   protected void readAdditionalSaveData(ValueInput input) {
      this.func_70037_a(EntityTagData.read(input));
   }

   public void func_70105_a(float width, float height) {
      this.ic2Dimensions = EntityDimensions.scalable(width, height);
      this.refreshDimensions();
   }

   public EntityDimensions getDimensions(Pose pose) {
      return this.ic2Dimensions == null ? super.getDimensions(pose) : this.ic2Dimensions;
   }

   public void func_70091_d(MoverType type, double x, double y, double z) {
      this.move(type, new Vec3(x, y, z));
   }

   public void func_70091_d(MoverType type, Vec3 movement) {
      this.move(type, movement);
   }

   public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
      return false;
   }

   public boolean func_70067_L() {
      return false;
   }

   public boolean isPickable() {
      return this.func_70067_L();
   }

   protected MovementEmission getMovementEmission() {
      return MovementEmission.NONE;
   }
}
