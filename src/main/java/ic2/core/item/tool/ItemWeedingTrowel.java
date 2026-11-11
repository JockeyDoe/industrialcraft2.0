package ic2.core.item.tool;

import ic2.core.IC2;
import ic2.core.crop.IC2Crops;
import ic2.core.crop.TileEntityCrop;
import ic2.core.item.ItemDefaults;
import ic2.core.item.ItemIC2;
import ic2.core.item.type.CropResItemType;
import ic2.core.profile.NotClassic;
import ic2.core.ref.ItemName;
import ic2.core.util.StackUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

@NotClassic
public class ItemWeedingTrowel extends ItemIC2 {
   public ItemWeedingTrowel() {
      super(ItemName.weeding_trowel);
      ItemDefaults.stackSize(this, 1);
   }

   public InteractionResult onItemUseFirst(Player player, Level world, BlockPos pos, Direction side, float hitX, float hitY, float hitZ, InteractionHand hand) {
      if (!IC2.platform.isSimulating()) {
         return InteractionResult.PASS;
      } else if (world.getBlockEntity(pos) instanceof TileEntityCrop tileEntityCrop && tileEntityCrop.getCrop() == IC2Crops.weed) {
         StackUtil.dropAsEntity(world, pos, StackUtil.copyWithSize(ItemName.crop_res.getItemStack(CropResItemType.weed), tileEntityCrop.getCurrentSize()));
         tileEntityCrop.reset();
         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }
}
