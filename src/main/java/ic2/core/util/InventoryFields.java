package ic2.core.util;

import java.lang.reflect.Field;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

public interface InventoryFields extends ContainerData {
   int func_174887_a_(int var1);

   void func_174885_b(int var1, int var2);

   int func_174890_g();

   default int get(int id) {
      return this.func_174887_a_(id);
   }

   default void set(int id, int value) {
      this.func_174885_b(id, value);
   }

   default int getCount() {
      return this.func_174890_g();
   }

   static ContainerData of(Container inventory) {
      if (inventory instanceof ContainerData data) {
         return data;
      } else {
         for (Class<?> type = inventory.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
               if (ContainerData.class.isAssignableFrom(field.getType())) {
                  try {
                     field.setAccessible(true);
                     return (ContainerData)field.get(inventory);
                  } catch (ReflectiveOperationException var7) {
                     throw new IllegalStateException("Cannot access inventory menu fields", var7);
                  }
               }
            }
         }

         return new SimpleContainerData(0);
      }
   }
}
