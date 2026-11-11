package ic2.core.item.armor;

import ic2.api.item.ElectricItem;
import ic2.core.item.ItemDefaults;
import ic2.core.item.armor.jetpack.IJetpack;
import ic2.core.ref.ItemName;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ItemArmorJetpackElectric extends ItemArmorElectric implements IJetpack {
   public ItemArmorJetpackElectric() {
      super(ItemName.jetpack_electric, "jetpack", EquipmentSlot.CHEST, 30000.0, 60.0, 1);
      ItemDefaults.durability(this, 27);
      ItemDefaults.stackSize(this, 1);
      ItemDefaults.noRepair(this);
   }

   @Override
   public boolean drainEnergy(ItemStack pack, int amount) {
      return ElectricItem.manager.discharge(pack, amount + 6, Integer.MAX_VALUE, true, false, false) > 0.0;
   }

   @Override
   public float getPower(ItemStack stack) {
      return 0.7F;
   }

   @Override
   public float getDropPercentage(ItemStack stack) {
      return 0.05F;
   }

   @Override
   public boolean isJetpackActive(ItemStack stack) {
      return true;
   }

   @Override
   public double getChargeLevel(ItemStack stack) {
      return ElectricItem.manager.getCharge(stack) / this.getMaxCharge(stack);
   }

   @Override
   public float getHoverMultiplier(ItemStack stack, boolean upwards) {
      return 0.1F;
   }

   @Override
   public float getWorldHeightDivisor(ItemStack stack) {
      return 1.28F;
   }

   @Override
   public ArmorProtection.ArmorProperties getProperties(LivingEntity player, ItemStack armor, DamageSource source, double damage, int slot) {
      return new ArmorProtection.ArmorProperties(0, 0.0, 0);
   }

   @Override
   public int getArmorDisplay(Player player, ItemStack armor, int slot) {
      return 0;
   }

   @Override
   public int getEnergyPerDamage() {
      return 0;
   }

   @Override
   public double getDamageAbsorptionRatio() {
      return 0.0;
   }
}
