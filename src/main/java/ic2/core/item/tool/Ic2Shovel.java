package ic2.core.item.tool;

import ic2.core.init.Localization;
import ic2.core.item.ItemIC2;
import ic2.core.item.RepairMatcher;
import ic2.core.ref.IItemModelProvider;
import ic2.core.ref.ItemName;
import ic2.core.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.Item.Properties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class Ic2Shovel extends ItemIC2 implements IItemModelProvider, RepairMatcher {
   private final Object repairMaterial = "ingotBronze";

   public Ic2Shovel(ToolMaterial material) {
      super(ItemName.bronze_shovel, new Properties().shovel(material, 1.5F, -3.0F));
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void registerModels(ItemName name) {
      ItemIC2.registerModel(this, 0, name, null);
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
   public boolean func_82789_a(ItemStack stack1, ItemStack stack2) {
      return stack2 != null && Util.matchesOD(stack2, this.repairMaterial);
   }
}
