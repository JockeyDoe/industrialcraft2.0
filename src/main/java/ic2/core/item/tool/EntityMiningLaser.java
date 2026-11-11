package ic2.core.item.tool;

import ic2.api.event.LaserEvent;
import ic2.core.ExplosionIC2;
import ic2.core.IC2;
import ic2.core.Ic2Player;
import ic2.core.block.MaterialIC2TNT;
import ic2.core.entity.Ic2Entity;
import ic2.core.entity.Ic2EntityTypes;
import ic2.core.recipe.NativeSmelting;
import ic2.core.ref.BlockMaterial;
import ic2.core.ref.BlockName;
import ic2.core.util.StackUtil;
import ic2.core.util.Vector3;
import ic2.core.util.WorldQueries;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.Explosion.BlockInteraction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

public class EntityMiningLaser extends Ic2Entity {
   public float range = 0.0F;
   public float power = 0.0F;
   public int blockBreaks = 0;
   public boolean explosive = false;
   public static final double laserSpeed = 1.0;
   public LivingEntity owner;
   public boolean headingSet = false;
   public boolean smelt = false;
   private int ticksInAir = 0;

   public EntityMiningLaser(Level world) {
      super(Ic2EntityTypes.get("mining_laser"), world);
      this.func_70105_a(0.8F, 0.8F);
   }

   public EntityMiningLaser(Level world, Vector3 start, Vector3 dir, LivingEntity owner, float range, float power, int blockBreaks, boolean explosive) {
      super(Ic2EntityTypes.get("mining_laser"), world);
      this.owner = owner;
      this.func_70105_a(0.8F, 0.8F);
      this.setPos(start.x, start.y, start.z);
      this.setLaserHeading(dir.x, dir.y, dir.z, 1.0);
      this.range = range;
      this.power = power;
      this.blockBreaks = blockBreaks;
      this.explosive = explosive;
   }

   @Override
   protected void func_70088_a() {
   }

