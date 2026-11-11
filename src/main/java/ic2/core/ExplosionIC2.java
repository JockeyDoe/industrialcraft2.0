package ic2.core;

import ic2.api.event.ExplosionEvent;
import ic2.api.tile.ExplosionWhitelist;
import ic2.core.item.armor.ItemArmorHazmat;
import ic2.core.util.ItemComparableItemStack;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.Explosion.BlockInteraction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

public class ExplosionIC2 implements Explosion {
   private final Level worldObj;
   private final Entity exploder;
   private final double explosionX;
   private final double explosionY;
   private final double explosionZ;
   private final int mapHeight;
   private final float power;
   private final float explosionDropRate;
   private final ExplosionIC2.Type type;
   private final int radiationRange;
   private final LivingEntity igniter;
   private final Random rng = new Random();
   private final double maxDistance;
   private final int areaSize;
   private final int areaX;
   private final int areaZ;
   private final DamageSource damageSource;
   private final List<ExplosionIC2.EntityDamage> entitiesInRange = new ArrayList<>();
   private final long[][] destroyedBlockPositions;
   private PathNavigationRegion chunkCache;
   private static final double dropPowerLimit = 8.0;
   private static final double damageAtDropPowerLimit = 32.0;
   private static final double accelerationAtDropPowerLimit = 0.7;
   private static final double motionLimit = 60.0;
   private static final int secondaryRayCount = 5;
   private static final int bitSetElementSize = 2;

   public ExplosionIC2(Level world, Entity entity, double x, double y, double z, float power, float drop) {
      this(world, entity, x, y, z, power, drop, ExplosionIC2.Type.Normal);
   }

   public ExplosionIC2(Level world, Entity entity, double x, double y, double z, float power, float drop, ExplosionIC2.Type type) {
      this(world, entity, x, y, z, power, drop, type, null, 0);
   }

   public ExplosionIC2(Level world, Entity entity, BlockPos pos, float power, float drop, ExplosionIC2.Type type) {
      this(world, entity, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, power, drop, type);
   }

   public ExplosionIC2(
      Level world, Entity entity, double x, double y, double z, float power1, float drop, ExplosionIC2.Type type1, LivingEntity igniter1, int radiationRange1
   ) {
      this.worldObj = world;
      this.exploder = entity;
      this.explosionX = x;
      this.explosionY = y;
      this.explosionZ = z;
      this.mapHeight = IC2.getWorldHeight(world);
      this.power = power1;
      this.explosionDropRate = drop;
      this.type = type1;
      this.igniter = igniter1;
      this.radiationRange = radiationRange1;
      this.maxDistance = this.power / 0.4;
      int maxDistanceInt = (int)Math.ceil(this.maxDistance);
      this.areaSize = maxDistanceInt * 2;
      this.areaX = Util.roundToNegInf(x) - maxDistanceInt;
      this.areaZ = Util.roundToNegInf(z) - maxDistanceInt;
      if (this.isNuclear()) {
         this.damageSource = IC2DamageSource.getNukeSource(this);
      } else {
         this.damageSource = world.damageSources().explosion(this);
      }

      this.destroyedBlockPositions = new long[this.mapHeight][];
   }

   public ExplosionIC2(Level world, Entity entity, BlockPos pos, int i, float f, ExplosionIC2.Type heat) {
      this(world, entity, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, i, f, heat);
   }

