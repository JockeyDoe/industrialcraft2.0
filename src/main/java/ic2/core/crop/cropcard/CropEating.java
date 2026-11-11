package ic2.core.crop.cropcard;

import ic2.api.crops.BiomeCategory;
import ic2.api.crops.CropProperties;
import ic2.api.crops.ICropTile;
import ic2.api.item.ItemWrapper;
import ic2.core.IC2;
import ic2.core.IC2DamageSource;
import ic2.core.crop.IC2CropCard;
import ic2.core.slot.ArmorSlot;
import ic2.core.util.BiomeUtil;
import ic2.core.util.StackUtil;
import ic2.core.util.WorldQueries;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class CropEating extends IC2CropCard {
   private final double movementMultiplier = 0.5;
   private final double length = 1.0;
   private static final IC2DamageSource damage = new IC2DamageSource("cropEating");

   @Override
   public String getDiscoveredBy() {
      return "Hasudako";
   }

   @Override
   public String getId() {
      return "eatingplant";
   }

   @Override
   public CropProperties getProperties() {
      return new CropProperties(6, 1, 1, 3, 1, 4);
   }

   @Override
   public String[] getAttributes() {
      return new String[]{"Bad", "Food"};
   }

   @Override
   public int getMaxSize() {
      return 6;
   }

   @Override
   public boolean canGrow(ICropTile crop) {
      return crop.getCurrentSize() < 3
         ? crop.getLightLevel() > 10
         : crop.isBlockBelow(Blocks.LAVA) && crop.getCurrentSize() < this.getMaxSize() && crop.getLightLevel() > 10;
   }

   @Override
   public int getOptimalHarvestSize(ICropTile crop) {
      return 4;
   }

   @Override
   public boolean canBeHarvested(ICropTile crop) {
      return crop.getCurrentSize() >= 4 && crop.getCurrentSize() < 6;
   }

   @Override
   public ItemStack getGain(ICropTile crop) {
      return crop.getCurrentSize() >= 4 && crop.getCurrentSize() < 6 ? new ItemStack(Blocks.CACTUS) : null;
   }

   @Override
   public void tick(ICropTile crop) {
      if (crop.getCurrentSize() != 1) {
         BlockPos coords = crop.getPosition();
         double xcentered = coords.getX() + 0.5;
         double ycentered = coords.getY() + 0.5;
         double zcentered = coords.getZ() + 0.5;
         if (crop.getCustomData().getBooleanOr("eaten", false)) {
            StackUtil.dropAsEntity(crop.getWorldObj(), coords, new ItemStack(Items.ROTTEN_FLESH));
            crop.getCustomData().putBoolean("eaten", false);
         }

         List<LivingEntity> list = crop.getWorldObj()
            .getEntitiesOfClass(
               LivingEntity.class, new AABB(xcentered - 1.0, coords.getY(), zcentered - 1.0, xcentered + 1.0, coords.getY() + 1.0 + 1.0, zcentered + 1.0)
            );
         if (!list.isEmpty()) {
            Collections.shuffle(list);

            for (LivingEntity entity : list) {
               if (!(entity instanceof Player) || !((Player)entity).getAbilities().instabuild) {
                  entity.setDeltaMovement(new Vec3((xcentered - entity.getX()) * 0.5, entity.getDeltaMovement().y, entity.getDeltaMovement().z));
                  entity.setDeltaMovement(new Vec3(entity.getDeltaMovement().x, entity.getDeltaMovement().y, (zcentered - entity.getZ()) * 0.5));
                  if (entity.getDeltaMovement().y > -0.05) {
                     entity.setDeltaMovement(new Vec3(entity.getDeltaMovement().x, -0.05, entity.getDeltaMovement().z));
                  }

                  WorldQueries.hurt(entity, damage, crop.getCurrentSize() * 2.0F);
                  if (!hasMetalAromor(entity)) {
                     entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 64, 50));
                     entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 64, 0));
                     entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 64, 0));
                  }

                  if (this.canGrow(crop)) {
                     crop.setGrowthPoints(crop.getGrowthPoints() + 100);
                  }

                  crop.getWorldObj()
                     .playSound(null, xcentered, ycentered, zcentered, SoundEvents.GENERIC_EAT, SoundSource.BLOCKS, 1.0F, IC2.random.nextFloat() * 0.1F + 0.9F);
                  crop.getCustomData().putBoolean("eaten", true);
                  break;
               }
            }
         }
      }
   }

   @Override
   public int getRootsLength(ICropTile crop) {
      return 5;
   }

   @Override
   public int getGrowthDuration(ICropTile crop) {
      float multiplier = 1.0F;
      BlockPos coords = crop.getPosition();
      Holder<Biome> biome = BiomeUtil.getBiome(crop.getWorldObj(), coords);
      if (BiomeCategory.hasType(biome, BiomeCategory.SWAMP) || BiomeCategory.hasType(biome, BiomeCategory.MOUNTAIN)) {
         multiplier /= 1.5F;
      }

      multiplier /= 1.0F + crop.getTerrainAirQuality() / 10.0F;
      return (int)(super.getGrowthDuration(crop) * multiplier);
   }

   private static boolean hasMetalAromor(LivingEntity entity) {
      if (!(entity instanceof Player player)) {
         return false;
      } else {
         for (ItemStack stack : ArmorSlot.getEquipment(player.getInventory())) {
            if (stack != null && ItemWrapper.isMetalArmor(stack, player)) {
               return true;
            }
         }

         return false;
      }
   }
}
