package ic2.core.item.armor;

import ic2.core.ref.ItemName;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorMaterials;

public class ItemArmorUtility extends ItemArmorIC2 implements ArmorProtection {
   public ItemArmorUtility(ItemName name, String armorName, EquipmentSlot type) {
      super(name, ArmorMaterials.DIAMOND, armorName, type, null);
   }

   public int func_77619_b() {
      return 0;
   }

   @Override
   public boolean func_82789_a(ItemStack par1ItemStack, ItemStack par2ItemStack) {
      return false;
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
   public void damageArmor(LivingEntity entity, ItemStack stack, DamageSource source, int damage, int slot) {
   }
}
