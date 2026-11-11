package ic2.core.item.armor;

import ic2.core.profile.NotExperimental;
import ic2.core.ref.ItemName;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@NotExperimental
public class ItemArmorLappack extends ItemArmorElectric {
   public ItemArmorLappack() {
      super(ItemName.lappack, "lappack", EquipmentSlot.CHEST, 2.0E7, 2500.0, 4);
   }

   @Override
   public boolean canProvideEnergy(ItemStack stack) {
      return true;
   }

   @Override
   public double getDamageAbsorptionRatio() {
      return 0.0;
   }

   @Override
   public int getEnergyPerDamage() {
      return 0;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Rarity func_77613_e(ItemStack stack) {
      return Rarity.UNCOMMON;
   }
}
