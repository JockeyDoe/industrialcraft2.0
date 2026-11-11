package ic2.core.item.tool;

import com.google.common.base.Predicate;
import ic2.api.item.IEnhancedOverlayProvider;
import ic2.core.IC2;
import ic2.core.audio.AudioPosition;
import ic2.core.block.wiring.TileEntityCable;
import ic2.core.item.type.CraftingItemType;
import ic2.core.ref.ItemName;
import ic2.core.util.StackUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemToolCutter extends ItemToolCrafting implements IEnhancedOverlayProvider {
   public ItemToolCutter() {
      super(ItemName.cutter, 60);
   }

   @Override
   public InteractionResult func_180614_a(Player player, Level world, BlockPos pos, InteractionHand hand, Direction side, float hitX, float hitY, float hitZ) {
      if (world.getBlockEntity(pos) instanceof TileEntityCable cable) {
         Predicate<ItemStack> request = StackUtil.sameStack(ItemName.crafting.getItemStack(CraftingItemType.rubber));
         if (StackUtil.consumeFromPlayerInventory(player, request, 1, true) && cable.tryAddInsulation()) {
            StackUtil.consumeFromPlayerInventory(player, request, 1, false);
            StackUtil.damageOrError(player, hand, 1);
            return InteractionResult.SUCCESS;
         }
      }

      return InteractionResult.PASS;
   }

   public boolean removeInsulation(Player player, InteractionHand hand, TileEntityCable cable) {
      if (cable.tryRemoveInsulation(true) && StackUtil.damage(player, hand, StackUtil.sameItem(this), 3)) {
         cable.tryRemoveInsulation(false);
         if (cable.getLevel().isClientSide()) {
            IC2.audioManager.playOnce(new AudioPosition(cable.getLevel(), cable.getBlockPos()), "Tools/InsulationCutters.ogg");
         } else {
            StackUtil.dropAsEntity(cable.getLevel(), cable.getBlockPos(), ItemName.crafting.getItemStack(CraftingItemType.rubber));
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean providesEnhancedOverlay(Level world, BlockPos pos, Direction side, Player player, ItemStack stack) {
      return false;
   }
}
