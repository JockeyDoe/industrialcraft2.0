package ic2.core.item;

import ic2.api.item.ICustomDamageItem;
import ic2.core.IC2;
import ic2.core.ref.ItemName;
import ic2.core.util.ItemData;
import ic2.core.util.LogCategory;
import ic2.core.util.StackUtil;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ItemGradualInt extends ItemIC2 implements ICustomDamageItem {
   private static final boolean alwaysShowDurability = true;
   private static final String nbtKey = "advDmg";
   private final int maxDamage;

   public ItemGradualInt(ItemName name, int maxDamage) {
      super(name);
      ItemDefaults.noRepair(this);
      this.maxDamage = maxDamage;
   }

   public boolean showDurabilityBar(ItemStack stack) {
      return true;
   }

   public double getDurabilityForDisplay(ItemStack stack) {
      return (double)this.getCustomDamage(stack) / this.getMaxCustomDamage(stack);
   }

   public boolean func_77645_m() {
      return true;
   }

   public boolean isDamaged(ItemStack stack) {
      return this.getCustomDamage(stack) > 0;
   }

   public int getDamage(ItemStack stack) {
      return this.getCustomDamage(stack);
   }

   @Override
   public int getCustomDamage(ItemStack stack) {
      return !ItemData.has(stack) ? 0 : ItemData.read(stack).getIntOr("advDmg", 0);
   }

   public int getMaxDamage(ItemStack stack) {
      return this.getMaxCustomDamage(stack);
   }

   @Override
   public int getMaxCustomDamage(ItemStack stack) {
      return this.maxDamage;
   }

   public void setDamage(ItemStack stack, int damage) {
      int prev = this.getCustomDamage(stack);
      if (damage != prev && BaseElectricItem.logIncorrectItemDamaging) {
         IC2.log.warn(LogCategory.Armor, new Throwable(), "Detected invalid gradual item damage application (%d):", damage - prev);
      }
   }

   @Override
   public void setCustomDamage(ItemStack stack, int damage) {
      CompoundTag nbt = StackUtil.getOrCreateNbtData(stack);
      nbt.putInt("advDmg", damage);
      ItemData.commit(stack, nbt);
   }

   @Override
   public boolean applyCustomDamage(ItemStack stack, int damage, LivingEntity src) {
      this.setCustomDamage(stack, this.getCustomDamage(stack) + damage);
      return true;
   }

   @Override
   public void func_150895_a(CreativeModeTab tab, NonNullList<ItemStack> subItems) {
      if (this.func_194125_a(tab)) {
         ItemStack stack = new ItemStack(this);
         this.setCustomDamage(stack, 0);
         subItems.add(stack);
      }
   }
}
