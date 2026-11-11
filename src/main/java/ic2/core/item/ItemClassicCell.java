package ic2.core.item;

import ic2.core.crop.TileEntityCrop;
import ic2.core.init.Localization;
import ic2.core.item.type.CellType;
import ic2.core.ref.ItemName;
import ic2.core.util.StackUtil;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ItemClassicCell extends ItemMulti<CellType> {
   public ItemClassicCell() {
      super(ItemName.cell, CellType.class);
   }

   public InteractionResult onItemUseFirst(Player player, Level world, BlockPos pos, Direction side, float hitX, float hitY, float hitZ, InteractionHand hand) {
      ItemStack stack = StackUtil.get(player, hand);
      CellType type = this.getType(stack);
      if (type.hasCropAction()) {
         BlockEntity te = world.getBlockEntity(pos);
         if (te instanceof TileEntityCrop) {
            return type.doCropAction(stack, result -> StackUtil.set(player, hand, result), (TileEntityCrop)te, true);
         }
      }

      return InteractionResult.PASS;
   }

   public int getItemStackLimit(ItemStack stack) {
      CellType type = this.getType(stack);
      return type != null ? type.getStackSize() : 0;
   }

   public boolean showDurabilityBar(ItemStack stack) {
      return this.getType(stack).getUsage(stack) > 0;
   }

   public double getDurabilityForDisplay(ItemStack stack) {
      CellType type = this.getType(stack);
      return (double)type.getUsage(stack) / type.getMaximum(stack);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void func_77624_a(ItemStack stack, Level world, List<String> tooltip, TooltipFlag advanced) {
      CellType type = this.getType(stack);
      if (type.getStackSize() == 1 && advanced.isAdvanced()) {
         int max = type.getMaximum(stack);
         tooltip.add(Localization.translate("item.durability", max - type.getUsage(stack), max));
      }
   }
}
