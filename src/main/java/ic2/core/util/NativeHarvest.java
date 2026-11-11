package ic2.core.util;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

public final class NativeHarvest {
   private NativeHarvest() {
   }

   public static ItemStack tool(ServerLevel level, int fortune, boolean silk) {
      ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
      Registry<Enchantment> registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
      if (fortune > 0) {
         tool.enchant(registry.getOrThrow(Enchantments.FORTUNE), fortune);
      }

      if (silk) {
         tool.enchant(registry.getOrThrow(Enchantments.SILK_TOUCH), 1);
      }

      return tool;
   }
}
