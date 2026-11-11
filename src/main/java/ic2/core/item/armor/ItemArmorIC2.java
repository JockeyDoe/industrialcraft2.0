package ic2.core.item.armor;

import ic2.api.item.IMetalArmor;
import ic2.core.IC2;
import ic2.core.init.Localization;
import ic2.core.item.ItemCapabilities;
import ic2.core.item.ItemIC2;
import ic2.core.item.RepairMatcher;
import ic2.core.model.ModelRegistry;
import ic2.core.profile.Version;
import ic2.core.ref.IItemModelProvider;
import ic2.core.ref.ItemName;
import ic2.core.util.Util;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.transfer.access.ItemAccess;

public class ItemArmorIC2 extends ItemIC2 implements IItemModelProvider, IMetalArmor, RepairMatcher {
   private final String armorName;
   protected final EquipmentSlot armorSlot;
   private final Object repairMaterial;

   public ItemArmorIC2(ItemName name, ArmorMaterial armorMaterial, String armorName, EquipmentSlot armorType, Object repairMaterial) {
      super(
         name,
         new Properties()
            .humanoidArmor(armorMaterial, armorType(armorType))
            .component(
               DataComponents.EQUIPPABLE,
               Equippable.builder(armorType)
                  .setEquipSound(armorMaterial.equipSound())
                  .setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID, IC2.getIdentifier(armorName)))
                  .build()
            )
      );
      this.repairMaterial = repairMaterial;
      this.armorName = armorName;
      this.armorSlot = armorType;
   }

   private static ArmorType armorType(EquipmentSlot slot) {
      return switch (slot) {
         case HEAD -> ArmorType.HELMET;
         case CHEST -> ArmorType.CHESTPLATE;
         case LEGS -> ArmorType.LEGGINGS;
         case FEET -> ArmorType.BOOTS;
         default -> throw new IllegalArgumentException("Not a humanoid armor slot: " + slot);
      };
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void registerModels(ItemName name) {
      ModelRegistry.setCustomModelResourceLocation(this, 0, ItemIC2.getModelLocation(name, null));
   }

   public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
      char suffix1 = (char)(this.armorSlot == EquipmentSlot.LEGS ? 50 : 49);
      String suffix2 = type != null && this.hasOverlayTexture() ? "_overlay" : "";
      return "ic2:textures/armor/" + this.armorName + "_" + suffix1 + suffix2 + ".png";
   }

   protected boolean hasOverlayTexture() {
      return false;
   }

   @Override
   public String func_77658_a() {
      return this.getDescriptionId();
   }

   @Override
   public String func_77667_c(ItemStack stack) {
      return this.func_77658_a();
   }

   @Override
   public String func_77657_g(ItemStack stack) {
      return this.func_77667_c(stack);
   }

   @Override
   public String func_77653_i(ItemStack stack) {
      return Localization.translate(this.func_77667_c(stack));
   }

   @Override
   protected boolean func_194125_a(CreativeModeTab tab) {
      return this.isEnabled() && tab == IC2.tabIC2;
   }

   @Override
   protected boolean isEnabled() {
      return Version.shouldEnable(this.getClass());
   }

   @Override
   public boolean isMetalArmor(ItemStack itemstack, Player player) {
      return true;
   }

   @Override
   public boolean func_82789_a(ItemStack toRepair, ItemStack repair) {
      Object material = this.repairMaterial instanceof Supplier<?> supplier ? supplier.get() : this.repairMaterial;
      return repair != null && Util.matchesOD(repair, material);
   }

   @Override
   public <T> void addCapability(ItemCapability<T, ItemAccess> cap, BiFunction<ItemStack, ItemAccess, T> lookup) {
      ItemCapabilities.add(this, cap, lookup);
   }
}
