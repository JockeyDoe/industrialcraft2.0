package ic2.core.item.block;

import ic2.core.block.BlockScaffold;
import ic2.core.block.TileEntityBarrel;
import ic2.core.init.Localization;
import ic2.core.item.ItemBooze;
import ic2.core.item.ItemDefaults;
import ic2.core.item.ItemIC2;
import ic2.core.model.ModelKey;
import ic2.core.model.ModelRegistry;
import ic2.core.ref.BlockName;
import ic2.core.ref.ItemName;
import ic2.core.util.ItemStacks;
import ic2.core.util.StackUtil;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ItemBarrel extends ItemIC2 {
   public ItemBarrel() {
      super(ItemName.barrel);
      ItemDefaults.stackSize(this, 1);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void registerModels(final ItemName name) {
      ModelRegistry.setCustomMeshDefinition(this, new ModelRegistry.ItemMeshDefinition() {
         {
            Objects.requireNonNull(ItemBarrel.this);
         }

         @Override
         public ModelKey func_178113_a(ItemStack stack) {
            return ItemIC2.getModelLocation(name, null);
         }
      });
      ModelRegistry.registerItemVariants(this, new ModelKey[]{getModelLocation(name, null)});
   }

   @Override
   public Component getName(ItemStack stack) {
      int amount = ItemBooze.getAmountOfValue(ItemStacks.rawMeta(stack));
      return amount > 0
         ? Component.literal(Integer.toString(amount)).append(Component.translatable("ic2.item.LBoozeBarrel"))
         : Component.translatable("ic2.item.EmptyBoozeBarrel");
   }

   @Override
   public String func_77653_i(ItemStack itemstack) {
      int v = ItemBooze.getAmountOfValue(ItemStacks.rawMeta(itemstack));
      return v > 0 ? v + Localization.translate("ic2.item.LBoozeBarrel") : Localization.translate("ic2.item.EmptyBoozeBarrel");
   }

   @Override
   public InteractionResult func_180614_a(Player player, Level world, BlockPos pos, InteractionHand hand, Direction side, float a, float b, float c) {
      ItemStack stack = StackUtil.get(player, hand);
      if (world.getBlockState(pos) == BlockName.scaffold.getBlockState(BlockScaffold.ScaffoldType.wood)
         && ItemBlockTileEntity.placeTeBlock(stack, player, world, pos, side, new TileEntityBarrel(ItemStacks.rawMeta(stack)))) {
         StackUtil.consumeOrError(player, hand, 1);
         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }
}