   public void doExplosion() {
      if (!(this.power <= 0.0F)) {
         ExplosionEvent event = new ExplosionEvent(
            this.worldObj, this.exploder, this.getPosition(), this.power, this.igniter, this.radiationRange, this.maxDistance
         );
         if (!((ExplosionEvent)NeoForge.EVENT_BUS.post(event)).isCanceled()) {
            int range = this.areaSize / 2;
            BlockPos pos = BlockPos.containing(this.getPosition());
            BlockPos start = pos.offset(-range, -range, -range);
            BlockPos end = pos.offset(range, range, range);
            this.chunkCache = new PathNavigationRegion(this.worldObj, start, end);

            for (Entity entity : this.worldObj.getEntities(null, AABB.encapsulatingFullBlocks(start, end))) {
               if (entity instanceof LivingEntity || entity instanceof ItemEntity) {
                  int distance = (int)(
                     Util.square(entity.getX() - this.explosionX) + Util.square(entity.getY() - this.explosionY) + Util.square(entity.getZ() - this.explosionZ)
                  );
                  double health = getEntityHealth(entity);
                  this.entitiesInRange.add(new ExplosionIC2.EntityDamage(entity, distance, health));
               }
            }

            boolean entitiesAreInRange = !this.entitiesInRange.isEmpty();
            if (entitiesAreInRange) {
               Collections.sort(this.entitiesInRange, new Comparator<ExplosionIC2.EntityDamage>() {
                  {
                     Objects.requireNonNull(ExplosionIC2.this);
                  }

                  public int compare(ExplosionIC2.EntityDamage a, ExplosionIC2.EntityDamage b) {
                     return a.distance - b.distance;
                  }
               });
            }

            int steps = (int)Math.ceil(Math.PI / Math.atan(1.0 / this.maxDistance));
            MutableBlockPos tmpPos = new MutableBlockPos();

            for (int phi_n = 0; phi_n < 2 * steps; phi_n++) {
               for (int theta_n = 0; theta_n < steps; theta_n++) {
                  double phi = (Math.PI * 2) / steps * phi_n;
                  double theta = Math.PI / steps * theta_n;
                  this.shootRay(
                     this.explosionX,
                     this.explosionY,
                     this.explosionZ,
                     phi,
                     theta,
                     this.power,
                     entitiesAreInRange && phi_n % 8 == 0 && theta_n % 8 == 0,
                     tmpPos
                  );
               }
            }

            for (ExplosionIC2.EntityDamage entry : this.entitiesInRange) {
               Entity entityx = entry.entity;
               entityx.hurtServer(this.level(), this.damageSource, (float)entry.damage);
               if (entityx instanceof Player player && this.isNuclear() && this.igniter != null && player == this.igniter && player.getHealth() <= 0.0F) {
                  IC2.achievements.issueAchievement(player, "dieFromOwnNuke");
               }

               double motionSq = Util.square(entry.motionX) + Util.square(entityx.getDeltaMovement().y) + Util.square(entityx.getDeltaMovement().z);
               double reduction = motionSq > 3600.0 ? Math.sqrt(3600.0 / motionSq) : 1.0;
               entityx.setDeltaMovement(
                  new Vec3(entityx.getDeltaMovement().x + entry.motionX * reduction, entityx.getDeltaMovement().y, entityx.getDeltaMovement().z)
               );
               entityx.setDeltaMovement(
                  new Vec3(entityx.getDeltaMovement().x, entityx.getDeltaMovement().y + entry.motionY * reduction, entityx.getDeltaMovement().z)
               );
               entityx.setDeltaMovement(
                  new Vec3(entityx.getDeltaMovement().x, entityx.getDeltaMovement().y, entityx.getDeltaMovement().z + entry.motionZ * reduction)
               );
            }

            if (this.isNuclear() && this.radiationRange >= 1) {
               for (Mob entityx : this.worldObj
                  .getEntitiesOfClass(
                     Mob.class,
                     new AABB(
                        this.explosionX - this.radiationRange,
                        this.explosionY - this.radiationRange,
                        this.explosionZ - this.radiationRange,
                        this.explosionX + this.radiationRange,
                        this.explosionY + this.radiationRange,
                        this.explosionZ + this.radiationRange
                     )
                  )) {
                  if (!ItemArmorHazmat.hasCompleteHazmat(entityx)) {
                     double distance = Math.sqrt(entityx.distanceToSqr(this.explosionX, this.explosionY, this.explosionZ));
                     int hungerLength = (int)(120.0 * (this.radiationRange - distance));
                     int poisonLength = (int)(80.0 * (this.radiationRange / 3 - distance));
                     if (hungerLength >= 0) {
                        entityx.addEffect(new MobEffectInstance(MobEffects.HUNGER, hungerLength, 0));
                     }

                     if (poisonLength >= 0) {
                        IC2Potion.radiation.applyTo(entityx, poisonLength, 0);
                     }
                  }
               }
            }

            IC2.network.get(true).initiateExplosionEffect(this.worldObj, this.getPosition(), this.type);
            RandomSource rng = this.worldObj.getRandom();
            boolean doDrops = (Boolean)this.level().getGameRules().get(GameRules.BLOCK_DROPS);
            Map<ExplosionIC2.XZposition, Map<ItemComparableItemStack, ExplosionIC2.DropData>> blocksToDrop = new HashMap<>();

            for (int y = 0; y < this.destroyedBlockPositions.length; y++) {
               long[] bitSet = this.destroyedBlockPositions[y];
               if (bitSet != null) {
                  int index = -2;

                  while ((index = nextSetIndex(index + 2, bitSet, 2)) != -1) {
                     int realIndex = index / 2;
                     int z = realIndex / this.areaSize;
                     int x = realIndex - z * this.areaSize;
                     x += this.areaX;
                     z += this.areaZ;
                     tmpPos.set(x, y, z);
                     BlockState state = this.chunkCache.getBlockState(tmpPos);
                     Block block = state.getBlock();
                     if (this.power < 20.0F) {
                     }

                     if (doDrops && block.dropFromExplosion(this) && getAtIndex(index, bitSet, 2) == 1) {
                        for (ItemStack stack : StackUtil.getDrops(this.worldObj, tmpPos, state, block, 0)) {
                           if (!(rng.nextFloat() > this.explosionDropRate)) {
                              ExplosionIC2.XZposition xZposition = new ExplosionIC2.XZposition(x / 2, z / 2);
                              Map<ItemComparableItemStack, ExplosionIC2.DropData> map = blocksToDrop.get(xZposition);
                              if (map == null) {
                                 map = new HashMap<>();
                                 blocksToDrop.put(xZposition, map);
                              }

                              ItemComparableItemStack isw = new ItemComparableItemStack(stack, false);
                              ExplosionIC2.DropData data = map.get(isw);
                              if (data == null) {
                                 data = new ExplosionIC2.DropData(StackUtil.getSize(stack), y);
                                 map.put(isw.copy(), data);
                              } else {
                                 data.add(StackUtil.getSize(stack), y);
                              }
                           }
                        }
                     }

                     state.onBlockExploded(this.level(), tmpPos, this);
                  }
               }
            }

            for (Entry<ExplosionIC2.XZposition, Map<ItemComparableItemStack, ExplosionIC2.DropData>> entry : blocksToDrop.entrySet()) {
               ExplosionIC2.XZposition xZpositionx = entry.getKey();

               for (Entry<ItemComparableItemStack, ExplosionIC2.DropData> entry2 : entry.getValue().entrySet()) {
                  ItemComparableItemStack isw = entry2.getKey();
                  int count = entry2.getValue().n;

                  while (count > 0) {
                     int stackSize = Math.min(count, 64);
                     ItemEntity entityitem = new ItemEntity(
                        this.worldObj,
                        (xZpositionx.x + this.worldObj.getRandom().nextFloat()) * 2.0F,
                        entry2.getValue().maxY + 0.5,
                        (xZpositionx.z + this.worldObj.getRandom().nextFloat()) * 2.0F,
                        isw.toStack(stackSize)
                     );
                     entityitem.setDefaultPickUpDelay();
                     this.worldObj.addFreshEntity(entityitem);
                     count -= stackSize;
                  }
               }
            }
         }
      }
   }

