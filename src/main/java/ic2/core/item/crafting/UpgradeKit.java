package ic2.core.item.crafting;

import ic2.core.IC2;
import ic2.core.block.NativeBlockEntities;
import ic2.core.block.TileEntityBlock;
import ic2.core.block.wiring.TileEntityChargepadMFE;
import ic2.core.block.wiring.TileEntityChargepadMFSU;
import ic2.core.block.wiring.TileEntityElectricBlock;
import ic2.core.block.wiring.TileEntityElectricMFE;
import ic2.core.block.wiring.TileEntityElectricMFSU;
import ic2.core.init.Localization;
import ic2.core.item.ItemMulti;
import ic2.core.item.type.UpdateKitType;
import ic2.core.ref.ItemName;
import ic2.core.util.StackUtil;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class UpgradeKit extends ItemMulti<UpdateKitType> {
   public UpgradeKit() {
      super(ItemName.upgrade_kit, UpdateKitType.class);
   }

   public InteractionResult onItemUseFirst(Player player, Level world, BlockPos pos, Direction side, float hitX, float hitY, float hitZ, InteractionHand hand) {
      if (!IC2.platform.isSimulating()) {
         return InteractionResult.PASS;
      } else {
         UpdateKitType type = this.getType(StackUtil.get(player, hand));
         if (type == null) {
            return InteractionResult.PASS;
         } else {
            boolean ret = false;
            switch (type) {
               case mfsu:
                  ret = upgradeToMfsu(world, pos);
               default:
                  if (!ret) {
                     return InteractionResult.PASS;
                  } else {
                     StackUtil.consumeOrError(player, hand, 1);
                     return InteractionResult.SUCCESS;
                  }
            }
         }
      }
   }

   private static boolean upgradeToMfsu(Level world, BlockPos pos) {
      BlockEntity te = world.getBlockEntity(pos);
      if (!(te instanceof TileEntityBlock)) {
         return false;
      } else {
         TileEntityElectricBlock replacement = null;
         if (te instanceof TileEntityElectricMFE) {
            replacement = new TileEntityElectricMFSU();
         } else if (te instanceof TileEntityChargepadMFE) {
            replacement = new TileEntityChargepadMFSU();
         }

         if (replacement != null) {
            CompoundTag nbt = new CompoundTag();
            ((TileEntityBlock)te).saveAdditional(nbt);
            replacement.loadAdditional(nbt);
            replacement = (TileEntityElectricBlock)NativeBlockEntities.at(replacement, pos);
            world.setBlockEntity(replacement);
            replacement.onUpgraded();
            replacement.setChanged();
            return true;
         } else {
            return false;
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void func_77624_a(ItemStack stack, Level world, List<String> tooltip, TooltipFlag advanced) {
      UpdateKitType type = this.getType(stack);
      if (type != null) {
         switch (type) {
            case mfsu:
               tooltip.add(Localization.translate("ic2.upgrade_kit.mfsu.info"));
         }
      }
   }
}
