package ic2.core.item.armor;

import ic2.api.item.ElectricItem;
import ic2.api.item.HudMode;
import ic2.api.item.IHazmatLike;
import ic2.api.item.IItemHudProvider;
import ic2.core.IC2;
import ic2.core.IC2Potion;
import ic2.core.init.Localization;
import ic2.core.init.MainConfig;
import ic2.core.item.ItemTinCan;
import ic2.core.item.armor.jetpack.IJetpack;
import ic2.core.ref.ItemName;
import ic2.core.util.ConfigUtil;
import ic2.core.util.ItemData;
import ic2.core.util.StackUtil;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResult.Success;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

public class ItemArmorQuantumSuit extends ItemArmorElectric implements IJetpack, IHazmatLike, IItemHudProvider {
   private static final int defaultColor = -1;
   protected static final Map<Holder<MobEffect>, Integer> potionRemovalCost = new IdentityHashMap<>();
   private float jumpCharge;

   public ItemArmorQuantumSuit(ItemName name, EquipmentSlot armorType) {
      super(name, "quantum", armorType, 1.0E7, 12000.0, 4);
      if (armorType == EquipmentSlot.FEET) {
         NeoForge.EVENT_BUS.register(this);
      }

      potionRemovalCost.put(MobEffects.POISON, 10000);
      potionRemovalCost.put(IC2Potion.radiation.holder(), 10000);
      potionRemovalCost.put(MobEffects.WITHER, 25000);
   }

   @Override
   protected boolean hasOverlayTexture() {
      return true;
   }

   public boolean func_82816_b_(ItemStack stack) {
      return stack.has(DataComponents.DYED_COLOR);
   }

   public void func_82815_c(ItemStack stack) {
      stack.remove(DataComponents.DYED_COLOR);
   }

   public int func_82814_b(ItemStack stack) {
      DyedItemColor color = (DyedItemColor)stack.get(DataComponents.DYED_COLOR);
      return color == null ? -1 : color.rgb();
   }

