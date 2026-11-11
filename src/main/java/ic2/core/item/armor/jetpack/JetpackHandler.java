package ic2.core.item.armor.jetpack;

import ic2.api.item.ElectricItem;
import ic2.api.item.IBackupElectricItemManager;
import ic2.api.item.IElectricItem;
import ic2.core.IC2;
import ic2.core.init.Localization;
import ic2.core.ref.ItemName;
import ic2.core.util.ItemData;
import ic2.core.util.ItemStacks;
import ic2.core.util.StackUtil;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre;

public class JetpackHandler implements IBackupElectricItemManager {
   private static Map<Player, JetpackHandler.ArmorSnapshot> playerArmorBuffer = new WeakHashMap<>();
   private boolean internalHandlesCheck = false;
   static final ItemStack jetpack = ItemName.jetpack_electric.getItemStack();
   public static JetpackHandler instance;

   private JetpackHandler() {
      NeoForge.EVENT_BUS.register(this);
      ElectricItem.registerBackupManager(this);
   }

   public static void init() {
      if (!IC2.initialized) {
         throw new IllegalStateException();
      } else {
         instance = new JetpackHandler();
      }
   }

   public static void setJetpackAttached(ItemStack stack, boolean value) {
      if (stack != null) {
         if (!value) {
            if (!ItemData.has(stack)) {
               return;
            }

            ItemData.remove(stack, "hasIC2Jetpack");
            if (ItemData.read(stack).isEmpty()) {
               ItemData.store(stack, null);
            }
         } else if (ItemStacks.equipmentSlot(stack) == EquipmentSlot.CHEST) {
            ItemData.put(stack, "hasIC2Jetpack", ByteTag.valueOf(true));
         }
      }
   }

   public static boolean hasJetpackAttached(ItemStack stack) {
      return stack != null
         && ItemStacks.equipmentSlot(stack) == EquipmentSlot.CHEST
         && ItemData.has(stack)
         && ItemData.read(stack).getBooleanOr("hasIC2Jetpack", false);
   }

   public static boolean hasJetpack(ItemStack stack) {
      return stack != null && (hasJetpackAttached(stack) || stack.getItem() instanceof IJetpack);
   }

   public static IJetpack getJetpack(ItemStack stack) {
      assert hasJetpack(stack);

      return stack.getItem() instanceof IJetpack ? (IJetpack)stack.getItem() : (IJetpack)jetpack.getItem();
   }

   public static double getTransferLimit() {
      return ((IElectricItem)jetpack.getItem()).getTransferLimit(jetpack);
   }

   @Override
   public double charge(ItemStack stack, double amount, int tier, boolean ignoreTransferLimit, boolean simulate) {
      if (this.getTier(stack) > tier) {
         return 0.0;
      } else {
         if (!ignoreTransferLimit) {
            amount = Math.min(amount, getTransferLimit());
         }

         double charge = ItemData.has(stack) ? ItemData.read(stack).getDoubleOr("charge", 0.0) : 0.0;
         amount = Math.min(amount, this.getMaxCharge(stack) - charge);
         if (!simulate) {
            ItemData.put(stack, "charge", DoubleTag.valueOf(charge + amount));
         }

         return amount;
      }
   }

   @Override
   public double discharge(ItemStack stack, double amount, int tier, boolean ignoreTransferLimit, boolean externally, boolean simulate) {
      if (!externally && this.getTier(stack) <= tier && ItemData.has(stack)) {
         if (!ignoreTransferLimit) {
            amount = Math.min(amount, getTransferLimit());
         }

         double charge = ItemData.read(stack).getDoubleOr("charge", 0.0);
         amount = Math.min(amount, charge);
         if (!simulate) {
            charge -= amount;
            if (charge == 0.0) {
               ItemData.remove(stack, "charge");
               if (ItemData.read(stack).isEmpty()) {
                  ItemData.store(stack, null);
               }
            } else {
               ItemData.put(stack, "charge", DoubleTag.valueOf(charge));
            }
         }

         return amount;
      } else {
         return 0.0;
      }
   }

   @Override
   public double getCharge(ItemStack stack) {
      return this.discharge(stack, Double.MAX_VALUE, Integer.MAX_VALUE, true, false, true);
   }

   @Override
   public double getMaxCharge(ItemStack stack) {
      return ElectricItem.manager.getMaxCharge(jetpack.copy());
   }

   @Override
   public boolean canUse(ItemStack stack, double amount) {
      return ElectricItem.rawManager.canUse(stack, amount);
   }

   @Override
   public boolean use(ItemStack stack, double amount, LivingEntity entity) {
      return ElectricItem.rawManager.use(stack, amount, entity);
   }

   @Override
   public void chargeFromArmor(ItemStack stack, LivingEntity entity) {
   }

   @Override
   public String getToolTip(ItemStack stack) {
      return ElectricItem.rawManager.getToolTip(stack);
   }

   @Override
   public int getTier(ItemStack stack) {
      return ElectricItem.manager.getTier(jetpack.copy());
   }

   @Override
   public synchronized boolean handles(ItemStack stack) {
      if (this.internalHandlesCheck) {
         return false;
      } else {
         this.internalHandlesCheck = true;

         boolean var2;
         try {
            var2 = hasJetpackAttached(stack) && ElectricItem.manager.getMaxCharge(stack) <= 0.0;
         } finally {
            this.internalHandlesCheck = false;
         }

         return var2;
      }
   }

   @SubscribeEvent
   public void tick(Pre event) {
      ItemStack stack = event.getEntity().getItemBySlot(EquipmentSlot.CHEST);
      if (hasJetpack(stack)) {
         JetpackLogic.onArmorTick(event.getEntity().level(), event.getEntity(), stack, getJetpack(stack));
      }

      if (playerArmorBuffer.containsKey(event.getEntity())) {
         JetpackHandler.ArmorSnapshot snapshot = playerArmorBuffer.get(event.getEntity());
         ItemStack lastStack = snapshot.saved();
         if (StackUtil.isEmpty(snapshot.original()) && hasJetpackAttached(lastStack) && StackUtil.isEmpty(stack)) {
            ItemStack newJetpack = jetpack.copy();
            double oldCharge = ElectricItem.manager.getCharge(lastStack);
            ElectricItem.manager.charge(newJetpack, oldCharge, Integer.MAX_VALUE, true, false);
            event.getEntity().setItemSlot(EquipmentSlot.CHEST, newJetpack);
         }

         playerArmorBuffer.remove(event.getEntity());
      }
   }

   @OnlyIn(Dist.CLIENT)
   @SubscribeEvent(priority = EventPriority.HIGH)
   public void tooltip(ItemTooltipEvent event) {
      if (hasJetpackAttached(event.getItemStack())) {
         event.getToolTip().add(Component.literal(Localization.translate("ic2.jetpackAttached")).withStyle(ChatFormatting.YELLOW));
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
   public void livingAttack(LivingIncomingDamageEvent event) {
      if (event.getEntity() instanceof Player && event.getSource() != null && !event.getSource().is(DamageTypeTags.BYPASSES_ARMOR)) {
         Player player = (Player)event.getEntity();
         ItemStack currentArmor = player.getItemBySlot(EquipmentSlot.CHEST);
         if (hasJetpackAttached(currentArmor) && !player.level().isClientSide()) {
            playerArmorBuffer.put(player, new JetpackHandler.ArmorSnapshot(currentArmor, currentArmor.copy()));
         }
      }
   }

   private record ArmorSnapshot(ItemStack original, ItemStack saved) {
   }
}
