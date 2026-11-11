package ic2.core.item.tool;

import ic2.api.event.LaserEvent;
import ic2.api.item.ElectricItem;
import ic2.api.network.INetworkItemEventListener;
import ic2.core.IC2;
import ic2.core.audio.PositionSpec;
import ic2.core.init.Localization;
import ic2.core.ref.ItemName;
import ic2.core.util.ItemData;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import ic2.core.util.Vector3;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.NeoForge;

public class ItemToolMiningLaser extends ItemElectricTool implements INetworkItemEventListener {
   private static final int EventShotMining = 0;
   private static final int EventShotLowFocus = 1;
   private static final int EventShotLongRange = 2;
   private static final int EventShotHorizontal = 3;
   private static final int EventShotSuperHeat = 4;
   private static final int EventShotScatter = 5;
   private static final int EventShotExplosive = 6;
   private static final int EventShot3x3 = 7;

   public ItemToolMiningLaser() {
      super(ItemName.mining_laser, 100);
      this.maxCharge = 300000;
      this.transferLimit = 512;
      this.tier = 3;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void func_77624_a(ItemStack stack, Level world, List<String> list, TooltipFlag par4) {
      super.func_77624_a(stack, world, list, par4);
      CompoundTag nbtData = StackUtil.getOrCreateNbtData(stack);
      String mode;
      switch (nbtData.getIntOr("laserSetting", 0)) {
         case 0:
            mode = Localization.translate("ic2.tooltip.mode.mining");
            break;
         case 1:
            mode = Localization.translate("ic2.tooltip.mode.lowFocus");
            break;
         case 2:
            mode = Localization.translate("ic2.tooltip.mode.longRange");
            break;
         case 3:
            mode = Localization.translate("ic2.tooltip.mode.horizontal");
            break;
         case 4:
            mode = Localization.translate("ic2.tooltip.mode.superHeat");
            break;
         case 5:
            mode = Localization.translate("ic2.tooltip.mode.scatter");
            break;
         case 6:
            mode = Localization.translate("ic2.tooltip.mode.explosive");
            break;
         case 7:
            mode = Localization.translate("ic2.tooltip.mode.3x3");
            break;
         default:
            return;
      }

      list.add(Localization.translate("ic2.tooltip.mode", mode));
   }

   @Override
   public List<String> getHudInfo(ItemStack stack, boolean advanced) {
      List<String> info = new LinkedList<>();
      CompoundTag nbtData = StackUtil.getOrCreateNbtData(stack);
      String mode = Localization.translate(getModeString(nbtData.getIntOr("laserSetting", 0)));
      info.addAll(super.getHudInfo(stack, advanced));
      info.add(Localization.translate("ic2.tooltip.mode", mode));
      return info;
   }

   @Override
   public InteractionResult use(Level world, Player player, InteractionHand hand) {
      ItemStack stack = StackUtil.get(player, hand);
      if (!IC2.platform.isSimulating()) {
         return InteractionResult.PASS;
      } else {
         CompoundTag nbtData = StackUtil.getOrCreateNbtData(stack);
         int laserSetting = nbtData.getIntOr("laserSetting", 0);
         if (IC2.keyboard.isModeSwitchKeyDown(player)) {
            laserSetting = (laserSetting + 1) % 8;
            nbtData.putInt("laserSetting", laserSetting);
            ItemData.commit(stack, nbtData);
            IC2.platform.messagePlayer(player, "ic2.tooltip.mode", getModeString(laserSetting));
         } else {
            int consume = new int[]{1250, 100, 5000, 0, 2500, 10000, 5000, 7500}[laserSetting];
            if (!ElectricItem.manager.use(stack, consume, player)) {
               return InteractionResult.FAIL;
            }

            switch (laserSetting) {
               case 0:
                  if (this.shootLaser(stack, world, player, Float.POSITIVE_INFINITY, 5.0F, Integer.MAX_VALUE, false, false)) {
                     IC2.network.get(true).initiateItemEvent(player, stack, 0, true);
                  }
                  break;
               case 1:
                  if (this.shootLaser(stack, world, player, 4.0F, 5.0F, 1, false, false)) {
                     IC2.network.get(true).initiateItemEvent(player, stack, 1, true);
                  }
                  break;
               case 2:
                  if (this.shootLaser(stack, world, player, Float.POSITIVE_INFINITY, 20.0F, Integer.MAX_VALUE, false, false)) {
                     IC2.network.get(true).initiateItemEvent(player, stack, 2, true);
                  }
               case 3:
               case 7:
               default:
                  break;
               case 4:
                  if (this.shootLaser(stack, world, player, Float.POSITIVE_INFINITY, 8.0F, Integer.MAX_VALUE, false, true)) {
                     IC2.network.get(true).initiateItemEvent(player, stack, 4, true);
                  }
                  break;
               case 5:
                  Vector3 look = Util.getLook(player);
                  Vector3 right = look.copy().cross(Vector3.UP);
                  if (right.lengthSquared() < 1.0E-4) {
                     double angle = Math.toRadians(player.getYRot()) - (Math.PI / 2);
                     right.set(Math.sin(angle), 0.0, -Math.cos(angle));
                  } else {
                     right.normalize();
                  }

                  Vector3 up = right.copy().cross(look);
                  int sideShots = 2;
                  double unitDistance = 8.0;
                  look.scale(8.0);

                  for (int r = -2; r <= 2; r++) {
                     for (int u = -2; u <= 2; u++) {
                        Vector3 dir = look.copy().addScaled(right, r).addScaled(up, u).normalize();
                        this.shootLaser(stack, world, dir, player, Float.POSITIVE_INFINITY, 12.0F, Integer.MAX_VALUE, false, false);
                     }
                  }

                  IC2.network.get(true).initiateItemEvent(player, stack, 5, true);
                  break;
               case 6:
                  if (this.shootLaser(stack, world, player, Float.POSITIVE_INFINITY, 12.0F, Integer.MAX_VALUE, true, false)) {
                     IC2.network.get(true).initiateItemEvent(player, stack, 6, true);
                  }
            }
         }

         return super.use(world, player, hand);
      }
   }

   public InteractionResult onItemUseFirst(Player player, Level world, BlockPos pos, Direction side, float hitX, float hitY, float hitZ, InteractionHand hand) {
      if (world.isClientSide()) {
         return InteractionResult.PASS;
      } else {
         ItemStack stack = StackUtil.get(player, hand);
         CompoundTag nbtData = StackUtil.getOrCreateNbtData(stack);
         if (!IC2.keyboard.isModeSwitchKeyDown(player) && (nbtData.getIntOr("laserSetting", 0) == 3 || nbtData.getIntOr("laserSetting", 0) == 7)) {
            Vector3 dir = Util.getLook(player);
            double angle = dir.dot(Vector3.UP);
            if (Math.abs(angle) < 1.0 / Math.sqrt(2.0)) {
               if (ElectricItem.manager.use(stack, 3000.0, player)) {
                  dir.y = 0.0;
                  dir.normalize();
                  Vector3 start = Util.getEyePosition(player);
                  start.y = pos.getY() + 0.5;
                  start = adjustStartPos(start, dir);
                  if (nbtData.getIntOr("laserSetting", 0) == 3) {
                     if (this.shootLaser(stack, world, start, dir, player, Float.POSITIVE_INFINITY, 5.0F, Integer.MAX_VALUE, false, false)) {
                        IC2.network.get(true).initiateItemEvent(player, stack, 3, true);
                     }
                  } else if (nbtData.getIntOr("laserSetting", 0) == 7
                     && this.shootLaser(stack, world, start, dir, player, Float.POSITIVE_INFINITY, 5.0F, Integer.MAX_VALUE, false, false)) {
                     this.shootLaser(
                        stack, world, new Vector3(start.x, start.y - 1.0, start.z), dir, player, Float.POSITIVE_INFINITY, 5.0F, Integer.MAX_VALUE, false, false
                     );
                     this.shootLaser(
                        stack, world, new Vector3(start.x, start.y + 1.0, start.z), dir, player, Float.POSITIVE_INFINITY, 5.0F, Integer.MAX_VALUE, false, false
                     );
                     if (player.getDirection().equals(Direction.SOUTH) || player.getDirection().equals(Direction.NORTH)) {
                        this.shootLaser(
                           stack,
                           world,
                           new Vector3(start.x - 1.0, start.y, start.z),
                           dir,
                           player,
                           Float.POSITIVE_INFINITY,
                           5.0F,
                           Integer.MAX_VALUE,
                           false,
                           false
                        );
                        this.shootLaser(
                           stack,
                           world,
                           new Vector3(start.x + 1.0, start.y, start.z),
                           dir,
                           player,
                           Float.POSITIVE_INFINITY,
                           5.0F,
                           Integer.MAX_VALUE,
                           false,
                           false
                        );
                        this.shootLaser(
                           stack,
                           world,
                           new Vector3(start.x - 1.0, start.y - 1.0, start.z),
                           dir,
                           player,
                           Float.POSITIVE_INFINITY,
                           5.0F,
                           Integer.MAX_VALUE,
                           false,
                           false
                        );
                        this.shootLaser(
                           stack,
                           world,
                           new Vector3(start.x + 1.0, start.y - 1.0, start.z),
                           dir,
                           player,
                           Float.POSITIVE_INFINITY,
                           5.0F,
                           Integer.MAX_VALUE,
                           false,
                           false
                        );
                        this.shootLaser(
                           stack,
                           world,
                           new Vector3(start.x - 1.0, start.y + 1.0, start.z),
                           dir,
                           player,
                           Float.POSITIVE_INFINITY,
                           5.0F,
                           Integer.MAX_VALUE,
                           false,
                           false
                        );
                        this.shootLaser(
                           stack,
                           world,
                           new Vector3(start.x + 1.0, start.y + 1.0, start.z),
                           dir,
                           player,
                           Float.POSITIVE_INFINITY,
                           5.0F,
                           Integer.MAX_VALUE,
                           false,
                           false
                        );
                     }

                     if (player.getDirection().equals(Direction.EAST) || player.getDirection().equals(Direction.WEST)) {
                        this.shootLaser(
                           stack,
                           world,
                           new Vector3(start.x, start.y, start.z - 1.0),
                           dir,
                           player,
                           Float.POSITIVE_INFINITY,
                           5.0F,
                           Integer.MAX_VALUE,
                           false,
                           false
                        );
                        this.shootLaser(
                           stack,
                           world,
                           new Vector3(start.x, start.y, start.z + 1.0),
                           dir,
                           player,
                           Float.POSITIVE_INFINITY,
                           5.0F,
                           Integer.MAX_VALUE,
                           false,
                           false
                        );
                        this.shootLaser(
                           stack,
                           world,
                           new Vector3(start.x, start.y - 1.0, start.z - 1.0),
                           dir,
                           player,
                           Float.POSITIVE_INFINITY,
                           5.0F,
                           Integer.MAX_VALUE,
                           false,
                           false
                        );
                        this.shootLaser(
                           stack,
                           world,
                           new Vector3(start.x, start.y - 1.0, start.z + 1.0),
                           dir,
                           player,
                           Float.POSITIVE_INFINITY,
                           5.0F,
                           Integer.MAX_VALUE,
                           false,
                           false
                        );
                        this.shootLaser(
                           stack,
                           world,
                           new Vector3(start.x, start.y + 1.0, start.z - 1.0),
                           dir,
                           player,
                           Float.POSITIVE_INFINITY,
                           5.0F,
                           Integer.MAX_VALUE,
                           false,
                           false
                        );
                        this.shootLaser(
                           stack,
                           world,
                           new Vector3(start.x, start.y + 1.0, start.z + 1.0),
                           dir,
                           player,
                           Float.POSITIVE_INFINITY,
                           5.0F,
                           Integer.MAX_VALUE,
                           false,
                           false
                        );
                     }

                     IC2.network.get(true).initiateItemEvent(player, stack, 7, true);
                  }
               }
            } else if (nbtData.getIntOr("laserSetting", 0) == 7) {
               if (ElectricItem.manager.use(stack, 3000.0, player)) {
                  dir.x = 0.0;
                  dir.z = 0.0;
                  dir.normalize();
                  Vector3 start = Util.getEyePosition(player);
                  start.x = pos.getX() + 0.5;
                  start.z = pos.getZ() + 0.5;
                  start = adjustStartPos(start, dir);
                  if (this.shootLaser(stack, world, start, dir, player, Float.POSITIVE_INFINITY, 5.0F, Integer.MAX_VALUE, false, false)) {
                     this.shootLaser(
                        stack, world, new Vector3(start.x + 1.0, start.y, start.z), dir, player, Float.POSITIVE_INFINITY, 5.0F, Integer.MAX_VALUE, false, false
                     );
                     this.shootLaser(
                        stack, world, new Vector3(start.x - 1.0, start.y, start.z), dir, player, Float.POSITIVE_INFINITY, 5.0F, Integer.MAX_VALUE, false, false
                     );
                     this.shootLaser(
                        stack,
                        world,
                        new Vector3(start.x + 1.0, start.y, start.z + 1.0),
                        dir,
                        player,
                        Float.POSITIVE_INFINITY,
                        5.0F,
                        Integer.MAX_VALUE,
                        false,
                        false
                     );
                     this.shootLaser(
                        stack,
                        world,
                        new Vector3(start.x - 1.0, start.y, start.z - 1.0),
                        dir,
                        player,
                        Float.POSITIVE_INFINITY,
                        5.0F,
                        Integer.MAX_VALUE,
                        false,
                        false
                     );
                     this.shootLaser(
                        stack,
                        world,
                        new Vector3(start.x + 1.0, start.y, start.z - 1.0),
                        dir,
                        player,
                        Float.POSITIVE_INFINITY,
                        5.0F,
                        Integer.MAX_VALUE,
                        false,
                        false
                     );
                     this.shootLaser(
                        stack,
                        world,
                        new Vector3(start.x - 1.0, start.y, start.z + 1.0),
                        dir,
                        player,
                        Float.POSITIVE_INFINITY,
                        5.0F,
                        Integer.MAX_VALUE,
                        false,
                        false
                     );
                     this.shootLaser(
                        stack, world, new Vector3(start.x, start.y, start.z + 1.0), dir, player, Float.POSITIVE_INFINITY, 5.0F, Integer.MAX_VALUE, false, false
                     );
                     this.shootLaser(
                        stack, world, new Vector3(start.x, start.y, start.z - 1.0), dir, player, Float.POSITIVE_INFINITY, 5.0F, Integer.MAX_VALUE, false, false
                     );
                     IC2.network.get(true).initiateItemEvent(player, stack, 7, true);
                  }
               }
            } else {
               IC2.platform.messagePlayer(player, "Mining laser aiming angle too steep");
            }
         }

         return InteractionResult.FAIL;
      }
   }

   private static Vector3 adjustStartPos(Vector3 pos, Vector3 dir) {
      return pos.addScaled(dir, 0.2);
   }

   public boolean shootLaser(ItemStack stack, Level world, LivingEntity owner, float range, float power, int blockBreaks, boolean explosive, boolean smelt) {
      Vector3 dir = Util.getLook(owner);
      return this.shootLaser(stack, world, dir, owner, range, power, blockBreaks, explosive, smelt);
   }

   public boolean shootLaser(
      ItemStack stack, Level world, Vector3 dir, LivingEntity owner, float range, float power, int blockBreaks, boolean explosive, boolean smelt
   ) {
      Vector3 start = adjustStartPos(Util.getEyePosition(owner), dir);
      return this.shootLaser(stack, world, start, dir, owner, range, power, blockBreaks, explosive, smelt);
   }

   public boolean shootLaser(
      ItemStack stack, Level world, Vector3 start, Vector3 dir, LivingEntity owner, float range, float power, int blockBreaks, boolean explosive, boolean smelt
   ) {
      EntityMiningLaser entity = new EntityMiningLaser(world, start, dir, owner, range, power, blockBreaks, explosive);
      LaserEvent.LaserShootEvent event = new LaserEvent.LaserShootEvent(world, entity, owner, range, power, blockBreaks, explosive, smelt, stack);
      NeoForge.EVENT_BUS.post(event);
      if (event.isCanceled()) {
         return false;
      } else {
         entity.copyDataFromEvent(event);
         world.addFreshEntity(entity);
         return true;
      }
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Rarity func_77613_e(ItemStack stack) {
      return Rarity.UNCOMMON;
   }

   @Override
   public void onNetworkEvent(ItemStack stack, Player player, int event) {
      switch (event) {
         case 0:
            IC2.audioManager.playOnce(player, PositionSpec.Hand, "Tools/MiningLaser/MiningLaser.ogg", true, IC2.audioManager.getDefaultVolume());
            break;
         case 1:
            IC2.audioManager.playOnce(player, PositionSpec.Hand, "Tools/MiningLaser/MiningLaserLowFocus.ogg", true, IC2.audioManager.getDefaultVolume());
            break;
         case 2:
            IC2.audioManager.playOnce(player, PositionSpec.Hand, "Tools/MiningLaser/MiningLaserLongRange.ogg", true, IC2.audioManager.getDefaultVolume());
            break;
         case 3:
            IC2.audioManager.playOnce(player, PositionSpec.Hand, "Tools/MiningLaser/MiningLaser.ogg", true, IC2.audioManager.getDefaultVolume());
            break;
         case 4:
            IC2.audioManager.playOnce(player, PositionSpec.Hand, "Tools/MiningLaser/MiningLaser.ogg", true, IC2.audioManager.getDefaultVolume());
            break;
         case 5:
            IC2.audioManager.playOnce(player, PositionSpec.Hand, "Tools/MiningLaser/MiningLaserScatter.ogg", true, IC2.audioManager.getDefaultVolume());
            break;
         case 6:
            IC2.audioManager.playOnce(player, PositionSpec.Hand, "Tools/MiningLaser/MiningLaserExplosive.ogg", true, IC2.audioManager.getDefaultVolume());
            break;
         case 7:
            IC2.audioManager.playOnce(player, PositionSpec.Hand, "Tools/MiningLaser/MiningLaserScatter.ogg", true, IC2.audioManager.getDefaultVolume());
      }
   }

   private static String getModeString(int mode) {
      switch (mode) {
         case 0:
            return "ic2.tooltip.mode.mining";
         case 1:
            return "ic2.tooltip.mode.lowFocus";
         case 2:
            return "ic2.tooltip.mode.longRange";
         case 3:
            return "ic2.tooltip.mode.horizontal";
         case 4:
            return "ic2.tooltip.mode.superHeat";
         case 5:
            return "ic2.tooltip.mode.scatter";
         case 6:
            return "ic2.tooltip.mode.explosive";
         case 7:
            return "ic2.tooltip.mode.3x3";
         default:
            assert false;

            return "";
      }
   }
}