   public void func_82813_b(ItemStack stack, int color) {
      stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color & 16777215));
   }

   @Override
   public boolean addsProtection(LivingEntity entity, EquipmentSlot slot, ItemStack stack) {
      return ElectricItem.manager.getCharge(stack) > 0.0;
   }

   @Override
   public ArmorProtection.ArmorProperties getProperties(LivingEntity entity, ItemStack armor, DamageSource source, double damage, int slot) {
      int energyPerDamage = this.getEnergyPerDamage();
      int damageLimit = Integer.MAX_VALUE;
      if (energyPerDamage > 0) {
         damageLimit = (int)Math.min((double)damageLimit, 25.0 * ElectricItem.manager.getCharge(armor) / energyPerDamage);
      }

      if (source.is(DamageTypes.FALL)) {
         if (this.armorSlot == EquipmentSlot.FEET) {
            return new ArmorProtection.ArmorProperties(10, 1.0, damageLimit);
         }

         if (this.armorSlot == EquipmentSlot.LEGS) {
            return new ArmorProtection.ArmorProperties(9, 0.8, damageLimit);
         }
      }

      double absorptionRatio = this.getBaseAbsorptionRatio() * this.getDamageAbsorptionRatio();
      return new ArmorProtection.ArmorProperties(8, absorptionRatio, damageLimit);
   }

   @SubscribeEvent(priority = EventPriority.LOW)
   public void onEntityLivingFallEvent(LivingFallEvent event) {
      if (IC2.platform.isSimulating() && event.getEntity() instanceof LivingEntity) {
         LivingEntity entity = event.getEntity();
         ItemStack armor = entity.getItemBySlot(EquipmentSlot.FEET);
         if (armor != null && armor.getItem() == this) {
            int fallDamage = Math.max((int)event.getDistance() - 10, 0);
            double energyCost = this.getEnergyPerDamage() * fallDamage;
            if (energyCost <= ElectricItem.manager.getCharge(armor)) {
               ElectricItem.manager.discharge(armor, energyCost, Integer.MAX_VALUE, true, false, false);
               event.setCanceled(true);
            }
         }
      }
   }

   @Override
   public double getDamageAbsorptionRatio() {
      return this.armorSlot == EquipmentSlot.CHEST ? 1.2 : 1.0;
   }

   @Override
   public int getEnergyPerDamage() {
      return 20000;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Rarity func_77613_e(ItemStack stack) {
      return Rarity.RARE;
   }

   public void onArmorTick(Level world, Player player, ItemStack stack) {
      CompoundTag nbtData = StackUtil.getOrCreateNbtData(stack);
      byte toggleTimer = nbtData.getByteOr("toggleTimer", (byte)0);
      boolean ret = false;
      switch (this.armorSlot) {
         case HEAD:
            IC2.platform.profilerStartSection("QuantumHelmet");
            int air = player.getAirSupply();
            if (ElectricItem.manager.canUse(stack, 1000.0) && air < 100) {
               player.setAirSupply(air + 200);
               ElectricItem.manager.use(stack, 1000.0, null);
               ret = true;
            } else if (air <= 0) {
               IC2.achievements.issueAchievement(player, "starveWithQHelmet");
            }

            if (ElectricItem.manager.canUse(stack, 1000.0) && player.getFoodData().needsFood()) {
               int slot = -1;

               for (int i = 0; i < player.getInventory().getNonEquipmentItems().size(); i++) {
                  ItemStack playerStack = (ItemStack)player.getInventory().getNonEquipmentItems().get(i);
                  if (!StackUtil.isEmpty(playerStack) && playerStack.getItem() == ItemName.filled_tin_can.getInstance()) {
                     slot = i;
                     break;
                  }
               }

               if (slot > -1) {
                  ItemStack playerStack = (ItemStack)player.getInventory().getNonEquipmentItems().get(slot);
                  ItemTinCan can = (ItemTinCan)playerStack.getItem();
                  InteractionResult result = can.onEaten(player, playerStack);
                  if (result instanceof Success success && success.heldItemTransformedTo() != null) {
                     playerStack = success.heldItemTransformedTo();
                     player.getInventory().getNonEquipmentItems().set(slot, playerStack);
                  }

                  if (StackUtil.isEmpty(playerStack)) {
                     player.getInventory().getNonEquipmentItems().set(slot, StackUtil.emptyStack);
                  }

                  if (result.consumesAction()) {
                     ElectricItem.manager.use(stack, 1000.0, null);
                  }

                  ret = true;
               }
            } else if (player.getFoodData().getFoodLevel() <= 0) {
               IC2.achievements.issueAchievement(player, "starveWithQHelmet");
            }

            for (MobEffectInstance effect : new LinkedList(player.getActiveEffects())) {
               Holder<MobEffect> potion = effect.getEffect();
               Integer cost = potionRemovalCost.get(potion);
               if (cost != null) {
                  cost = cost * (effect.getAmplifier() + 1);
                  if (ElectricItem.manager.canUse(stack, cost.intValue())) {
                     ElectricItem.manager.use(stack, cost.intValue(), null);
                     IC2.platform.removePotion(player, potion);
                  }
               }
            }

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
            break;
         case CHEST:
            IC2.platform.profilerStartSection("QuantumBodyarmor");
            player.clearFire();
            IC2.platform.profilerEndSection();
            break;
         case LEGS:
            IC2.platform.profilerStartSection("QuantumLeggings");
            boolean enableQuantumSpeedOnSprint;
            if (IC2.platform.isRendering()) {
               enableQuantumSpeedOnSprint = ConfigUtil.getBool(MainConfig.get(), "misc/quantumSpeedOnSprint");
            } else {
               enableQuantumSpeedOnSprint = true;
            }

            if (ElectricItem.manager.canUse(stack, 1000.0)
               && (player.onGround() || player.isInWater())
               && IC2.keyboard.isForwardKeyDown(player)
               && (enableQuantumSpeedOnSprint && player.isSprinting() || !enableQuantumSpeedOnSprint && IC2.keyboard.isBoostKeyDown(player))) {
               byte speedTicker = nbtData.getByteOr("speedTicker", (byte)0);
               if (++speedTicker >= 10) {
                  speedTicker = 0;
                  ElectricItem.manager.use(stack, 1000.0, null);
                  ret = true;
               }

               nbtData.putByte("speedTicker", speedTicker);
               ItemData.commit(stack, nbtData);
               float speed = 0.22F;
               if (player.isInWater()) {
                  speed = 0.1F;
                  if (IC2.keyboard.isJumpKeyDown(player)) {
                     player.setDeltaMovement(new Vec3(player.getDeltaMovement().x, player.getDeltaMovement().y + 0.1F, player.getDeltaMovement().z));
                  }
               }

               if (speed > 0.0F) {
                  player.moveRelative(speed, new Vec3(0.0, 0.0, 1.0));
               }
            }

            IC2.platform.profilerEndSection();
            break;
         case FEET:
            IC2.platform.profilerStartSection("QuantumBoots");
            if (IC2.platform.isSimulating()) {
               boolean wasOnGround = nbtData.contains("wasOnGround") ? nbtData.getBooleanOr("wasOnGround", false) : true;
               if (wasOnGround && !player.onGround() && IC2.keyboard.isJumpKeyDown(player) && IC2.keyboard.isBoostKeyDown(player)) {
                  ElectricItem.manager.use(stack, 4000.0, null);
                  ret = true;
               }

               if (player.onGround() != wasOnGround) {
                  nbtData.putBoolean("wasOnGround", player.onGround());
                  ItemData.commit(stack, nbtData);
               }
            } else {
               if (ElectricItem.manager.canUse(stack, 4000.0) && player.onGround()) {
                  this.jumpCharge = 1.0F;
               }

               if (player.getDeltaMovement().y >= 0.0 && this.jumpCharge > 0.0F && !player.isInWater()) {
                  if (IC2.keyboard.isJumpKeyDown(player) && IC2.keyboard.isBoostKeyDown(player)) {
                     if (this.jumpCharge == 1.0F) {
                        player.setDeltaMovement(new Vec3(player.getDeltaMovement().x * 3.5, player.getDeltaMovement().y, player.getDeltaMovement().z));
                        player.setDeltaMovement(new Vec3(player.getDeltaMovement().x, player.getDeltaMovement().y, player.getDeltaMovement().z * 3.5));
                     }

                     player.setDeltaMovement(
                        new Vec3(player.getDeltaMovement().x, player.getDeltaMovement().y + this.jumpCharge * 0.3F, player.getDeltaMovement().z)
                     );
                     this.jumpCharge = (float)(this.jumpCharge * 0.75);
                  } else if (this.jumpCharge < 1.0F) {
                     this.jumpCharge = 0.0F;
                  }
               }
            }

            IC2.platform.profilerEndSection();
      }

      if (ret) {
         player.inventoryMenu.broadcastChanges();
      }
   }

   @Override
   public int func_77619_b() {
      return 0;
   }

   @Override
   public boolean drainEnergy(ItemStack pack, int amount) {
      return ElectricItem.manager.discharge(pack, amount + 6, Integer.MAX_VALUE, true, false, false) > 0.0;
   }

   @Override
   public float getPower(ItemStack stack) {
      return 1.0F;
   }

   @Override
   public float getDropPercentage(ItemStack stack) {
      return 0.05F;
   }

   @Override
   public double getChargeLevel(ItemStack stack) {
      return ElectricItem.manager.getCharge(stack) / this.getMaxCharge(stack);
   }

   @Override
   public boolean isJetpackActive(ItemStack stack) {
      return true;
   }

   @Override
   public float getHoverMultiplier(ItemStack stack, boolean upwards) {
      return 0.1F;
   }

   @Override
   public float getWorldHeightDivisor(ItemStack stack) {
      return 0.9F;
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