   public void setLaserHeading(double motionX, double motionY, double motionZ, double speed) {
      double currentSpeed = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
      this.setDeltaMovement(new Vec3(motionX / currentSpeed * speed, this.getDeltaMovement().y, this.getDeltaMovement().z));
      this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, motionY / currentSpeed * speed, this.getDeltaMovement().z));
      this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, this.getDeltaMovement().y, motionZ / currentSpeed * speed));
      this.yRotO = (float)Math.toDegrees(Math.atan2(motionX, motionZ));
      this.setYRot(this.yRotO);
      this.xRotO = (float)Math.toDegrees(Math.atan2(motionY, Math.sqrt(motionX * motionX + motionZ * motionZ)));
      this.setXRot(this.xRotO);
      this.headingSet = true;
   }

   public void setDeltaMovement(double motionX, double motionY, double motionZ) {
      this.setLaserHeading(motionX, motionY, motionZ, 1.0);
   }

   public void tick() {
      super.tick();
      if (!IC2.platform.isSimulating() || !(this.range < 1.0F) && !(this.power <= 0.0F) && this.blockBreaks > 0) {
         this.ticksInAir++;
         Vec3 oldPosition = new Vec3(this.getX(), this.getY(), this.getZ());
         Vec3 newPosition = new Vec3(this.getX() + this.getDeltaMovement().x, this.getY() + this.getDeltaMovement().y, this.getZ() + this.getDeltaMovement().z);
         Level world = this.level();
         HitResult result = WorldQueries.clip(world, oldPosition, newPosition, false);
         oldPosition = new Vec3(this.getX(), this.getY(), this.getZ());
         if (result != null) {
            newPosition = new Vec3(result.getLocation().x, result.getLocation().y, result.getLocation().z);
         } else {
            newPosition = new Vec3(this.getX() + this.getDeltaMovement().x, this.getY() + this.getDeltaMovement().y, this.getZ() + this.getDeltaMovement().z);
         }

         Entity hitEntity = null;
         List<Entity> list = world.getEntities(
            this, this.getBoundingBox().expandTowards(this.getDeltaMovement().x, this.getDeltaMovement().y, this.getDeltaMovement().z).inflate(1.0)
         );
         double distance = 0.0;

         for (Entity entity : list) {
            if (entity.isPickable() && (entity != this.owner || this.ticksInAir >= 5)) {
               AABB hitBox = entity.getBoundingBox().inflate(0.3);
               HitResult intercept = WorldQueries.intersect(hitBox, oldPosition, newPosition);
               if (intercept != null) {
                  double newDistance = oldPosition.distanceTo(intercept.getLocation());
                  if (newDistance < distance || distance == 0.0) {
                     hitEntity = entity;
                     distance = newDistance;
                  }
               }
            }
         }

         HitResult blockHit = result;
         if (hitEntity != null) {
            result = new EntityHitResult(hitEntity);
         }

         if (result != null && result.getType() != Type.MISS && !world.isClientSide()) {
            if (this.explosive) {
               this.explode();
               this.discard();
               return;
            }

            switch (result.getType()) {
               case ENTITY:
                  if (this.hitEntity(((EntityHitResult)result).getEntity())) {
                     break;
                  }

                  if (blockHit == null) {
                     this.power -= 0.5F;
                     break;
                  } else {
                     result = blockHit;
                  }
               case BLOCK:
                  if (!this.hitBlock(((BlockHitResult)result).getBlockPos(), ((BlockHitResult)result).getDirection())) {
                     this.power -= 0.5F;
                  }
                  break;
               default:
                  throw new RuntimeException("invalid hit type: " + result.getType());
            }
         } else {
            this.power -= 0.5F;
         }

         this.setPos(this.getX() + this.getDeltaMovement().x, this.getY() + this.getDeltaMovement().y, this.getZ() + this.getDeltaMovement().z);
         this.range = (float)(
            this.range
               - Math.sqrt(
                  this.getDeltaMovement().x * this.getDeltaMovement().x
                     + this.getDeltaMovement().y * this.getDeltaMovement().y
                     + this.getDeltaMovement().z * this.getDeltaMovement().z
               )
         );
         if (this.isInWater()) {
            this.discard();
         }
      } else {
         if (this.explosive) {
            this.explode();
         }

         this.discard();
      }
   }

   private void explode() {
      Level world = this.level();
      LaserEvent.LaserExplodesEvent event = new LaserEvent.LaserExplodesEvent(
         world, this, this.owner, this.range, this.power, this.blockBreaks, this.explosive, this.smelt, 5.0F, 0.85F, 0.55F
      );
      if (((LaserEvent.LaserExplodesEvent)NeoForge.EVENT_BUS.post(event)).isCanceled()) {
         this.discard();
      } else {
         this.copyDataFromEvent(event);
         ExplosionIC2 explosion = new ExplosionIC2(world, this, this.getX(), this.getY(), this.getZ(), event.explosionPower, event.explosionDropRate);
         explosion.doExplosion();
      }
   }

   private Holder<DamageType> worldRegistryDamage() {
      return this.level()
         .registryAccess()
         .lookupOrThrow(Registries.DAMAGE_TYPE)
         .getOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, IC2.getIdentifier("mining_laser")));
   }

   private boolean hitEntity(Entity entity) {
      LaserEvent.LaserHitsEntityEvent event = new LaserEvent.LaserHitsEntityEvent(
         this.level(), this, this.owner, this.range, this.power, this.blockBreaks, this.explosive, this.smelt, entity
      );
      if (((LaserEvent.LaserHitsEntityEvent)NeoForge.EVENT_BUS.post(event)).isCanceled()) {
         if (event.passThrough) {
            return false;
         } else {
            this.discard();
            return true;
         }
      } else {
         this.copyDataFromEvent(event);
         entity = event.hitEntity;
         int damage = (int)this.power;
         if (damage > 0) {
            entity.igniteForSeconds(damage * (this.smelt ? 2 : 1));
            if (WorldQueries.hurt(entity, new DamageSource(this.worldRegistryDamage(), this, this.owner), damage)
               && (
                  this.owner instanceof Player && entity instanceof EnderDragon && ((EnderDragon)entity).getHealth() <= 0.0F
                     || entity instanceof PartEntity
                        && ((PartEntity)entity).getParent() instanceof EnderDragon
                        && ((LivingEntity)((PartEntity)entity).getParent()).getHealth() <= 0.0F
               )) {
               IC2.achievements.issueAchievement((Player)this.owner, "killDragonMiningLaser");
            }
         }

         this.discard();
         return true;
      }
   }

   private boolean hitBlock(BlockPos pos, Direction side) {
      Level world = this.level();
      LaserEvent.LaserHitsBlockEvent event = new LaserEvent.LaserHitsBlockEvent(
         world, this, this.owner, this.range, this.power, this.blockBreaks, this.explosive, this.smelt, pos, side, 0.9F, true, true
      );
      if (((LaserEvent.LaserHitsBlockEvent)NeoForge.EVENT_BUS.post(event)).isCanceled()) {
         this.discard();
         return true;
      } else {
         this.copyDataFromEvent(event);
         BlockState state = world.getBlockState(event.pos);
         Block block = state.getBlock();
         Player playerOwner = this.owner instanceof Player ? (Player)this.owner : Ic2Player.get(world);
         if (((BreakBlockEvent)NeoForge.EVENT_BUS.post(new BreakBlockEvent(world, event.pos, state, playerOwner))).isCanceled()) {
            this.discard();
            return true;
         } else if (state.isAir() || block == Blocks.GLASS || block == Blocks.GLASS_PANE || block == BlockName.glass.getInstance()) {
            return false;
         } else if (world.isClientSide()) {
            return true;
         } else {
            float hardness = state.getDestroySpeed(world, event.pos);
            if (hardness < 0.0F) {
               this.discard();
               return true;
            } else {
               this.power -= hardness / 1.5F;
               if (this.power < 0.0F) {
                  return true;
               } else {
                  List<ItemStack> replacements = new ArrayList<>();
                  if (BlockMaterial.of(state) == BlockMaterial.TNT || BlockMaterial.of(state) == MaterialIC2TNT.instance) {
                     block.onBlockExploded(
                        state,
                        (ServerLevel)world,
                        event.pos,
                        new ServerExplosion((ServerLevel)world, this, null, null, Vec3.atCenterOf(event.pos), 1.0F, false, BlockInteraction.DESTROY)
                     );
                  } else if (this.smelt) {
                     if (BlockMaterial.of(state) == BlockMaterial.WOOD) {
                        event.dropBlock = false;
                     } else {
                        for (ItemStack isa : StackUtil.getDrops(world, event.pos, state, block, 0)) {
                           ItemStack is = NativeSmelting.result(world, isa);
                           if (!StackUtil.isEmpty(is)) {
                              replacements.add(is);
                           }
                        }

                        event.dropBlock = replacements.isEmpty();
                     }
                  }

                  if (event.removeBlock) {
                     if (event.dropBlock) {
                        for (ItemStack drop : StackUtil.getDrops(world, event.pos, state, block, 0)) {
                           if (world.getRandom().nextFloat() <= event.dropChance) {
                              StackUtil.dropAsEntity(world, event.pos, drop);
                           }
                        }
                     }

                     world.removeBlock(event.pos, false);

                     for (ItemStack replacement : replacements) {
                        if (!StackUtil.placeBlock(replacement, world, event.pos)) {
                           StackUtil.dropAsEntity(world, event.pos, replacement);
                        }

                        this.power = 0.0F;
                     }

                     if (world.getRandom().nextInt(10) == 0 && BlockMaterial.of(state).isFlammable()) {
                        world.setBlock(event.pos, Blocks.FIRE.defaultBlockState(), 3);
                     }
                  }

                  this.blockBreaks--;
                  return true;
               }
            }
         }
      }
   }

   @Override
   public void func_70014_b(CompoundTag nbttagcompound) {
   }

   @Override
   public void func_70037_a(CompoundTag nbttagcompound) {
   }

   void copyDataFromEvent(LaserEvent event) {
      this.owner = event.owner;
      this.range = event.range;
      this.power = event.power;
      this.blockBreaks = event.blockBreaks;
      this.explosive = event.explosive;
      this.smelt = event.smelt;
   }

   public Entity getThrower() {
      return this.owner;
   }

   public void setThrower(Entity entity) {
      if (entity instanceof LivingEntity) {
         this.owner = (LivingEntity)entity;
      }
   }
}