   public void destroy(int x, int y, int z, boolean noDrop) {
      this.destroyUnchecked(x, y, z, noDrop);
   }

   private void destroyUnchecked(int x, int y, int z, boolean noDrop) {
      int index = (z - this.areaZ) * this.areaSize + (x - this.areaX);
      index *= 2;
      long[] array = this.destroyedBlockPositions[y];
      if (array == null) {
         array = makeArray(Util.square(this.areaSize), 2);
         this.destroyedBlockPositions[y] = array;
      }

      if (noDrop) {
         setAtIndex(index, array, 3);
      } else {
         setAtIndex(index, array, 1);
      }
   }

   private void shootRay(double x, double y, double z, double phi, double theta, double power1, boolean killEntities, MutableBlockPos tmpPos) {
      double deltaX = Math.sin(theta) * Math.cos(phi);
      double deltaY = Math.cos(theta);
      double deltaZ = Math.sin(theta) * Math.sin(phi);
      int step = 0;

      while (true) {
         int blockY = Util.roundToNegInf(y);
         if (blockY < 0 || blockY >= this.mapHeight) {
            break;
         }

         int blockX = Util.roundToNegInf(x);
         int blockZ = Util.roundToNegInf(z);
         tmpPos.set(blockX, blockY, blockZ);
         BlockState state = this.chunkCache.getBlockState(tmpPos);
         Block block = state.getBlock();
         double absorption = this.getAbsorption(block, tmpPos);
         if (absorption < 0.0) {
            break;
         }

         if (absorption > 1000.0 && !ExplosionWhitelist.isBlockWhitelisted(block)) {
            absorption = 0.5;
         } else {
            if (absorption > power1) {
               break;
            }

            if (block == Blocks.STONE || block != Blocks.AIR && !state.isAir()) {
               this.destroyUnchecked(blockX, blockY, blockZ, power1 > 8.0);
            }
         }

         if (killEntities && (step + 4) % 8 == 0 && !this.entitiesInRange.isEmpty() && power1 >= 0.25) {
            this.damageEntities(x, y, z, step, power1);
         }

         if (absorption > 10.0) {
            for (int i = 0; i < 5; i++) {
               this.shootRay(x, y, z, this.rng.nextDouble() * 2.0 * Math.PI, this.rng.nextDouble() * Math.PI, absorption * 0.4, false, tmpPos);
            }
         }

         power1 -= absorption;
         x += deltaX;
         y += deltaY;
         z += deltaZ;
         step++;
      }
   }

