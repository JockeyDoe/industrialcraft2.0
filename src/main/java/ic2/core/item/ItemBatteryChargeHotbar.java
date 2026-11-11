package ic2.core.item;

import ic2.api.item.ElectricItem;
import ic2.api.item.IBoxable;
import ic2.core.IC2;
import ic2.core.init.Localization;
import ic2.core.item.tool.GuiToolbox;
import ic2.core.profile.NotClassic;
import ic2.core.ref.ItemName;
import ic2.core.util.ItemData;
import ic2.core.util.StackUtil;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@NotClassic
public class ItemBatteryChargeHotbar extends ItemBattery implements IBoxable {
   public ItemBatteryChargeHotbar(ItemName name, double maxCharge, double transferLimit, int tier) {
      super(name, maxCharge, transferLimit, tier);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void func_77624_a(ItemStack stack, Level world, List<String> list, TooltipFlag advanced) {
      super.func_77624_a(stack, world, list, advanced);
      ItemBatteryChargeHotbar.Mode mode = this.getMode(stack);
      list.add(this.getNameOfMode(mode));
      if (Minecraft.getInstance().gui.screen() instanceof GuiToolbox) {
         list.add((mode.enabled ? ChatFormatting.RED : ChatFormatting.GREEN) + Localization.translate("ic2.tooltip.mode.boxable"));
      }
   }

   public void func_77663_a(ItemStack stack, Level world, Entity entity, int itemSlot, boolean isSelected) {
      ItemBatteryChargeHotbar.Mode mode = this.getMode(stack);
      if (entity instanceof ServerPlayer && world.getGameTime() % 10L < this.getTier(stack) && mode.enabled) {
         Player thePlayer = (Player)entity;
         List<ItemStack> inventory = thePlayer.getInventory().getNonEquipmentItems();
         double limit = this.getTransferLimit(stack);
         int tier = this.getTier(stack);

         for (int i = 0; i < 9 && limit > 0.0; i++) {
            ItemStack toCharge = inventory.get(i);
            if (!StackUtil.isEmpty(toCharge)
               && (mode != ItemBatteryChargeHotbar.Mode.NOT_IN_HAND || i != thePlayer.getInventory().getSelectedSlot())
               && !(toCharge.getItem() instanceof ItemBatteryChargeHotbar)) {
               double charge = ElectricItem.manager.charge(toCharge, limit, tier, false, true);
               charge = ElectricItem.manager.discharge(stack, charge, tier, true, false, false);
               ElectricItem.manager.charge(toCharge, charge, tier, true, false);
               limit -= charge;
            }
         }
      }
   }

   @Override
   public InteractionResult use(Level world, Player player, InteractionHand hand) {
      ItemStack stack = StackUtil.get(player, hand);
      if (world.isClientSide()) {
         return InteractionResult.PASS;
      } else {
         ItemBatteryChargeHotbar.Mode mode = this.getMode(stack);
         mode = ItemBatteryChargeHotbar.Mode.values[(mode.ordinal() + 1) % ItemBatteryChargeHotbar.Mode.values.length];
         this.setMode(stack, mode);
         IC2.platform.messagePlayer(player, Localization.translate("ic2.tooltip.mode", this.getNameOfMode(mode)));
         return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
      }
   }

   private String getNameOfMode(ItemBatteryChargeHotbar.Mode mode) {
      return Localization.translate("ic2.tooltip.mode." + mode.toString().toLowerCase(Locale.ENGLISH));
   }

   public void setMode(ItemStack stack, ItemBatteryChargeHotbar.Mode mode) {
      CompoundTag nbt = StackUtil.getOrCreateNbtData(stack);
      nbt.putByte("mode", (byte)mode.ordinal());
      ItemData.commit(stack, nbt);
   }

   public ItemBatteryChargeHotbar.Mode getMode(ItemStack stack) {
      CompoundTag nbt = StackUtil.getOrCreateNbtData(stack);
      return !nbt.contains("mode") ? ItemBatteryChargeHotbar.Mode.ENABLED : this.getMode(nbt.getByteOr("mode", (byte)0));
   }

   private ItemBatteryChargeHotbar.Mode getMode(int mode) {
      if (mode < 0 || mode >= ItemBatteryChargeHotbar.Mode.values.length) {
         mode = 0;
      }

      return ItemBatteryChargeHotbar.Mode.values[mode];
   }

   @Override
   public boolean canBeStoredInToolbox(ItemStack itemstack) {
      return this.getMode(itemstack) == ItemBatteryChargeHotbar.Mode.DISABLED;
   }

   private static enum Mode {
      ENABLED(true),
      DISABLED(false),
      NOT_IN_HAND(true);

      private boolean enabled;
      public static final ItemBatteryChargeHotbar.Mode[] values = values();

      private Mode(boolean enabled) {
         this.enabled = enabled;
      }
   }
}
