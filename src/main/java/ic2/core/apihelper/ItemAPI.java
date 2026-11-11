package ic2.core.apihelper;

import ic2.api.item.IItemAPI;
import ic2.core.ref.BlockName;
import ic2.core.ref.FluidName;
import ic2.core.ref.IMultiBlock;
import ic2.core.ref.ItemName;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class ItemAPI implements IItemAPI {
   @Override
   public ItemStack getItemStack(String name, String variant) {
      if (name == null) {
         return null;
      } else {
         if (variant == null) {
            int idx = name.indexOf(35);
            if (idx != -1) {
               variant = name.substring(idx + 1);
               name = name.substring(0, idx);
            }
         }

         BlockName blockName = this.getBlockName(name);
         if (blockName != null) {
            return blockName.getItemStack(variant);
         } else {
            ItemName itemName = this.getItemName(name);
            return itemName != null ? itemName.getItemStack(variant) : null;
         }
      }
   }

   @Override
   public Block getBlock(String name) {
      if (name == null) {
         return null;
      } else {
         BlockName blockName = this.getBlockName(name);
         if (blockName != null) {
            return blockName.getInstance();
         } else {
            FluidName fluidName = this.getFluidName(name);
            return fluidName != null ? fluidName.getInstance().defaultFluidState().createLegacyBlock().getBlock() : null;
         }
      }
   }

   @Override
   public Item getItem(String name) {
      if (name == null) {
         return null;
      } else {
         ItemName itemName = this.getItemName(name);
         if (itemName != null) {
            return itemName.getInstance();
         } else {
            Block block = this.getBlock(name);
            if (block != null) {
               Item ret = Item.byBlock(block);
               if (ret != Items.AIR || block == Blocks.AIR) {
                  return ret;
               }
            }

            return null;
         }
      }
   }

   @Override
   public BlockState getBlockState(String name, String variant) {
      if (variant == null) {
         int idx = name.indexOf(35);
         if (idx != -1) {
            variant = name.substring(idx + 1);
            name = name.substring(0, idx);
         }
      }

      BlockName blockName = this.getBlockName(name);
      if (blockName != null) {
         Block block = blockName.getInstance();
         if (block instanceof IMultiBlock) {
            return ((IMultiBlock)block).getState(variant);
         } else {
            assert variant == null;

            return block.defaultBlockState();
         }
      } else {
         FluidName fluidName = this.getFluidName(name);
         if (fluidName != null) {
            assert variant == null;

            return fluidName.getInstance().defaultFluidState().createLegacyBlock().getBlock().defaultBlockState();
         } else {
            return null;
         }
      }
   }

   private ItemName getItemName(String itemName) {
      for (ItemName name : ItemName.values) {
         if (name.name().equalsIgnoreCase(itemName)) {
            return name;
         }
      }

      return null;
   }

   private BlockName getBlockName(String blockName) {
      for (BlockName name : BlockName.values) {
         if (name.name().equalsIgnoreCase(blockName)) {
            return name;
         }
      }

      return null;
   }

   private FluidName getFluidName(String fluidName) {
      for (FluidName name : FluidName.values) {
         if (name.name().equalsIgnoreCase(fluidName)) {
            return name;
         }
      }

      return null;
   }
}
