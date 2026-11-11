package ic2.core.item.armor.jetpack;

import ic2.core.IC2;
import ic2.core.audio.AudioSource;
import ic2.core.audio.PositionSpec;
import ic2.core.util.ItemData;
import ic2.core.util.StackUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class JetpackLogic {
   private static boolean lastJetpackUsed;
   private static AudioSource audioSource;

   public static boolean useJetpack(Player player, boolean hoverMode, IJetpack jetpack, ItemStack stack) {
      if (jetpack.getChargeLevel(stack) <= 0.0) {
         return false;
      } else {
         IBoostingJetpack bjetpack = jetpack instanceof IBoostingJetpack ? (IBoostingJetpack)jetpack : null;
         float power = jetpack.getPower(stack);
         float dropPercentage = jetpack.getDropPercentage(stack);
         if (jetpack.getChargeLevel(stack) <= dropPercentage) {
            power = (float)(power * (jetpack.getChargeLevel(stack) / dropPercentage));
         }

         if (IC2.keyboard.isForwardKeyDown(player)) {
            float retruster;
            float boost;
            if (bjetpack != null) {
               retruster = bjetpack.getBaseThrust(stack, hoverMode);
               boost = bjetpack.getBoostThrust(player, stack, hoverMode);
            } else {
               retruster = hoverMode ? 1.0F : 0.15F;
               boost = 0.0F;
            }

            float forwardpower = power * retruster * 2.0F;
            if (forwardpower > 0.0F) {
               player.moveRelative(0.02F + boost, new Vec3(0.0, 0.0, 0.4F * forwardpower + boost));
               if (boost != 0.0F && !player.onGround()) {
                  bjetpack.useBoostPower(stack, boost);
               }
            }
         }

         int worldHeight = IC2.getWorldHeight(player.level());
         int maxFlightHeight = (int)(worldHeight / jetpack.getWorldHeightDivisor(stack));
         double y = player.getY();
         if (y > maxFlightHeight - 25) {
            if (y > maxFlightHeight) {
               y = maxFlightHeight;
            }

            power = (float)(power * ((maxFlightHeight - y) / 25.0));
         }

         double prevmotion = player.getDeltaMovement().y;
         player.setDeltaMovement(new Vec3(player.getDeltaMovement().x, Math.min(player.getDeltaMovement().y + power * 0.2F, 0.6F), player.getDeltaMovement().z));
         if (hoverMode) {
            float maxHoverY = 0.0F;
            if (IC2.keyboard.isJumpKeyDown(player)) {
               maxHoverY += jetpack.getHoverMultiplier(stack, true);
               if (bjetpack != null) {
                  maxHoverY *= bjetpack.getHoverBoost(player, stack, true);
               }
            }

            if (IC2.keyboard.isSneakKeyDown(player)) {
               maxHoverY += -jetpack.getHoverMultiplier(stack, false);
               if (bjetpack != null) {
                  maxHoverY *= bjetpack.getHoverBoost(player, stack, false);
               }
            }

            if (player.getDeltaMovement().y > maxHoverY) {
               player.setDeltaMovement(new Vec3(player.getDeltaMovement().x, maxHoverY, player.getDeltaMovement().z));
               if (prevmotion > player.getDeltaMovement().y) {
                  player.setDeltaMovement(new Vec3(player.getDeltaMovement().x, prevmotion, player.getDeltaMovement().z));
               }
            }
         }

         int consume = 2;
         if (hoverMode) {
            consume = 1;
         }

         if (!player.onGround()) {
            jetpack.drainEnergy(stack, consume);
         }

         player.fallDistance = 0.0;
         player.resetFallDistance();
         IC2.platform.resetPlayerInAirTime(player);
         return true;
      }
   }

   public static void onArmorTick(Level world, Player player, ItemStack stack, IJetpack jetpack) {
      if (stack != null && jetpack.isJetpackActive(stack)) {
         CompoundTag nbtData = getJetpackCompound(stack);
         boolean hoverMode = getHoverMode(nbtData);
         byte toggleTimer = nbtData.getByteOr("toggleTimer", (byte)0);
         boolean jetpackUsed = false;
         if (IC2.keyboard.isJumpKeyDown(player) && IC2.keyboard.isModeSwitchKeyDown(player) && toggleTimer == 0) {
            toggleTimer = 10;
            hoverMode = !hoverMode;
            if (IC2.platform.isSimulating()) {
               nbtData.putBoolean("hoverMode", hoverMode);
               if (hoverMode) {
                  IC2.platform.messagePlayer(player, "Hover Mode enabled.");
               } else {
                  IC2.platform.messagePlayer(player, "Hover Mode disabled.");
               }
            }
         }

         if (IC2.keyboard.isJumpKeyDown(player) || hoverMode) {
            jetpackUsed = useJetpack(player, hoverMode, jetpack, stack);
            if (player.onGround() && hoverMode && IC2.platform.isSimulating()) {
               setHoverMode(nbtData, false);
               IC2.platform.messagePlayer(player, "Hover Mode disabled.");
            }
         }

         if (IC2.platform.isSimulating() && toggleTimer > 0) {
            nbtData.putByte("toggleTimer", --toggleTimer);
         }

         ItemData.commit(stack, nbtData);
         if (IC2.platform.isRendering() && player == IC2.platform.getPlayerInstance()) {
            if (lastJetpackUsed != jetpackUsed) {
               if (jetpackUsed) {
                  if (audioSource == null) {
                     audioSource = IC2.audioManager
                        .createSource(player, PositionSpec.Backpack, "Tools/Jetpack/JetpackLoop.ogg", true, false, IC2.audioManager.getDefaultVolume());
                  }

                  if (audioSource != null) {
                     audioSource.play();
                  }
               } else if (audioSource != null) {
                  audioSource.remove();
                  audioSource = null;
               }

               lastJetpackUsed = jetpackUsed;
            }

            if (audioSource != null) {
               audioSource.updatePosition();
            }
         }

         if (jetpackUsed) {
            player.inventoryMenu.broadcastChanges();
         }
      }
   }

   private static void setHoverMode(CompoundTag nbt, boolean value) {
      nbt.putBoolean("hoverMode", value);
   }

   private static boolean getHoverMode(CompoundTag nbt) {
      return nbt.getBooleanOr("hoverMode", false);
   }

   private static CompoundTag getJetpackCompound(ItemStack stack) {
      return StackUtil.getOrCreateNbtData(stack);
   }
}
