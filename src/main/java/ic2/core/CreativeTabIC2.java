package ic2.core;

import ic2.core.block.BlockBase;
import ic2.core.init.NativeRegistration;
import ic2.core.item.ItemDefaults;
import ic2.core.item.ItemIC2;
import ic2.core.ref.BlockName;
import ic2.core.ref.ItemName;
import ic2.core.util.VanillaVariants;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class CreativeTabIC2 extends CreativeModeTab {
   private static ItemStack a;
   private static ItemStack b;
   private static ItemStack z;
   private int ticker;

   public CreativeTabIC2() {
      super(
         CreativeModeTab.builder().title(Component.translatable("itemGroup.IC2")).icon(() -> IC2.tabIC2.func_151244_d()).displayItems((parameters, output) -> {
            for (ItemName name : ItemName.values()) {
               if (name.hasInstance() && !ItemDefaults.isHidden(name.getInstance())) {
                  NonNullList<ItemStack> stacks = NonNullList.create();
                  if (name.getInstance() instanceof ItemIC2 item) {
                     item.func_150895_a(IC2.tabIC2, stacks);
                  } else {
                     stacks.add(name.getItemStack());
                  }

                  stacks.stream().filter(stack -> !stack.isEmpty()).forEach(output::accept);
               }
            }

            for (BlockName namex : BlockName.values()) {
               if (namex.hasInstance() && namex.getInstance() instanceof BlockBase block) {
                  NonNullList<ItemStack> stacks = NonNullList.create();
                  block.func_149666_a(IC2.tabIC2, stacks);
                  stacks.stream().filter(stack -> !stack.isEmpty()).forEach(output::accept);
               }
            }
         })
      );
      NativeRegistration.add(Registries.CREATIVE_MODE_TAB, IC2.getIdentifier("ic2"), this);
   }

   public ItemStack func_151244_d() {
      if (IC2.seasonal) {
         if (a == null) {
            a = new ItemStack(VanillaVariants.skull(2), 1);
         }

         if (b == null) {
            b = new ItemStack(VanillaVariants.skull(0), 1);
         }

         if (z == null) {
            z = ItemName.nano_chestplate.getItemStack();
         }

         if (++this.ticker >= 5000) {
            this.ticker = 0;
         }

         if (this.ticker >= 2500) {
            return this.ticker < 3000 ? a : (this.ticker < 4500 ? b : z);
         }
      }

      return ItemName.mining_laser.getItemStack();
   }

   public ItemStack func_78016_d() {
      return null;
   }
}
