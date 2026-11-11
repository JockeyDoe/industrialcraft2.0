package ic2.core.init;

import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;

public final class NativeMaterials {
   public static final TagKey<Item> BRONZE_REPAIR = TagKey.create(Registries.ITEM, Identifier.parse("ic2:bronze_repair"));
   public static final TagKey<Item> ALLOY_REPAIR = TagKey.create(Registries.ITEM, Identifier.parse("ic2:alloy_repair"));
   public static final ArmorMaterial BRONZE_ARMOR = armor("bronze", 15, 2, 5, 6, 2, 9, 0.0F, BRONZE_REPAIR);
   public static final ArmorMaterial ALLOY_ARMOR = armor("alloy", 50, 4, 7, 9, 4, 12, 2.0F, ALLOY_REPAIR);
   public static final ToolMaterial BRONZE_TOOL = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 350, 6.0F, 2.0F, 13, BRONZE_REPAIR);

   private NativeMaterials() {
   }

   private static ArmorMaterial armor(
      String id, int durability, int boots, int legs, int chest, int helmet, int enchantability, float toughness, TagKey<Item> repair
   ) {
      return new ArmorMaterial(
         durability,
         Map.of(ArmorType.BOOTS, boots, ArmorType.LEGGINGS, legs, ArmorType.CHESTPLATE, chest, ArmorType.HELMET, helmet),
         enchantability,
         SoundEvents.ARMOR_EQUIP_IRON,
         toughness,
         0.0F,
         repair,
         ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath("ic2", id))
      );
   }
}
