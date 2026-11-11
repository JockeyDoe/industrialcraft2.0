package ic2.core.block.machine.tileentity;

import ic2.api.network.INetworkTileEntityEventListener;
import ic2.api.tile.IEnergyStorage;
import ic2.core.IC2;
import ic2.core.audio.AudioPosition;
import ic2.core.audio.AudioSource;
import ic2.core.audio.PositionSpec;
import ic2.core.block.TileEntityBlock;
import ic2.core.block.comp.ComparatorEmitter;
import ic2.core.init.MainConfig;
import ic2.core.util.ConfigUtil;
import ic2.core.util.StackUtil;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class TileEntityTeleporter extends TileEntityBlock implements INetworkTileEntityEventListener {
   private BlockPos target;
   private AudioSource audioSource = null;
   private int targetCheckTicker = IC2.random.nextInt(1024);
   private int cooldown = 0;
   protected final ComparatorEmitter comparator = this.addComponent(new ComparatorEmitter(this));
   private static final int EventTeleport = 0;

   @Override
   public void loadAdditional(CompoundTag nbt) {
      super.loadAdditional(nbt);
      if (nbt.contains("targetX")) {
         this.target = new BlockPos(nbt.getIntOr("targetX", 0), nbt.getIntOr("targetY", 0), nbt.getIntOr("targetZ", 0));
      }
   }

   @Override
   public CompoundTag saveAdditional(CompoundTag nbt) {
      super.saveAdditional(nbt);
      if (this.target != null) {
         nbt.putInt("targetX", this.target.getX());
         nbt.putInt("targetY", this.target.getY());
         nbt.putInt("targetZ", this.target.getZ());
      }

      return nbt;
   }

   @Override
   protected void onUnloaded() {
      if (IC2.platform.isRendering() && this.audioSource != null) {
         IC2.audioManager.removeSources(this);
         this.audioSource = null;
      }

      super.onUnloaded();
   }

   @Override
   protected void onLoaded() {
      super.onLoaded();
      this.updateComparatorLevel();
   }

   @Override
   protected void updateEntityServer() {
      super.updateEntityServer();
      boolean coolingDown = this.cooldown > 0;
      if (coolingDown) {
         this.cooldown--;
         IC2.network.get(true).updateTileEntityField(this, "cooldown");
      }

      Level world = this.getLevel();
      if (world.hasNeighborSignal(this.getBlockPos()) && this.target != null) {
         this.setActive(true);
         List<Entity> entitiesNearby;
         if (coolingDown) {
            entitiesNearby = Collections.emptyList();
         } else {
            entitiesNearby = world.getEntitiesOfClass(
               Entity.class,
               new AABB(
                  this.getBlockPos().getX() - 1,
                  this.getBlockPos().getY(),
                  this.getBlockPos().getZ() - 1,
                  this.getBlockPos().getX() + 2,
                  this.getBlockPos().getY() + 3,
                  this.getBlockPos().getZ() + 2
               )
            );
         }

         if (!entitiesNearby.isEmpty() && this.verifyTarget()) {
            double minDistanceSquared = Double.MAX_VALUE;
            Entity closestEntity = null;

            for (Entity entity : entitiesNearby) {
               if (entity.getVehicle() == null) {
                  double distSquared = Vec3.atLowerCornerOf(this.getBlockPos()).distanceToSqr(entity.position());
                  if (distSquared < minDistanceSquared) {
                     minDistanceSquared = distSquared;
                     closestEntity = entity;
                  }
               }
            }

            assert closestEntity != null;

            this.teleport(closestEntity, Math.sqrt(this.getBlockPos().distSqr(this.target)));
         } else if (++this.targetCheckTicker % 1024 == 0) {
            this.verifyTarget();
         }
      } else {
         this.setActive(false);
      }
   }

   private boolean verifyTarget() {
      if (this.getLevel().getBlockEntity(this.target) instanceof TileEntityTeleporter) {
         return true;
      } else {
         this.target = null;
         this.updateComparatorLevel();
         this.setActive(false);
         return false;
      }
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   protected void updateEntityClient() {
      super.updateEntityClient();
      if (this.getActive()) {
         if (this.cooldown > 0) {
            this.spawnGreenParticles(2, this.getBlockPos());
         } else {
            this.spawnBlueParticles(2, this.getBlockPos());
         }
      }
   }

   private void updateComparatorLevel() {
      int targetLevel = this.target != null ? 15 : 0;
      this.comparator.setLevel(targetLevel);
   }

   public void teleport(Entity user, double distance) {
      int weight = this.getWeightOf(user);
      if (weight != 0) {
         int energyCost = (int)(weight * Math.pow(distance + 10.0, 0.7) * 5.0);
         if (energyCost <= this.getAvailableEnergy()) {
            this.consumeEnergy(energyCost);
            if (user instanceof ServerPlayer) {
               ((ServerPlayer)user).teleportTo(this.target.getX() + 0.5, this.target.getY() + 1.5 + 0.0, this.target.getZ() + 0.5);
            } else {
               user.snapTo(this.target.getX() + 0.5, this.target.getY() + 1.5 + 0.0, this.target.getZ() + 0.5, user.getYRot(), user.getXRot());
            }

            BlockEntity te = this.getLevel().getBlockEntity(this.target);

            assert te instanceof TileEntityTeleporter;

            ((TileEntityTeleporter)te).onTeleportTo(this, user);
            IC2.network.get(true).initiateTileEntityEvent(this, 0, true);
            if (user instanceof Player && distance >= 1000.0) {
               IC2.achievements.issueAchievement((Player)user, "teleportFarAway");
            }
         }
      }
   }

   public void spawnBlueParticles(int n, BlockPos pos) {
      this.spawnParticles(n, pos, -1, 0, 1);
   }

   public void spawnGreenParticles(int n, BlockPos pos) {
      this.spawnParticles(n, pos, -1, 1, 0);
   }

   private void spawnParticles(int n, BlockPos pos, int red, int green, int blue) {
      Level world = this.getLevel();
      RandomSource rnd = world.getRandom();

      for (int i = 0; i < n; i++) {
         world.addParticle(
            new DustParticleOptions(
               ARGB.color(255, Math.max(0, Math.min(255, red * 255)), Math.max(0, Math.min(255, green * 255)), Math.max(0, Math.min(255, blue * 255))), 1.0F
            ),
            pos.getX() + rnd.nextFloat(),
            pos.getY() + 1 + rnd.nextFloat(),
            pos.getZ() + rnd.nextFloat(),
            0.0,
            0.0,
            0.0
         );
         world.addParticle(
            new DustParticleOptions(
               ARGB.color(255, Math.max(0, Math.min(255, red * 255)), Math.max(0, Math.min(255, green * 255)), Math.max(0, Math.min(255, blue * 255))), 1.0F
            ),
            pos.getX() + rnd.nextFloat(),
            pos.getY() + 2 + rnd.nextFloat(),
            pos.getZ() + rnd.nextFloat(),
            0.0,
            0.0,
            0.0
         );
      }
   }

   public void consumeEnergy(int energy) {
      Level world = this.getLevel();
      List<IEnergyStorage> energySources = new LinkedList<>();

      for (Direction dir : Direction.values()) {
         if (world.getBlockEntity(this.getBlockPos().relative(dir)) instanceof IEnergyStorage energySource
            && energySource.isTeleporterCompatible(dir.getOpposite())
            && energySource.getStored() > 0) {
            energySources.add(energySource);
         }
      }

      while (energy > 0) {
         int drain = (energy + energySources.size() - 1) / energySources.size();
         Iterator<IEnergyStorage> it = energySources.iterator();

         while (it.hasNext()) {
            IEnergyStorage energySource = it.next();
            if (drain > energy) {
               drain = energy;
            }

            if (energySource.getStored() <= drain) {
               energy -= energySource.getStored();
               energySource.setStored(0);
               it.remove();
            } else {
               energy -= drain;
               energySource.addEnergy(-drain);
            }
         }
      }
   }

   public int getAvailableEnergy() {
      Level world = this.getLevel();
      int energy = 0;

      for (Direction dir : Direction.values()) {
         if (world.getBlockEntity(this.getBlockPos().relative(dir)) instanceof IEnergyStorage storage && storage.isTeleporterCompatible(dir.getOpposite())) {
            energy += storage.getStored();
         }
      }

      return energy;
   }

   public int getWeightOf(Entity user) {
      boolean teleporterUseInventoryWeight = ConfigUtil.getBool(MainConfig.get(), "balance/teleporterUseInventoryWeight");
      int weight = 0;
      if (user instanceof ItemEntity) {
         ItemStack is = ((ItemEntity)user).getItem();
         weight += 100 * StackUtil.getSize(is) / is.getMaxStackSize();
      } else if (user instanceof Animal || user instanceof AbstractMinecart || user instanceof Boat) {
         weight += 100;
      } else if (user instanceof Player) {
         weight += 1000;
         if (teleporterUseInventoryWeight) {
            for (ItemStack stack : ((Player)user).getInventory().getNonEquipmentItems()) {
               weight += getStackCost(stack);
            }
         }
      } else if (user instanceof Ghast) {
         weight += 2500;
      } else if (user instanceof WitherBoss) {
         weight += 5000;
      } else if (user instanceof EnderDragon) {
         weight += 10000;
      } else if (user instanceof PathfinderMob) {
         weight += 500;
      }

      if (teleporterUseInventoryWeight && user instanceof LivingEntity living) {
         for (ItemStack stack : List.of(
            living.getMainHandItem(),
            living.getOffhandItem(),
            living.getItemBySlot(EquipmentSlot.FEET),
            living.getItemBySlot(EquipmentSlot.LEGS),
            living.getItemBySlot(EquipmentSlot.CHEST),
            living.getItemBySlot(EquipmentSlot.HEAD)
         )) {
            weight += getStackCost(stack);
         }

         if (user instanceof Player) {
            ItemStack stack = living.getMainHandItem();
            weight -= getStackCost(stack);
         }
      }

      for (Entity passenger : user.getPassengers()) {
         weight += this.getWeightOf(passenger);
      }

      return weight;
   }

   private static int getStackCost(ItemStack stack) {
      return StackUtil.isEmpty(stack) ? 0 : 100 * StackUtil.getSize(stack) / stack.getMaxStackSize();
   }

   private void onTeleportTo(TileEntityTeleporter from, Entity entity) {
      this.cooldown = 20;
   }

   @Override
   protected boolean canEntityDestroy(Entity entity) {
      return !(entity instanceof EnderDragon) && !(entity instanceof WitherBoss);
   }

   public boolean hasTarget() {
      return this.target != null;
   }

   public BlockPos getTarget() {
      return this.target;
   }

   public void setTarget(BlockPos pos) {
      this.target = pos;
      this.updateComparatorLevel();
      IC2.network.get(true).updateTileEntityField(this, "target");
   }

   @Override
   public List<String> getNetworkedFields() {
      List<String> ret = super.getNetworkedFields();
      ret.add("target");
      return ret;
   }

   @Override
   public void onNetworkUpdate(String field) {
      if (field.equals("active")) {
         if (this.audioSource == null) {
            this.audioSource = IC2.audioManager
               .createSource(this, PositionSpec.Center, "Machines/Teleporter/TeleChargedLoop.ogg", true, false, IC2.audioManager.getDefaultVolume());
         }

         if (this.getActive()) {
            if (this.audioSource != null) {
               this.audioSource.play();
            }
         } else if (this.audioSource != null) {
            this.audioSource.stop();
         }
      }

      super.onNetworkUpdate(field);
   }

   @Override
   public void onNetworkEvent(int event) {
      switch (event) {
         case 0:
            IC2.audioManager.playOnce(this, "Machines/Teleporter/TeleUse.ogg");
            IC2.audioManager.playOnce(new AudioPosition(this.getLevel(), this.getBlockPos()), "Machines/Teleporter/TeleUse.ogg");
            this.spawnBlueParticles(20, this.getBlockPos());
            this.spawnBlueParticles(20, this.target);
            break;
         default:
            IC2.platform
               .displayError(
                  "An unknown event type was received over multiplayer.\nThis could happen due to corrupted data or a bug.\n\n(Technical information: event ID "
                     + event
                     + ", tile entity below)\nT: "
                     + this
                     + " ("
                     + this.getBlockPos()
                     + ")"
               );
      }
   }
}
