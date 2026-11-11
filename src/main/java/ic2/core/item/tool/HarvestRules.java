package ic2.core.item.tool;

import ic2.core.item.ItemToolIC2;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class HarvestRules {
   private HarvestRules() {
   }

   public static boolean allows(ItemStack stack, HarvestRules.Requirement requirement) {
      if (stack.isEmpty()) {
         return false;
      } else if (stack.getItem() instanceof ItemToolIC2 tool) {
         return tool.harvestLevel(requirement.tool()) >= requirement.level();
      } else {
         String var6 = requirement.tool();

         BlockState effective = switch (var6) {
            case "pickaxe" -> Blocks.STONE.defaultBlockState();
            case "axe" -> Blocks.OAK_LOG.defaultBlockState();
            case "shovel" -> Blocks.DIRT.defaultBlockState();
            default -> null;
         };
         if (effective != null && stack.isCorrectToolForDrops(effective)) {
            BlockState probe = switch (requirement.level()) {
               case 0 -> Blocks.STONE.defaultBlockState();
               case 1 -> Blocks.IRON_ORE.defaultBlockState();
               case 2 -> Blocks.DIAMOND_ORE.defaultBlockState();
               default -> Blocks.OBSIDIAN.defaultBlockState();
            };
            return requirement.level() == 0 || stack.isCorrectToolForDrops(probe);
         } else {
            return false;
         }
      }
   }

   public record Requirement(String tool, int level) {
   }
}
