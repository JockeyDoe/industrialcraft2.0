package ic2.core.item.tool;

import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import ic2.api.item.IItemHudInfo;
import ic2.core.IC2;
import ic2.core.item.BaseElectricItem;
import ic2.core.item.ElectricItemManager;
import ic2.core.item.IPseudoDamageItem;
import ic2.core.item.ItemDefaults;
import ic2.core.ref.ItemName;
import ic2.core.util.LogCategory;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ItemToolWrenchElectric extends ItemToolWrench implements IPseudoDamageItem, IElectricItem, IItemHudInfo {
   public ItemToolWrenchElectric() {
      super(ItemName.electric_wrench);
      ItemDefaults.durability(this, 27);
      ItemDefaults.stackSize(this, 1);
      ItemDefaults.noRepair(this);
   }

   @Override
   public List<String> getHudInfo(ItemStack stack, boolean advanced) {
      List<String> info = new LinkedList<>();
      info.add(ElectricItem.manager.getToolTip(stack));
      return info;
   }

   @Override
   public boolean canTakeDamage(ItemStack stack, int amount) {
      amount *= 100;
      return ElectricItem.manager.getCharge(stack) >= amount;
   }

   @Override
   public void damage(ItemStack stack, int amount, Player player) {
      ElectricItem.manager.use(stack, 100 * amount, player);
   }

   @Override
   public boolean canProvideEnergy(ItemStack stack) {
      return false;
   }

   @Override
   public double getMaxCharge(ItemStack stack) {
      return 12000.0;
   }

   @Override
   public int getTier(ItemStack stack) {
      return 1;
   }

   @Override
   public double getTransferLimit(ItemStack stack) {
      return 250.0;
   }

   @Override
   public void func_150895_a(CreativeModeTab tab, NonNullList<ItemStack> subItems) {
      if (this.func_194125_a(tab)) {
         ElectricItemManager.addChargeVariants(this, subItems);
      }
   }

   @Override
   public boolean func_82789_a(ItemStack toRepair, ItemStack repair) {
      return false;
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
