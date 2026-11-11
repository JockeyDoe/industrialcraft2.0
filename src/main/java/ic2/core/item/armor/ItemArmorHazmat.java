package ic2.core.item.armor;

import ic2.api.item.IHazmatLike;
import ic2.api.util.FluidContainerOutputMode;
import ic2.core.IC2;
import ic2.core.IC2DamageSource;
import ic2.core.item.ItemDefaults;
import ic2.core.ref.BlockMaterial;
import ic2.core.ref.FluidName;
import ic2.core.ref.ItemName;
import ic2.core.slot.ArmorSlot;
import ic2.core.util.ItemStacks;
import ic2.core.util.LiquidUtil;
import ic2.core.util.StackUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

public class ItemArmorHazmat extends ItemArmorUtility implements IHazmatLike {
   public ItemArmorHazmat(ItemName name, EquipmentSlot type) {
      super(name, "hazmat", type);
      ItemDefaults.durability(this, 64);
      if (this.armorSlot == EquipmentSlot.FEET) {
         NeoForge.EVENT_BUS.register(this);
      }
   }

   @Override
   public ArmorProtection.ArmorProperties getProperties(LivingEntity player, ItemStack armor, DamageSource source, double damage, int slot) {
      if (this.armorSlot == EquipmentSlot.HEAD && hazmatAbsorbs(source) && hasCompleteHazmat(player)) {
         if (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.HOT_FLOOR)) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 1));
         }

         return new ArmorProtection.ArmorProperties(10, 1.0, Integer.MAX_VALUE);
      } else {
         return this.armorSlot == EquipmentSlot.FEET && source.is(DamageTypes.FALL)
            ? new ArmorProtection.ArmorProperties(10, damage < 8.0 ? 1.0 : 0.875, (armor.getMaxDamage() - ItemStacks.rawMeta(armor) + 2) * 2 * 25)
            : new ArmorProtection.ArmorProperties(0, 0.05, (armor.getMaxDamage() - ItemStacks.rawMeta(armor) + 2) / 2 * 25);
      }
   }

   @Override
   public void damageArmor(LivingEntity entity, ItemStack stack, DamageSource source, int damage, int slot) {
      if (!hazmatAbsorbs(source) || !hasCompleteHazmat(entity)) {
         int damageTotal = damage * 2;
         if (this.armorSlot == EquipmentSlot.FEET && source.is(DamageTypes.FALL)) {
            damageTotal = (damage + 1) / 2;
         }

         stack.hurtAndBreak(damageTotal, entity, this.armorSlot);
      }
   }

   @SubscribeEvent(priority = EventPriority.LOW)
   public void onEntityLivingFallEvent(LivingFallEvent event) {
      if (IC2.platform.isSimulating() && event.getEntity() instanceof Player) {
         Player player = (Player)event.getEntity();
         ItemStack armor = ArmorSlot.getEquipment(player.getInventory()).get(0);
         if (armor != null && armor.getItem() == this) {
            int fallDamage = (int)event.getDistance() - 3;
            if (fallDamage >= 8) {
               return;
            }

            int armorDamage = (fallDamage + 1) / 2;
            if (armorDamage <= armor.getMaxDamage() - ItemStacks.rawMeta(armor) && armorDamage >= 0) {
               armor.hurtAndBreak(armorDamage, player, this.armorSlot);
               event.setCanceled(true);
            }
         }
      }
   }

   public boolean isRepairable() {
      return true;
   }

   @Override
   public int getArmorDisplay(Player player, ItemStack armor, int slot) {
      return 1;
   }

   public void onArmorTick(Level world, Player player, ItemStack stack) {
      if (!world.isClientSide() && this.armorSlot == EquipmentSlot.HEAD) {
         if (player.isOnFire() && hasCompleteHazmat(player)) {
            if (this.isInLava(player)) {
               player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20, 0, true, true));
            }

            player.clearFire();
         }

         int maxAir = 300;
         int refillThreshold = 100;
         int airToMbMul = 1000;
         int airToMbDiv = 150;
         int minAmount = 7;
         int air = player.getAirSupply();
         if (air <= 100) {
            int needed = (300 - air) * 1000 / 150;
            int supplied = 0;

            for (int i = 0; i < player.getInventory().getNonEquipmentItems().size() && needed > 0; i++) {
               ItemStack cStack = (ItemStack)player.getInventory().getNonEquipmentItems().get(i);
               if (cStack != null) {
                  LiquidUtil.FluidOperationResult result = LiquidUtil.drainContainer(
                     cStack, FluidName.air.getInstance(), needed, FluidContainerOutputMode.InPlacePreferred
                  );
                  if (result != null
                     && result.fluidChange.getAmount() >= 7
                     && (result.extraOutput == null || StackUtil.storeInventoryItem(result.extraOutput, player, false))) {
                     player.getInventory().getNonEquipmentItems().set(i, result.inPlaceOutput);
                     int amount = result.fluidChange.getAmount();
                     supplied += amount;
                     needed -= amount;
                  }
               }
            }

            player.setAirSupply(air + supplied * 150 / 1000);
         }
      }
   }

   public boolean isInLava(Player player) {
      int x = (int)Math.floor(player.getX());
      int y = (int)Math.floor(player.getY() + 0.02);
      int z = (int)Math.floor(player.getZ());
      BlockState state = player.level().getBlockState(new BlockPos(x, y, z));
      if (state.getBlock() instanceof LiquidBlock && (BlockMaterial.of(state) == BlockMaterial.LAVA || BlockMaterial.of(state) == BlockMaterial.FIRE)) {
         float height = y + state.getFluidState().getHeight(player.level(), new BlockPos(x, y, z));
         return player.getY() < height;
      } else {
         return false;
      }
   }

   @Override
   public boolean addsProtection(LivingEntity entity, EquipmentSlot slot, ItemStack stack) {
      return true;
   }

   public static boolean hasCompleteHazmat(LivingEntity living) {
      for (EquipmentSlot slot : ArmorSlot.getAll()) {
         ItemStack stack = living.getItemBySlot(slot);
         if (stack == null || !(stack.getItem() instanceof IHazmatLike)) {
            return false;
         }

         IHazmatLike hazmat = (IHazmatLike)stack.getItem();
         if (!hazmat.addsProtection(living, slot, stack)) {
            return false;
         }

         if (hazmat.fullyProtects(living, slot, stack)) {
            return true;
         }
      }

      return true;
   }

   public static boolean hazmatAbsorbs(DamageSource source) {
      return source.is(DamageTypes.IN_FIRE)
         || source.is(DamageTypes.LAVA)
         || source.is(DamageTypes.ON_FIRE)
         || source.is(DamageTypes.HOT_FLOOR)
         || source.is(DamageTypes.LIGHTNING_BOLT)
         || IC2DamageSource.electricity.matches(source)
         || IC2DamageSource.radiation.matches(source);
   }

   @Override
   public boolean isMetalArmor(ItemStack itemstack, Player player) {
      return false;
   }
}
