package ic2.core.item.tool;

import ic2.core.IC2;
import ic2.core.profile.NotClassic;
import ic2.core.ref.ItemName;
import ic2.core.util.ItemStacks;
import ic2.core.util.StackUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments.Mutable;
import net.minecraft.world.level.Level;

@NotClassic
public class ItemDrillIridium extends ItemDrill {
   public ItemDrillIridium() {
      super(ItemName.iridium_drill, 800, HarvestLevel.Iridium, 300000, 1000, 3, 24.0F);
   }

   @Override
   protected ItemStack getItemStack(double charge) {
      ItemStack ret = super.getItemStack(charge);
      Mutable enchantmentMap = new Mutable(ItemEnchantments.EMPTY);
      enchantmentMap.set(ItemStacks.registries().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), 3);
      EnchantmentHelper.setEnchantments(ret, enchantmentMap.toImmutable());
      return ret;
   }

   @Override
   public InteractionResult use(Level world, Player player, InteractionHand hand) {
      if (!world.isClientSide() && IC2.keyboard.isModeSwitchKeyDown(player)) {
         Mutable enchantmentMap = new Mutable(ItemEnchantments.EMPTY);
         enchantmentMap.set(ItemStacks.registries().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), 3);
         ItemStack stack = StackUtil.get(player, hand);
         if (EnchantmentHelper.getItemEnchantmentLevel(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), stack)
            == 0) {
            enchantmentMap.set(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
            IC2.platform.messagePlayer(player, "ic2.tooltip.mode", "ic2.tooltip.mode.silkTouch");
         } else {
            IC2.platform.messagePlayer(player, "ic2.tooltip.mode", "ic2.tooltip.mode.normal");
         }

         EnchantmentHelper.setEnchantments(stack, enchantmentMap.toImmutable());
      }

      return super.use(world, player, hand);
   }

   @Override
   public InteractionResult func_180614_a(
      Player player, Level world, BlockPos pos, InteractionHand hand, Direction side, float xOffset, float yOffset, float zOffset
   ) {
      return (InteractionResult)(IC2.keyboard.isModeSwitchKeyDown(player)
         ? InteractionResult.PASS
         : super.func_180614_a(player, world, pos, hand, side, xOffset, yOffset, zOffset));
   }
}
