package ic2.core.util;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class ItemTransfers {
   public static ItemStack get(ResourceHandler<ItemResource> handler, int slot) {
      return ((ItemResource)handler.getResource(slot)).toStack(handler.getAmountAsInt(slot));
   }

   public static ItemStack insert(ResourceHandler<ItemResource> handler, int slot, ItemStack stack, boolean simulate) {
      if (stack.isEmpty()) {
         return ItemStack.EMPTY;
      } else {
         Transaction transaction = Transaction.open(null);

         ItemStack var6;
         try {
            int inserted = handler.insert(slot, ItemResource.of(stack), stack.getCount(), transaction);
            if (!simulate) {
               transaction.commit();
            }

            var6 = stack.copyWithCount(stack.getCount() - inserted);
         } catch (Throwable var8) {
            if (transaction != null) {
               try {
                  transaction.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (transaction != null) {
            transaction.close();
         }

         return var6;
      }
   }

   public static ItemStack extract(ResourceHandler<ItemResource> handler, int slot, int amount, boolean simulate) {
      ItemResource resource = (ItemResource)handler.getResource(slot);
      if (!resource.isEmpty() && amount > 0) {
         Transaction transaction = Transaction.open(null);

         ItemStack var7;
         try {
            int extracted = handler.extract(slot, resource, amount, transaction);
            if (!simulate) {
               transaction.commit();
            }

            var7 = resource.toStack(extracted);
         } catch (Throwable var9) {
            if (transaction != null) {
               try {
                  transaction.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (transaction != null) {
            transaction.close();
         }

         return var7;
      } else {
         return ItemStack.EMPTY;
      }
   }
}