   private double getAbsorption(Block block, BlockPos pos) {
      double ret = 0.5;
      if (block != Blocks.AIR && !block.defaultBlockState().isAir()) {
         if ((block == Blocks.WATER || block == Blocks.WATER) && this.type != ExplosionIC2.Type.Normal) {
            ret++;
         } else {
            float resistance = this.worldObj.getBlockState(pos).getExplosionResistance(this.worldObj, pos, this);
            if (resistance < 0.0F) {
               return resistance;
            }

            double extra = (resistance + 4.0F) * 0.3;
            if (this.type != ExplosionIC2.Type.Heat) {
               ret += extra;
            } else {
               ret += extra * 6.0;
            }
         }

         return ret;
      } else {
         return ret;
      }
   }

   private void damageEntities(double x, double y, double z, int step, double power) {
      int index;
      if (step != 4) {
         int distanceMin = Util.square(step - 5);
         int indexStart = 0;
         int indexEnd = this.entitiesInRange.size() - 1;

         do {
            index = (indexStart + indexEnd) / 2;
            int distance = this.entitiesInRange.get(index).distance;
            if (distance < distanceMin) {
               indexStart = index + 1;
            } else if (distance > distanceMin) {
               indexEnd = index - 1;
            } else {
               indexEnd = index;
            }
         } while (indexStart < indexEnd);
      } else {
         index = 0;
      }

      int distanceMax = Util.square(step + 5);

      for (int i = index; i < this.entitiesInRange.size(); i++) {
         ExplosionIC2.EntityDamage entry = this.entitiesInRange.get(i);
         if (entry.distance >= distanceMax) {
            break;
         }

         Entity entity = entry.entity;
         if (Util.square(entity.getX() - x) + Util.square(entity.getY() - y) + Util.square(entity.getZ() - z) <= 25.0) {
            double damage = 4.0 * power;
            entry.damage += damage;
            entry.health -= damage;
            double dx = entity.getX() - this.explosionX;
            double dy = entity.getY() - this.explosionY;
            double dz = entity.getZ() - this.explosionZ;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            entry.motionX += dx / distance * 0.0875 * power;
            entry.motionY += dy / distance * 0.0875 * power;
            entry.motionZ += dz / distance * 0.0875 * power;
            if (entry.health <= 0.0) {
               entity.hurtServer(this.level(), this.damageSource, (float)entry.damage);
               if (!entity.isAlive()) {
                  this.entitiesInRange.remove(i);
                  i--;
               }
            }
         }
      }
   }

