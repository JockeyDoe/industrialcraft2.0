package ic2.core.item.tool;

import ic2.core.ExplosionIC2;
import ic2.core.IC2;
import ic2.core.entity.Ic2Entity;
import ic2.core.entity.Ic2EntityTypes;
import ic2.core.recipe.NativeSmelting;
import ic2.core.util.Quaternion;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import ic2.core.util.Vector3;
import ic2.core.util.WorldQueries;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class EntityParticle extends Ic2Entity {
   private double coreSize;
   private double influenceSize;
   private int lifeTime;
   private Entity owner;
   private Vector3[] radialTestVectors;

   public EntityParticle(Level world) {
      super(Ic2EntityTypes.get("particle"), world);
      this.noPhysics = true;
      this.lifeTime = 6000;
   }

   public EntityParticle(Level world, LivingEntity owner1, float speed, double coreSize1, double influenceSize1) {
      this(world);
      this.coreSize = coreSize1;
      this.influenceSize = influenceSize1;
      this.owner = owner1;
      Vector3 eyePos = Util.getEyePosition(this.owner);
      this.setPos(eyePos.x, eyePos.y, eyePos.z);
      Vector3 motion = new Vector3(owner1.getLookAngle());
      Vector3 ortho = motion.copy().cross(Vector3.UP).scaleTo(influenceSize1);
      double stepAngle = Math.atan(0.5 / influenceSize1) * 2.0;
      int steps = (int)Math.ceil((Math.PI * 2) / stepAngle);
      Quaternion q = new Quaternion().setFromAxisAngle(motion, stepAngle);
      this.radialTestVectors = new Vector3[steps];
      this.radialTestVectors[0] = ortho.copy();

      for (int i = 1; i < steps; i++) {
         q.rotate(ortho);
         this.radialTestVectors[i] = ortho.copy();
      }

      motion.scale(speed);
      this.setDeltaMovement(new Vec3(motion.x, this.getDeltaMovement().y, this.getDeltaMovement().z));
      this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, motion.y, this.getDeltaMovement().z));
      this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, this.getDeltaMovement().y, motion.z));
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

   public Entity getThrower() {
      return this.owner;
   }

   public void setThrower(Entity entity) {
      this.owner = entity;
   }

   public void tick() {
      this.xo = this.getX();
      this.yo = this.getY();
      this.zo = this.getZ();
      this.setPos(this.getX() + this.getDeltaMovement().x, this.getY(), this.getZ());
      this.setPos(this.getX(), this.getY() + this.getDeltaMovement().y, this.getZ());
      this.setPos(this.getX(), this.getY(), this.getZ() + this.getDeltaMovement().z);
      Vector3 start = new Vector3(this.xo, this.yo, this.zo);
      Vector3 end = new Vector3(this.getX(), this.getY(), this.getZ());
      Level world = this.level();
      HitResult hit = WorldQueries.clip(world, start.toVec3(), end.toVec3(), true);
      if (hit != null) {
         end.set(hit.getLocation());
         this.setPos(hit.getLocation().x, this.getY(), this.getZ());
         this.setPos(this.getX(), hit.getLocation().y, this.getZ());
         this.setPos(this.getX(), this.getY(), hit.getLocation().z);
      }

      List<Entity> entitiesToCheck = world.getEntities(
         this, new AABB(this.xo, this.yo, this.zo, this.getX(), this.getY(), this.getZ()).inflate(this.influenceSize)
      );
      List<HitResult> entitiesInfluences = new ArrayList<>();
      double minDistanceSq = start.distanceSquared(end);

      for (Entity entity : entitiesToCheck) {
         if (entity != this.owner && entity.isPickable()) {
            HitResult entityInfluence = WorldQueries.intersect(entity.getBoundingBox().inflate(this.influenceSize), start.toVec3(), end.toVec3());
            if (entityInfluence != null) {
               entitiesInfluences.add(entityInfluence);
               HitResult entityHit = WorldQueries.intersect(entity.getBoundingBox().inflate(this.coreSize), start.toVec3(), end.toVec3());
               if (entityHit != null) {
                  double distanceSq = start.distanceSquared(entityHit.getLocation());
                  if (distanceSq < minDistanceSq) {
                     hit = entityHit;
                     minDistanceSq = distanceSq;
                  }
               }
            }
         }
      }

      double maxInfluenceDistance = Math.sqrt(minDistanceSq) + this.influenceSize;

      for (HitResult entityInfluence : entitiesInfluences) {
         if (start.distance(entityInfluence.getLocation()) <= maxInfluenceDistance) {
            this.onInfluence(entityInfluence);
         }
      }

      if (this.radialTestVectors != null) {
         Vector3 vForward = end.copy().sub(start);
         double len = vForward.length();
         vForward.scale(1.0 / len);
         Vector3 origin = new Vector3(start);
         Vector3 tmp = new Vector3();

         for (int d = 0; d < len; d++) {
            for (Vector3 radialTestVector : this.radialTestVectors) {
               origin.copy(tmp).add(radialTestVector);
               HitResult influence = WorldQueries.clip(world, origin.toVec3(), tmp.toVec3(), true);
               if (influence != null) {
                  this.onInfluence(influence);
               }
            }

            origin.add(vForward);
         }
      }

      if (hit != null) {
         this.onImpact(hit);
         this.discard();
      } else {
         this.lifeTime--;
         if (this.lifeTime <= 0) {
            this.discard();
         }
      }
   }

   protected void onImpact(HitResult hit) {
      if (IC2.platform.isSimulating()) {
         System.out.println("hit " + hit.getType() + " " + hit.getLocation() + " sim=" + IC2.platform.isSimulating());
         if (hit.getType() == Type.BLOCK && IC2.platform.isSimulating()) {
         }

         ExplosionIC2 explosion = new ExplosionIC2(
            this.level(), this.owner, hit.getLocation().x, hit.getLocation().y, hit.getLocation().z, 18.0F, 0.95F, ExplosionIC2.Type.Heat
         );
         explosion.doExplosion();
      }
   }

   protected void onInfluence(HitResult hit) {
      if (IC2.platform.isSimulating()) {
         System.out.println("influenced " + hit.getType() + " " + hit.getLocation() + " sim=" + IC2.platform.isSimulating());
         if (hit.getType() == Type.BLOCK && IC2.platform.isSimulating()) {
            Level world = this.level();
            BlockState state = world.getBlockState(((BlockHitResult)hit).getBlockPos());
            Block block = state.getBlock();
            if (block != Blocks.WATER && block != Blocks.WATER) {
               List<ItemStack> drops = StackUtil.getDrops(world, ((BlockHitResult)hit).getBlockPos(), state, null, 0, true);
               if (drops.size() == 1 && StackUtil.getSize(drops.get(0)) == 1) {
                  ItemStack existing = drops.get(0);
                  ItemStack smelted = NativeSmelting.result(world, existing);
                  if (smelted != null && smelted.getItem() instanceof BlockItem) {
                     world.setBlock(((BlockHitResult)hit).getBlockPos(), ((BlockItem)smelted.getItem()).getBlock().defaultBlockState(), 3);
                  } else if (block.isFlammable(
                     world.getBlockState(((BlockHitResult)hit).getBlockPos()), world, ((BlockHitResult)hit).getBlockPos(), ((BlockHitResult)hit).getDirection()
                  )) {
                     world.setBlock(
                        ((BlockHitResult)hit).getBlockPos().relative(((BlockHitResult)hit).getDirection().getOpposite()), Blocks.FIRE.defaultBlockState(), 3
                     );
                  }
               }
            } else {
               world.removeBlock(((BlockHitResult)hit).getBlockPos(), false);
            }
         }
      }
   }
}
