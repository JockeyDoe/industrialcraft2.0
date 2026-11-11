package ic2.core.item.armor;

import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import ic2.api.item.IItemHudInfo;
import ic2.core.IC2;
import ic2.core.item.ElectricItemManager;
import ic2.core.item.ItemDefaults;
import ic2.core.ref.ItemName;
import ic2.core.util.ItemData;
import ic2.core.util.StackUtil;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemArmorNightvisionGoggles extends ItemArmorUtility implements IElectricItem, IItemHudInfo {
   public ItemArmorNightvisionGoggles() {
      super(ItemName.nightvision_goggles, "nightvision", EquipmentSlot.HEAD);
      ItemDefaults.durability(this, 27);
      ItemDefaults.noRepair(this);
   }

   @Override
   public boolean canProvideEnergy(ItemStack stack) {
      return false;
   }

   @Override
   public double getMaxCharge(ItemStack stack) {
      return 200000.0;
   }

   @Override
   public int getTier(ItemStack stack) {
      return 1;
   }

   @Override
   public double getTransferLimit(ItemStack stack) {
      return 200.0;
   }

   @Override
   public List<String> getHudInfo(ItemStack stack, boolean advanced) {
      List<String> info = new LinkedList<>();
      info.add(ElectricItem.manager.getToolTip(stack));
      return info;
   }

   public void onArmorTick(Level world, Player player, ItemStack stack) {
      CompoundTag nbtData = StackUtil.getOrCreateNbtData(stack);
      boolean active = nbtData.getBooleanOr("active", false);
      byte toggleTimer = nbtData.getByteOr("toggleTimer", (byte)0);
      if (IC2.keyboard.isAltKeyDown(player) && IC2.keyboard.isModeSwitchKeyDown(player) && toggleTimer == 0) {
         toggleTimer = 10;
         active = !active;
         if (IC2.platform.isSimulating()) {
            nbtData.putBoolean("active", active);
            ItemData.commit(stack, nbtData);
            if (active) {
               IC2.platform.messagePlayer(player, "Nightvision enabled.");
            } else {
               IC2.platform.messagePlayer(player, "Nightvision disabled.");
            }
         }
      }

      if (IC2.platform.isSimulating() && toggleTimer > 0) {
         nbtData.putByte("toggleTimer", --toggleTimer);
         ItemData.commit(stack, nbtData);
      }

      boolean ret = false;
      if (active && IC2.platform.isSimulating() && ElectricItem.manager.use(stack, 1.0, player)) {
         int skylight = player.level().getMaxLocalRawBrightness(player.blockPosition());
         if (skylight > 8) {
            IC2.platform.removePotion(player, MobEffects.NIGHT_VISION);
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, true, true));
         } else {
            IC2.platform.removePotion(player, MobEffects.BLINDNESS);
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, true));
         }

         ret = true;
      }

      if (ret) {
         player.inventoryMenu.broadcastChanges();
      }
   }

   @Override
   public void func_150895_a(CreativeModeTab tab, NonNullList<ItemStack> subItems) {
      if (this.func_194125_a(tab)) {
         ElectricItemManager.addChargeVariants(this, subItems);
      }
   }

   @Override
   public boolean func_82789_a(ItemStack par1ItemStack, ItemStack par2ItemStack) {
      return false;
   }
}
