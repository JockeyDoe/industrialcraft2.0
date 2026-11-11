package ic2.core.item.armor;

import ic2.api.item.ElectricItem;
import ic2.api.item.HudMode;
import ic2.api.item.IItemHudProvider;
import ic2.core.IC2;
import ic2.core.init.Localization;
import ic2.core.ref.ItemName;
import ic2.core.util.ItemData;
import ic2.core.util.StackUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

public class ItemArmorNanoSuit extends ItemArmorElectric implements IItemHudProvider {
   public ItemArmorNanoSuit(ItemName name, EquipmentSlot armorType) {
      super(name, "nano", armorType, 1000000.0, 1600.0, 3);
      if (armorType == EquipmentSlot.FEET) {
         NeoForge.EVENT_BUS.register(this);
      }
   }

   @Override
   public ArmorProtection.ArmorProperties getProperties(LivingEntity player, ItemStack armor, DamageSource source, double damage, int slot) {
      if (source.is(DamageTypes.FALL) && this.armorSlot == EquipmentSlot.FEET) {
         int energyPerDamage = this.getEnergyPerDamage();
         int damageLimit = Integer.MAX_VALUE;
         if (energyPerDamage > 0) {
            damageLimit = (int)Math.min((double)damageLimit, 25.0 * ElectricItem.manager.getCharge(armor) / energyPerDamage);
         }

         return new ArmorProtection.ArmorProperties(10, damage < 8.0 ? 1.0 : 0.875, damageLimit);
      } else {
         return super.getProperties(player, armor, source, damage, slot);
      }
   }

   @SubscribeEvent(priority = EventPriority.LOW)
   public void onEntityLivingFallEvent(LivingFallEvent event) {
      if (IC2.platform.isSimulating() && event.getEntity() instanceof LivingEntity) {
         LivingEntity entity = event.getEntity();
         ItemStack armor = entity.getItemBySlot(EquipmentSlot.FEET);
         if (armor != null && armor.getItem() == this) {
            int fallDamage = (int)event.getDistance() - 3;
            if (fallDamage >= 8) {
               return;
            }

            double energyCost = this.getEnergyPerDamage() * fallDamage;
            if (energyCost <= ElectricItem.manager.getCharge(armor)) {
               ElectricItem.manager.discharge(armor, energyCost, Integer.MAX_VALUE, true, false, false);
               event.setCanceled(true);
            }
         }
      }
   }

   public void onArmorTick(Level world, Player player, ItemStack stack) {
      CompoundTag nbtData = StackUtil.getOrCreateNbtData(stack);
      byte toggleTimer = nbtData.getByteOr("toggleTimer", (byte)0);
      boolean ret = false;
      if (this.armorSlot == EquipmentSlot.HEAD) {
         IC2.platform.profilerStartSection("NanoHelmet");
         boolean Nightvision = nbtData.getBooleanOr("Nightvision", false);
         short hubmode = nbtData.getShortOr("HudMode", (short)0);
         if (IC2.keyboard.isAltKeyDown(player) && IC2.keyboard.isModeSwitchKeyDown(player) && toggleTimer == 0) {
            toggleTimer = 10;
            Nightvision = !Nightvision;
            if (IC2.platform.isSimulating()) {
               nbtData.putBoolean("Nightvision", Nightvision);
               ItemData.commit(stack, nbtData);
               if (Nightvision) {
                  IC2.platform.messagePlayer(player, "Nightvision enabled.");
               } else {
                  IC2.platform.messagePlayer(player, "Nightvision disabled.");
               }
            }
         }

         if (IC2.keyboard.isAltKeyDown(player) && IC2.keyboard.isHudModeKeyDown(player) && toggleTimer == 0) {
            toggleTimer = 10;
            if (hubmode == HudMode.getMaxMode()) {
               hubmode = 0;
            } else {
               hubmode++;
            }

            if (IC2.platform.isSimulating()) {
               nbtData.putShort("HudMode", hubmode);
               ItemData.commit(stack, nbtData);
               IC2.platform.messagePlayer(player, Localization.translate(HudMode.getFromID(hubmode).getTranslationKey()));
            }
         }

         if (IC2.platform.isSimulating() && toggleTimer > 0) {
            nbtData.putByte("toggleTimer", --toggleTimer);
            ItemData.commit(stack, nbtData);
         }

         if (Nightvision && IC2.platform.isSimulating() && ElectricItem.manager.use(stack, 1.0, player)) {
            BlockPos pos = new BlockPos((int)Math.floor(player.getX()), (int)Math.floor(player.getY()), (int)Math.floor(player.getZ()));
            int skylight = player.level().getMaxLocalRawBrightness(pos);
            if (skylight > 8) {
               IC2.platform.removePotion(player, MobEffects.NIGHT_VISION);
               player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, true, true));
            } else {
               IC2.platform.removePotion(player, MobEffects.BLINDNESS);
               player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, true));
            }

            ret = true;
         }

         IC2.platform.profilerEndSection();
      }

      if (ret) {
         player.inventoryMenu.broadcastChanges();
      }
   }

   @Override
   public double getDamageAbsorptionRatio() {
      return 0.9;
   }

   @Override
   public int getEnergyPerDamage() {
      return 5000;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Rarity func_77613_e(ItemStack stack) {
      return Rarity.UNCOMMON;
   }

   @Override
   public boolean doesProvideHUD(ItemStack stack) {
      return this.armorSlot == EquipmentSlot.HEAD && ElectricItem.manager.getCharge(stack) > 0.0;
   }

   @Override
   public HudMode getHudMode(ItemStack stack) {
      return HudMode.getFromID(StackUtil.getOrCreateNbtData(stack).getShortOr("HudMode", (short)0));
   }
}
