package ic2.core.item.tool;

import ic2.api.item.IBoxable;
import ic2.api.item.IItemHudInfo;
import ic2.core.init.Localization;
import ic2.core.item.CraftingDurability;
import ic2.core.item.ItemDefaults;
import ic2.core.item.ItemIC2;
import ic2.core.ref.ItemName;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public abstract class ItemToolCrafting extends ItemIC2 implements IBoxable, IItemHudInfo {
   public ItemToolCrafting(ItemName name, int maximumUses) {
      super(name);
      ItemDefaults.durability(this, maximumUses - 1);
      ItemDefaults.stackSize(this, 1);
      ItemDefaults.noRepair(this);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void func_77624_a(ItemStack stack, Level world, List<String> tooltip, TooltipFlag advanced) {
      tooltip.add(Localization.translate("ic2.item.ItemTool.tooltip.UsesLeft", getRemainingUses(stack)));
   }

   @Override
   public boolean canBeStoredInToolbox(ItemStack itemstack) {
      return true;
   }

   @Override
   public List<String> getHudInfo(ItemStack stack, boolean advanced) {
      List<String> info = new LinkedList<>();
      info.add(Localization.translate("ic2.item.ItemTool.tooltip.UsesLeft", getRemainingUses(stack)));
      return info;
   }

   @Override
   public boolean hasContainerItem(ItemStack stack) {
      return true;
   }

   @Override
   public ItemStack getContainerItem(ItemStack stack) {
      MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
      return CraftingDurability.remainder(stack, server == null ? null : server.overworld());
   }
}