   public ServerLevel level() {
      return (ServerLevel)this.worldObj;
   }

   public BlockInteraction getBlockInteraction() {
      return BlockInteraction.DESTROY;
   }

   public LivingEntity getIndirectSourceEntity() {
      return this.igniter != null ? this.igniter : Explosion.getIndirectSourceEntity(this.exploder);
   }

   public Entity getDirectSourceEntity() {
      return this.exploder;
   }

   public float radius() {
      return this.power;
   }

   public Vec3 center() {
      return new Vec3(this.explosionX, this.explosionY, this.explosionZ);
   }

   public Vec3 getPosition() {
      return this.center();
   }

   public boolean canTriggerBlocks() {
      return false;
   }

   public boolean shouldAffectBlocklikeEntities() {
      return true;
   }

   public LivingEntity func_94613_c() {
      return this.igniter;
   }

   private boolean isNuclear() {
      return this.type == ExplosionIC2.Type.Nuclear;
   }

   private static double getEntityHealth(Entity entity) {
      return entity instanceof ItemEntity ? 5.0 : Double.POSITIVE_INFINITY;
   }

   private static long[] makeArray(int size, int step) {
      return new long[(size * step + 8 - step) / 8];
   }

   private static int nextSetIndex(int start, long[] array, int step) {
      int offset = start % 8;

      for (int i = start / 8; i < array.length; i++) {
         long aval = array[i];
         int j = offset;

         while (j < 8) {
            int val = (int)(aval >> j & (1 << step) - 1);
            if (val != 0) {
               return i * 8 + j;
            }

            j += step;
         }

         offset = 0;
      }

      return -1;
   }

   private static int getAtIndex(int index, long[] array, int step) {
      return (int)(array[index / 8] >>> index % 8 & (1 << step) - 1);
   }

   private static void setAtIndex(int index, long[] array, int value) {
      array[index / 8] = array[index / 8] | value << index % 8;
   }

   private static class DropData {
      int n;
      int maxY;

      DropData(int n1, int y) {
         this.n = n1;
         this.maxY = y;
      }

      public ExplosionIC2.DropData add(int n1, int y) {
         this.n += n1;
         if (y > this.maxY) {
            this.maxY = y;
         }

         return this;
      }
   }

   private static class EntityDamage {
      final Entity entity;
      final int distance;
      double health;
      double damage;
      double motionX;
      double motionY;
      double motionZ;

      EntityDamage(Entity entity, int distance, double health) {
         this.entity = entity;
         this.distance = distance;
         this.health = health;
      }
   }

   public static enum Type {
      Normal,
      Heat,
      Electrical,
      Nuclear;
   }

   private static class XZposition {
      int x;
      int z;

      XZposition(int x1, int z1) {
         this.x = x1;
         this.z = z1;
      }

      @Override
      public boolean equals(Object obj) {
         return !(obj instanceof ExplosionIC2.XZposition xZposition) ? false : xZposition.x == this.x && xZposition.z == this.z;
      }

      @Override
      public int hashCode() {
         return this.x * 31 ^ this.z;
      }
   }
}
