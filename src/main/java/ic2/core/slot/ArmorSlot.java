package ic2.core.slot;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlot.Type;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class ArmorSlot {
   private static final EquipmentSlot[] armorSlots = getArmorSlots();
   private static final List<EquipmentSlot> armorSlotList = Collections.unmodifiableList(Arrays.asList(armorSlots));

   public static EquipmentSlot get(int index) {
      return armorSlots[index];
   }

   public static int getCount() {
      return armorSlots.length;
   }

   public static Iterable<EquipmentSlot> getAll() {
      return armorSlotList;
   }

   public static List<ItemStack> getEquipment(final Inventory inventory) {
      return new AbstractList<ItemStack>() {
         @Override
         public int size() {
            return ArmorSlot.armorSlots.length;
         }

         public ItemStack get(int index) {
            return inventory.player.getItemBySlot(ArmorSlot.armorSlots[index]);
         }

         public ItemStack set(int index, ItemStack stack) {
            ItemStack old = this.get(index);
            inventory.player.setItemSlot(ArmorSlot.armorSlots[index], stack);
            return old;
         }
      };
   }

   private static EquipmentSlot[] getArmorSlots() {
      EquipmentSlot[] values = EquipmentSlot.values();
      int count = 0;

      for (EquipmentSlot slot : values) {
         if (slot.getType() == Type.HUMANOID_ARMOR) {
            count++;
         }
      }

      EquipmentSlot[] ret = new EquipmentSlot[count];

      for (int i = 0; i < ret.length; i++) {
         for (EquipmentSlot slotx : values) {
            if (slotx.getType() == Type.HUMANOID_ARMOR && slotx.getIndex() == i) {
               ret[i] = slotx;
               break;
            }
         }
      }

      for (int i = 0; i < ret.length; i++) {
         if (ret[i] == null) {
            throw new RuntimeException("Can't find an armor mapping for idx " + i);
         }
      }

      return ret;
   }
}
