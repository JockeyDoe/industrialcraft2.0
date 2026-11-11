package ic2.core.item;

import ic2.core.entity.Ic2EntityTypes;
import ic2.core.ref.BlockMaterial;
import ic2.core.ref.ItemName;
import ic2.core.util.ReflectionUtil;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundPaddleBoatPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat.Status;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public abstract class EntityIC2Boat extends Boat {
   private static Field field_paddlePositions;
   private static Field field_previousStatus;
   private static Field field_status;
   private static Field field_outOfControlTicks;
   private static Field field_lastYd;
   private static Field field_waterLevel;
   private static Field field_boatGlide;
   private static Field field_deltaRotation;
   private static Field field_rightInputDown;
   private static Field field_leftInputDown;
   private static Field field_forwardInputDown;
   private static Field field_backInputDown;

   public static void init() {
      field_paddlePositions = getField("paddlePositions", "field_184470_f");
      field_previousStatus = getField("oldStatus", "field_184471_aG");
      field_status = getField("status", "field_184469_aF");
      field_outOfControlTicks = getField("outOfControlTicks", "field_184474_h");
      field_lastYd = getField("lastYdd", "field_184473_aH");
      field_waterLevel = getField("waterLevel", "field_184465_aD");
      field_boatGlide = getField("landFriction", "field_184467_aE");
      field_deltaRotation = getField("deltaRotation", "field_184475_as");
      field_rightInputDown = getField("inputRight", "field_184459_aA");
      field_leftInputDown = getField("inputLeft", "field_184480_az");
      field_forwardInputDown = getField("inputUp", "field_184461_aB");
      field_backInputDown = getField("inputDown", "field_184463_aC");
   }

   private static Field getField(String deobfName, String srgName) {
      return ReflectionUtil.getField(AbstractBoat.class, deobfName);
   }

   private static Method getMethod(String deobfName, String srgName, Class<?>... parameterTypes) {
      return ReflectionUtil.getMethod(Boat.class, new String[]{srgName, deobfName}, parameterTypes);
   }

   public EntityIC2Boat(Level world, String type) {
      super(Ic2EntityTypes.get(type, Boat.class), world, () -> ItemName.boat.getInstance());
   }

   public void tick() {
      Level world = this.level();

      try {
         field_previousStatus.set(this, field_status.get(this));
         Status status = this.getBoatStatus();
         field_status.set(this, status);
         if (status != Status.UNDER_WATER && status != Status.UNDER_FLOWING_WATER) {
            field_outOfControlTicks.setFloat(this, 0.0F);
         } else {
            field_outOfControlTicks.setFloat(this, field_outOfControlTicks.getFloat(this) + 1.0F);
         }

         if (!world.isClientSide() && field_outOfControlTicks.getFloat(this) >= 60.0F) {
            this.ejectPassengers();
         }

         if (this.getHurtTime() > 0) {
            this.setHurtTime(this.getHurtTime() - 1);
         }

         if (this.getDamage() > 0.0F) {
            this.setDamage(this.getDamage() - 1.0F);
         }

         this.xo = this.getX();
         this.yo = this.getY();
         this.zo = this.getZ();
         this.doEntityUpdate(world);
         if (!this.canSimulateMovement()) {
            this.setDeltaMovement(new Vec3(0.0, this.getDeltaMovement().y, this.getDeltaMovement().z));
            this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, 0.0, this.getDeltaMovement().z));
            this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, this.getDeltaMovement().y, 0.0));
         } else {
            if (this.getPassengers().isEmpty() || !(this.getPassengers().get(0) instanceof Player)) {
               this.setPaddleState(false, false);
            }

            this.updateMotion();
            if (world.isClientSide()) {
               this.controlBoat();
               world.sendPacketToServer(new ServerboundPaddleBoatPacket(this.getPaddleState(0), this.getPaddleState(1)));
            }

            this.move(MoverType.SELF, this.getDeltaMovement());
         }

         for (int i = 0; i <= 1; i++) {
            if (this.getPaddleState(i)) {
               double paddlePosition = Array.getFloat(field_paddlePositions.get(this), i);
               if (!this.isSilent() && paddlePosition % (Math.PI * 2) <= Math.PI / 4 && (paddlePosition + 0.4) % (Math.PI * 2) >= Math.PI / 4) {
                  SoundEvent soundevent = this.getPaddleSound();
                  if (soundevent != null) {
                     Vec3 look = this.getViewVector(1.0F);
                     world.playSound(
                        null,
                        this.getX() + (i == 1 ? -look.z : look.z),
                        this.getY(),
                        this.getZ() + (i == 1 ? look.x : -look.x),
                        soundevent,
                        this.getSoundSource(),
                        1.0F,
                        0.8F + 0.4F * this.getRandom().nextFloat()
                     );
                  }
               }

               Array.setFloat(field_paddlePositions.get(this), i, (float)(paddlePosition + 0.04));
            } else {
               Array.setFloat(field_paddlePositions.get(this), i, 0.0F);
            }
         }
      } catch (Exception var8) {
         throw new RuntimeException("Error reflecting boat in update", var8);
      }

      this.applyEffectsFromBlocks();
      List<Entity> list = world.getEntities(this, this.getBoundingBox().inflate(0.2, -0.01, 0.2), EntitySelector.pushableBy(this));
      if (!list.isEmpty()) {
         boolean flag = !world.isClientSide() && !(this.getControllingPassenger() instanceof Player);

         for (Entity entity : list) {
            if (!entity.hasPassenger(this)) {
               if (flag
                  && this.getPassengers().size() < 2
                  && !entity.isPassenger()
                  && entity.getBbWidth() < this.getBbWidth()
                  && entity instanceof LivingEntity
                  && !(entity instanceof WaterAnimal)
                  && !(entity instanceof Player)) {
                  entity.startRiding(this);
               } else {
                  this.push(entity);
               }
            }
         }
      }
   }

   private void doEntityUpdate(Level world) {
      if (!world.isClientSide()) {
         this.setSharedFlag(6, this.isCurrentlyGlowing());
      }

      this.baseTick();
   }

   private void updateMotion() {
      double generalHeightChangingValue = this.isNoGravity() ? 0.0 : -0.04;
      double heightChange = 0.0;
      float momentum = 0.05F;

      try {
         Status status = (Status)field_status.get(this);
         if (field_previousStatus.get(this) == Status.IN_AIR && status != Status.IN_AIR && status != Status.ON_LAND) {
            field_waterLevel.setDouble(this, this.getBoundingBox().minY + this.getBbHeight());
            this.setPos(this.getX(), this.getWaterLevelAbove() - this.getBbHeight() + 0.101, this.getZ());
            this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, 0.0, this.getDeltaMovement().z));
            field_lastYd.setDouble(this, 0.0);
            field_status.set(this, Status.IN_WATER);
         } else {
            switch (status) {
               case IN_AIR:
                  momentum = 0.9F;
                  break;
               case IN_WATER:
                  heightChange = (field_waterLevel.getDouble(this) - this.getBoundingBox().minY) / this.getBbHeight();
                  momentum = 0.9F;
                  break;
               case ON_LAND:
                  momentum = field_boatGlide.getFloat(this);
                  if (this.getControllingPassenger() instanceof Player) {
                     field_boatGlide.setFloat(this, momentum / 2.0F);
                  }
                  break;
               case UNDER_FLOWING_WATER:
                  generalHeightChangingValue = -7.0E-4;
                  momentum = 0.9F;
                  break;
               case UNDER_WATER:
                  heightChange = 0.01;
                  momentum = 0.45F;
            }

            this.setDeltaMovement(new Vec3(this.getDeltaMovement().x * momentum, this.getDeltaMovement().y, this.getDeltaMovement().z));
            this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, this.getDeltaMovement().y, this.getDeltaMovement().z * momentum));
            field_deltaRotation.setFloat(this, field_deltaRotation.getFloat(this) * momentum);
            this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, this.getDeltaMovement().y + generalHeightChangingValue, this.getDeltaMovement().z));
            if (heightChange > 0.0) {
               this.setDeltaMovement(
                  new Vec3(this.getDeltaMovement().x, this.getDeltaMovement().y + heightChange * 0.061538461538461535, this.getDeltaMovement().z)
               );
               this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, this.getDeltaMovement().y * 0.75, this.getDeltaMovement().z));
            }
         }
      } catch (Exception var7) {
         throw new RuntimeException("Error reflecting boat in updateMotion", var7);
      }
   }

   public float getWaterLevelAbove() {
      AABB boundingBox = this.getBoundingBox();
      int minX = (int)Math.floor(boundingBox.minX);
      int maxX = (int)Math.ceil(boundingBox.maxX);
      int minZ = (int)Math.floor(boundingBox.minZ);
      int maxZ = (int)Math.ceil(boundingBox.maxZ);
      MutableBlockPos blockPosPool = new MutableBlockPos();

      try {
         Level world = this.level();
         int maxY = (int)Math.ceil(boundingBox.maxY - field_lastYd.getDouble(this));

         label81:
         for (int y = (int)Math.floor(boundingBox.maxY); y < maxY; y++) {
            float waterHeight = 0.0F;

            for (int x = minX; x < maxX; x++) {
               for (int z = minZ; z < maxZ; z++) {
                  blockPosPool.set(x, y, z);
                  BlockState block = world.getBlockState(blockPosPool);
                  if (this.isWater(block)) {
                     waterHeight = Math.max(waterHeight, getBlockLiquidHeight(block, world, blockPosPool));
                  }

                  if (waterHeight >= 1.0F) {
                     continue label81;
                  }
               }
            }

            if (waterHeight < 1.0F) {
               return blockPosPool.getY() + waterHeight;
            }
         }

         return maxY + 1;
      } catch (Exception var17) {
         throw new RuntimeException("Error reflecting boat in getWaterLevelAbove", var17);
      } finally {
         ;
      }
   }

   private Status getBoatStatus() {
      Status isUnderWater = this.getUnderwaterStatus();

      try {
         if (isUnderWater != null) {
            field_waterLevel.setDouble(this, this.getBoundingBox().maxY);
            return isUnderWater;
         } else if (this.checkInWater()) {
            return Status.IN_WATER;
         } else {
            float glideSpeed = this.getGroundFriction();
            if (glideSpeed > 0.0F) {
               field_boatGlide.setFloat(this, glideSpeed);
               return Status.ON_LAND;
            } else {
               return Status.IN_AIR;
            }
         }
      } catch (Exception var3) {
         throw new RuntimeException("Error reflecting boat in getBoatStatus", var3);
      }
   }

   private boolean checkInWater() {
      Level world = this.level();
      AABB boundingBox = this.getBoundingBox();
      boolean isInWater = false;
      MutableBlockPos blockPosPool = new MutableBlockPos();

      try {
         double waterLevel = Double.MIN_VALUE;

         for (int x = (int)Math.floor(boundingBox.minX); x < Math.ceil(boundingBox.maxX); x++) {
            for (int y = (int)Math.floor(boundingBox.minY); y < Math.ceil(boundingBox.minY + 0.001); y++) {
               for (int z = (int)Math.floor(boundingBox.minZ); z < Math.ceil(boundingBox.maxZ); z++) {
                  blockPosPool.set(x, y, z);
                  BlockState block = world.getBlockState(blockPosPool);
                  if (this.isWater(block)) {
                     float waterHeight = getLiquidHeight(block, world, blockPosPool);
                     waterLevel = Math.max((double)waterHeight, waterLevel);
                     isInWater |= boundingBox.minY < waterHeight;
                  }
               }
            }
         }

         field_waterLevel.setDouble(this, waterLevel);
         return isInWater;
      } catch (Exception var15) {
         throw new RuntimeException("Error reflecting boat in checkInWater", var15);
      } finally {
         ;
      }
   }

   @Nullable
   private Status getUnderwaterStatus() {
      Level world = this.level();
      AABB boundingBox = this.getBoundingBox();
      double boatTop = boundingBox.maxY + 0.001;
      MutableBlockPos blockPosPool = new MutableBlockPos();

      for (int x = (int)Math.floor(boundingBox.minX); x < Math.ceil(boundingBox.maxX); x++) {
         for (int y = (int)Math.floor(boundingBox.maxY); y < Math.ceil(boatTop); y++) {
            for (int z = (int)Math.floor(boundingBox.minZ); z < Math.ceil(boundingBox.maxZ); z++) {
               blockPosPool.set(x, y, z);
               BlockState block = world.getBlockState(blockPosPool);
               if (this.isWater(block) && boatTop < getLiquidHeight(block, world, blockPosPool)) {
                  return !block.getFluidState().isSource() ? Status.UNDER_FLOWING_WATER : Status.UNDER_WATER;
               }
            }
         }
      }

      return null;
   }

   public static float getLiquidHeight(BlockState block, BlockGetter world, BlockPos pos) {
      return pos.getY() + getBlockLiquidHeight(block, world, pos);
   }

   public static float getBlockLiquidHeight(BlockState block, BlockGetter world, BlockPos pos) {
      return block.getFluidState().getHeight(world, pos);
   }

   private void controlBoat() {
      if (this.isVehicle()) {
         float speed = 0.0F;

         try {
            boolean left = field_leftInputDown.getBoolean(this);
            boolean right = field_rightInputDown.getBoolean(this);
            boolean forward = field_forwardInputDown.getBoolean(this);
            boolean backward = field_backInputDown.getBoolean(this);
            if (left) {
               field_deltaRotation.setFloat(this, field_deltaRotation.getFloat(this) - 1.0F);
            }

            if (right) {
               field_deltaRotation.setFloat(this, field_deltaRotation.getFloat(this) + 1.0F);
            }

            if (right != left && !forward && !backward) {
               speed += 0.005F;
            }

            this.setYRot(this.getYRot() + field_deltaRotation.getFloat(this));
            if (forward) {
               speed += 0.04F;
            }

            if (backward) {
               speed -= 0.005F;
            }

            this.setDeltaMovement(
               new Vec3(
                  this.getDeltaMovement().x + Mth.sin(-this.getYRot() * (float) Math.PI / 180.0F) * speed * this.getAccelerationFactor(),
                  this.getDeltaMovement().y,
                  this.getDeltaMovement().z
               )
            );
            this.setDeltaMovement(
               new Vec3(
                  this.getDeltaMovement().x,
                  this.getDeltaMovement().y,
                  this.getDeltaMovement().z + Mth.cos(this.getYRot() * (float) Math.PI / 180.0F) * speed * this.getAccelerationFactor()
               )
            );
            this.setPaddleState(right && !left || forward, left && !right || forward);
         } catch (Exception var6) {
            throw new RuntimeException("Error reflecting boat in controlBoat", var6);
         }
      }
   }

   protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
      boolean breaks = onGround && this.fallDistance > 3.0 && !this.isPassenger() && !this.isRemoved();
      super.checkFallDamage(y, onGround, state, pos);
      if (breaks && this.level() instanceof ServerLevel server) {
         this.kill(server);
         if ((Boolean)server.getGameRules().get(GameRules.ENTITY_DROPS)) {
            this.spawnAtLocation(server, this.getBrokenItem());
         }
      }
   }

   public void destroy(ServerLevel server, Item dropItem) {
      this.kill(server);
      if ((Boolean)server.getGameRules().get(GameRules.ENTITY_DROPS)) {
         ItemStack drop = this.getItem().copy();
         if (this.hasCustomName()) {
            drop.set(DataComponents.CUSTOM_NAME, this.getCustomName());
         }

         this.spawnAtLocation(server, drop);
      }
   }

   public ItemStack getPickedResult(HitResult target) {
      return this.getItem();
   }

   protected abstract ItemStack getItem();

   protected ItemStack getBrokenItem() {
      return this.getItem();
   }

   public abstract String getTexture();

   protected double getAccelerationFactor() {
      return 1.0;
   }

   protected double getTopSpeed() {
      return 0.35;
   }

   protected boolean isWater(BlockState block) {
      return BlockMaterial.of(block) == BlockMaterial.WATER;
   }
}
