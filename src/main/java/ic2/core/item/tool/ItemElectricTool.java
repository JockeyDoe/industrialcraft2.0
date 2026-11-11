package ic2.core.item.tool;

import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import ic2.api.item.IItemHudInfo;
import ic2.core.IC2;
import ic2.core.audio.AudioSource;
import ic2.core.audio.PositionSpec;
import ic2.core.init.Localization;
import ic2.core.item.BaseElectricItem;
import ic2.core.item.ElectricItemManager;
import ic2.core.item.IPseudoDamageItem;
import ic2.core.item.ItemDefaults;
import ic2.core.item.ItemToolIC2;
import ic2.core.ref.ItemName;
import ic2.core.util.LogCategory;
import ic2.core.util.StackUtil;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public abstract class ItemElectricTool extends ItemToolIC2 implements IPseudoDamageItem, IElectricItem, IItemHudInfo {
   public double operationEnergyCost;
   public int maxCharge;
   public int transferLimit;
   public int tier;
   protected AudioSource audioSource;
   protected boolean wasEquipped;

   protected ItemElectricTool(ItemName name, int operationEnergyCost) {
      this(name, operationEnergyCost, HarvestLevel.Iron, Collections.emptySet());
   }

   protected ItemElectricTool(ItemName name, int operationEnergyCost, HarvestLevel harvestLevel, Set<? extends IToolClass> toolClasses) {
      this(name, 2.0F, -3.0F, operationEnergyCost, harvestLevel, toolClasses, new HashSet<>());
   }

   private ItemElectricTool(
      ItemName name,
      float damage,
      float speed,
      int operationEnergyCost,
      HarvestLevel harvestLevel,
      Set<? extends IToolClass> toolClasses,
      Set<Block> mineableBlocks
   ) {
      super(name, damage, speed, harvestLevel, toolClasses, mineableBlocks);
      this.operationEnergyCost = operationEnergyCost;
      ItemDefaults.durability(this, 27);
      ItemDefaults.noRepair(this);
   }

   @Override
   public InteractionResult func_180614_a(
      Player player, Level world, BlockPos pos, InteractionHand hand, Direction side, float xOffset, float yOffset, float zOffset
   ) {
      ElectricItem.manager.use(StackUtil.get(player, hand), 0.0, player);
      return super.func_180614_a(player, world, pos, hand, side, xOffset, yOffset, zOffset);
   }

   public InteractionResult use(Level world, Player player, InteractionHand hand) {
      ElectricItem.manager.use(StackUtil.get(player, hand), 0.0, player);
      return super.use(world, player, hand);
   }

   @Override
   public float getDestroySpeed(ItemStack stack, BlockState state) {
      return !ElectricItem.manager.canUse(stack, this.operationEnergyCost) ? 1.0F : super.getDestroySpeed(stack, state);
   }

   public boolean func_77644_a(ItemStack itemstack, LivingEntity entityliving, LivingEntity entityliving1) {
      return true;
   }

   public int func_77619_b() {
      return 0;
   }

   public boolean isRepairable() {
      return false;
   }

   @Override
   public boolean canProvideEnergy(ItemStack stack) {
      return false;
   }

   @Override
   public double getMaxCharge(ItemStack stack) {
      return this.maxCharge;
   }

   @Override
   public int getTier(ItemStack stack) {
      return this.tier;
   }

   @Override
   public double getTransferLimit(ItemStack stack) {
      return this.transferLimit;
   }

   public boolean func_179218_a(ItemStack stack, Level world, BlockState state, BlockPos pos, LivingEntity user) {
      if (state.getDestroySpeed(world, pos) != 0.0F) {
         if (user != null) {
            ElectricItem.manager.use(stack, this.operationEnergyCost, user);
         } else {
            ElectricItem.manager.discharge(stack, this.operationEnergyCost, this.tier, true, false, false);
         }
      }

      return true;
   }

   public boolean func_82789_a(ItemStack par1ItemStack, ItemStack par2ItemStack) {
      return false;
   }

   public boolean isBookEnchantable(ItemStack itemstack1, ItemStack itemstack2) {
      return false;
   }

   @Override
   public void func_150895_a(CreativeModeTab tab, NonNullList<ItemStack> subItems) {
      if (this.func_194125_a(tab)) {
         ElectricItemManager.addChargeVariants(this, subItems);
      }
   }

   @Override
   public List<String> getHudInfo(ItemStack stack, boolean advanced) {
      List<String> info = new LinkedList<>();
      info.add(ElectricItem.manager.getToolTip(stack));
      info.add(Localization.translate("ic2.item.tooltip.PowerTier", this.tier));
      return info;
   }

   protected ItemStack getItemStack(double charge) {
      ItemStack ret = new ItemStack(this);
      ElectricItem.manager.charge(ret, charge, Integer.MAX_VALUE, true, false);
      return ret;
   }

   public void func_77663_a(ItemStack itemstack, Level world, Entity entity, int i, boolean flag) {
      boolean isEquipped = flag && entity instanceof LivingEntity;
      if (IC2.platform.isRendering()) {
         if (isEquipped && !this.wasEquipped) {
            if (this.audioSource == null) {
               String sound = this.getIdleSound((LivingEntity)entity, itemstack);
               if (sound != null) {
                  this.audioSource = IC2.audioManager.createSource(entity, PositionSpec.Hand, sound, true, false, IC2.audioManager.getDefaultVolume());
               }
            }

            if (this.audioSource != null) {
               this.audioSource.play();
            }

            String initSound = this.getStartSound((LivingEntity)entity, itemstack);
            if (initSound != null) {
               IC2.audioManager.playOnce(entity, PositionSpec.Hand, initSound, true, IC2.audioManager.getDefaultVolume());
            }
         } else if (!isEquipped && this.audioSource != null) {
            if (entity instanceof LivingEntity theEntity) {
               ItemStack stack = theEntity.getItemBySlot(EquipmentSlot.MAINHAND);
               if (stack == null || stack.getItem() != this || stack == itemstack) {
                  this.removeAudioSource();
                  String sound = this.getStopSound(theEntity, itemstack);
                  if (sound != null) {
                     IC2.audioManager.playOnce(entity, PositionSpec.Hand, sound, true, IC2.audioManager.getDefaultVolume());
                  }
               }
            }
         } else if (this.audioSource != null) {
            this.audioSource.updatePosition();
         }

         this.wasEquipped = isEquipped;
      }
   }

   protected void removeAudioSource() {
      if (this.audioSource != null) {
         this.audioSource.stop();
         this.audioSource.remove();
         this.audioSource = null;
      }
   }

   public boolean onDroppedByPlayer(ItemStack item, Player player) {
      this.removeAudioSource();
      return true;
   }

   protected String getIdleSound(LivingEntity player, ItemStack stack) {
      return null;
   }

   protected String getStopSound(LivingEntity player, ItemStack stack) {
      return null;
   }

   protected String getStartSound(LivingEntity player, ItemStack stack) {
      return null;
   }

   public void setDamage(ItemStack stack, int damage) {
      int prev = this.getDamage(stack);
      if (damage != prev && BaseElectricItem.logIncorrectItemDamaging) {
         IC2.log.warn(LogCategory.Armor, new Throwable(), "Detected invalid armor damage application (%d):", damage - prev);
      }
   }

   @Override
   public void setStackDamage(ItemStack stack, int damage) {
      super.setDamage(stack, damage);
   }
}
