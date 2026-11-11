package ic2.core.item;

import ic2.core.init.Localization;
import ic2.core.profile.NotClassic;
import ic2.core.ref.ItemName;
import ic2.core.util.ItemData;
import ic2.core.util.ItemStacks;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import ic2.core.uu.UuIndex;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@NotClassic
public class ItemCrystalMemory extends ItemIC2 {
   public ItemCrystalMemory() {
      super(ItemName.crystal_memory);
      ItemDefaults.stackSize(this, 1);
   }

   public boolean isRepairable() {
      return false;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void func_77624_a(ItemStack stack, Level world, List<String> tooltip, TooltipFlag advanced) {
      ItemStack recorded = this.readItemStack(stack);
      if (!StackUtil.isEmpty(recorded)) {
         tooltip.add(Localization.translate("ic2.item.CrystalMemory.tooltip.Item") + " " + recorded.getDisplayName());
         tooltip.add(
            Localization.translate("ic2.item.CrystalMemory.tooltip.UU-Matter") + " " + Util.toSiString(UuIndex.instance.getInBuckets(recorded), 4) + "B"
         );
      } else {
         tooltip.add(Localization.translate("ic2.item.CrystalMemory.tooltip.Empty"));
      }
   }

   public ItemStack readItemStack(ItemStack stack) {
      CompoundTag nbt = StackUtil.getOrCreateNbtData(stack);
      CompoundTag contentTag = nbt.getCompoundOrEmpty("Pattern");
      return ItemStacks.read(contentTag);
   }

   public void writecontentsTag(ItemStack stack, ItemStack recorded) {
      CompoundTag nbt = StackUtil.getOrCreateNbtData(stack);
      CompoundTag contentTag = new CompoundTag();
      ItemStacks.write(recorded, contentTag);
      nbt.put("Pattern", contentTag);
      ItemData.commit(stack, nbt);
   }
}
