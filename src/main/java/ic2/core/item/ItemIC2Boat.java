package ic2.core.item;

import ic2.core.block.state.IIdProvider;
import ic2.core.entity.Ic2EntityTypes;
import ic2.core.ref.ItemName;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import ic2.core.util.Vector3;
import ic2.core.util.WorldQueries;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class ItemIC2Boat extends ItemMulti<ItemIC2Boat.BoatType> {
   public ItemIC2Boat() {
      super(ItemName.boat, ItemIC2Boat.BoatType.class);
   }

   @Override
   public InteractionResult use(Level world, Player player, InteractionHand hand) {
      ItemStack stack = StackUtil.get(player, hand);
      EntityIC2Boat boat = this.makeBoat(stack, world, player);
      if (boat == null) {
         return InteractionResult.PASS;
      } else {
         Vector3 lookVec = Util.getLookScaled(player);
         Vector3 start = Util.getEyePosition(player);
         Vec3 startMc = start.toVec3();
         HitResult hitPos = WorldQueries.clip(world, startMc, start.add(lookVec).toVec3(), true);
         if (hitPos == null) {
            return InteractionResult.PASS;
         } else {
            boolean inEntity = false;
            float border = 1.0F;

            for (Entity entity : world.getEntities(player, player.getBoundingBox().expandTowards(lookVec.x, lookVec.y, lookVec.z).inflate(border))) {
               if (entity.isPickable()) {
                  border = entity.getPickRadius();
                  AABB aabb = entity.getBoundingBox().inflate(border);
                  if (aabb.contains(startMc)) {
                     inEntity = true;
                     break;
                  }
               }
            }

            if (inEntity) {
               return InteractionResult.PASS;
            } else {
               if (hitPos.getType() == Type.BLOCK) {
                  BlockPos pos = ((BlockHitResult)hitPos).getBlockPos();
                  if (world.getBlockState(pos).getBlock() == Blocks.SNOW) {
                     pos = pos.below();
                  }

                  boat.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
                  boat.setYRot(((Mth.floor(player.getYRot() * 4.0F / 360.0F + 0.5) & 3) - 1) * 90);
                  if (!world.noCollision(boat, boat.getBoundingBox().deflate(0.1))) {
                     return InteractionResult.PASS;
                  }

                  if (!world.isClientSide()) {
                     world.addFreshEntity(boat);
                  }

                  if (!player.getAbilities().instabuild) {
                     stack = StackUtil.decSize(stack);
                  }
               }

               return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
            }
         }
      }
   }

   protected EntityIC2Boat makeBoat(ItemStack stack, Level world, Player player) {
      ItemIC2Boat.BoatType type = this.getType(stack);
      if (type == null) {
         return null;
      } else {
         switch (type) {
            case rubber:
               return new EntityBoatRubber(world);
            case carbon:
               return new EntityBoatCarbon(world);
            case electric:
               return new EntityBoatElectric(world);
            default:
               return null;
         }
      }
   }

   public boolean hasCustomEntity(ItemStack stack) {
      return this.getType(stack) == ItemIC2Boat.BoatType.electric;
   }

   public Entity createEntity(Level world, Entity location, ItemStack stack) {
      assert this.hasCustomEntity(stack);

      assert !world.isClientSide();

      ItemEntity item = new ItemIC2Boat.FireproofItem(world, location.getX(), location.getY(), location.getZ(), stack);
      item.setDefaultPickUpDelay();
      item.setDeltaMovement(new Vec3(location.getDeltaMovement().x, item.getDeltaMovement().y, item.getDeltaMovement().z));
      item.setDeltaMovement(new Vec3(item.getDeltaMovement().x, location.getDeltaMovement().y, item.getDeltaMovement().z));
      item.setDeltaMovement(new Vec3(item.getDeltaMovement().x, item.getDeltaMovement().y, location.getDeltaMovement().z));
      return item;
   }

   public static enum BoatType implements IIdProvider {
      broken_rubber,
      rubber,
      carbon,
      electric;

      @Override
      public String getName() {
         return this.name();
      }

      @Override
      public int getId() {
         return this.ordinal();
      }
   }

   public static class FireproofItem extends ItemEntity {
      public FireproofItem(Level world, double x, double y, double z, ItemStack stack) {
         this(world);
         this.setPos(x, y, z);
         this.setItem(stack);
      }

      public FireproofItem(Level world, double x, double y, double z) {
         this(world);
         this.setPos(x, y, z);
      }

      public FireproofItem(Level world) {
         super(Ic2EntityTypes.get("fireproof_item", ItemEntity.class), world);
      }

      public void tick() {
         super.tick();
         this.clearFire();
      }

      public boolean fireImmune() {
         return true;
      }

      protected void func_70081_e(int amount) {
      }

      public void igniteForSeconds(int seconds) {
         this.clearFire();
      }
   }
}
