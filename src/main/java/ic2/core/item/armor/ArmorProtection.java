package ic2.core.item.armor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public interface ArmorProtection {
   ArmorProtection.ArmorProperties getProperties(LivingEntity var1, ItemStack var2, DamageSource var3, double var4, int var6);

   int getArmorDisplay(Player var1, ItemStack var2, int var3);

   void damageArmor(LivingEntity var1, ItemStack var2, DamageSource var3, int var4, int var5);

   public record ArmorProperties(int priority, double ratio, int maximum) {
   }

   @EventBusSubscriber(modid = "ic2")
   public static final class Handler {
      @SubscribeEvent(priority = EventPriority.LOWEST)
      public static void protect(LivingIncomingDamageEvent event) {
         if (!event.getEntity().level().isClientSide()) {
            List<ArmorProtection.Handler.Piece> pieces = new ArrayList<>();
            int index = 0;

            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
               ItemStack stack = event.getEntity().getItemBySlot(slot);
               if (stack.getItem() instanceof ArmorProtection armor) {
                  ArmorProtection.ArmorProperties p = armor.getProperties(event.getEntity(), stack, event.getSource(), event.getAmount(), index);
                  if (p.ratio() > 0.0 && p.maximum() > 0) {
                     pieces.add(new ArmorProtection.Handler.Piece(armor, stack, index, p));
                  }
               }

               index++;
            }

            pieces.sort(Comparator.<ArmorProtection.Handler.Piece>comparingInt(p -> p.properties.priority()).reversed());
            double remaining = event.getAmount();
            int start = 0;

            while (start < pieces.size()) {
               int end = start + 1;

               while (end < pieces.size() && pieces.get(end).properties.priority() == pieces.get(start).properties.priority()) {
                  end++;
               }

               List<ArmorProtection.Handler.Piece> group = pieces.subList(start, end);
               double absorbed = 0.0;
               double[] amounts = ArmorAbsorption.allocate(
                  remaining,
                  group.stream().mapToDouble(p -> p.properties.ratio()).toArray(),
                  group.stream().mapToDouble(p -> p.properties.maximum() / 25.0).toArray()
               );

               for (int i = 0; i < group.size(); i++) {
                  ArmorProtection.Handler.Piece piece = group.get(i);
                  double amount = amounts[i];
                  absorbed += amount;
                  if (amount > 0.0) {
                     piece.armor.damageArmor(event.getEntity(), piece.stack, event.getSource(), Math.max(1, (int)amount), piece.slot);
                  }
               }

               remaining = Math.max(0.0, remaining - absorbed);
               start = end;
            }

            event.setAmount((float)remaining);
         }
      }

      private record Piece(ArmorProtection armor, ItemStack stack, int slot, ArmorProtection.ArmorProperties properties) {
      }
   }
}
