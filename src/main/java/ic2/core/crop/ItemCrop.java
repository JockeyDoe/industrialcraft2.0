package ic2.core.crop;

import ic2.api.item.IBoxable;
import ic2.core.block.TileEntityBlock;
import ic2.core.item.ItemIC2;
import ic2.core.item.block.ItemBlockTileEntity;
import ic2.core.ref.BlockName;
import ic2.core.ref.ItemName;
import ic2.core.ref.TeBlock;
import ic2.core.util.NativePlacement;
import ic2.core.util.StackUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;

public class ItemCrop extends ItemIC2 implements IBoxable {
   public ItemCrop() {
      super(ItemName.crop_stick);
   }

   @Override
   public InteractionResult func_180614_a(Player player, Level world, BlockPos pos, InteractionHand hand, Direction side, float hitX, float hitY, float hitZ) {
      if (!world.getBlockState(pos).canBeReplaced()) {
         pos = pos.relative(side);
      }

      ItemStack cropStickStack = StackUtil.get(player, hand);
      if (StackUtil.isEmpty(cropStickStack)) {
         return InteractionResult.PASS;
      } else if (world.getBlockState(pos.below()).getBlock() != Blocks.FARMLAND) {
         return InteractionResult.PASS;
      } else if (!player.mayUseItemAt(pos, side, cropStickStack)) {
         return InteractionResult.PASS;
      } else if (!NativePlacement.canPlace(BlockName.te.getInstance(), world, pos, side, player, hand, true)) {
         return InteractionResult.PASS;
      } else {
         TileEntityBlock tile = TileEntityBlock.instantiate(TeBlock.crop.getTeClass());
         if (ItemBlockTileEntity.placeTeBlock(cropStickStack, player, world, pos, side, tile)) {
            SoundType stepSound = SoundType.GRASS;
            world.playSound(
               null,
               pos.getX() + 0.5,
               pos.getY() + 0.5,
               pos.getZ() + 0.5,
               stepSound.getPlaceSound(),
               SoundSource.BLOCKS,
               (stepSound.getVolume() + 1.0F) / 2.0F,
               stepSound.getPitch() * 0.8F
            );
            StackUtil.consumeOrError(player, hand, 1);
            return InteractionResult.SUCCESS;
         } else {
            return InteractionResult.PASS;
         }
      }
   }

   @Override
   public boolean canBeStoredInToolbox(ItemStack itemStack) {
      return true;
   }
}
