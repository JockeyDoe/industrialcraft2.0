package ic2.core.util;

import ic2.core.item.ItemCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class FluidTransfer {
   public final ResourceHandler<FluidResource> handler;
   private final ItemAccess access;

   public FluidTransfer(ResourceHandler<FluidResource> handler, ItemAccess access) {
      this.handler = handler;
      this.access = access;
   }

   public static FluidTransfer getFluidHandler(ItemStack stack) {
      if (stack.isEmpty()) {
         return null;
      } else {
         ItemAccess access = ItemAccess.forStack(stack);
         ResourceHandler<FluidResource> handler = ItemCapabilities.get(stack, access, Fluid.ITEM);
         if (handler == null) {
            handler = (ResourceHandler<FluidResource>)access.getCapability(Fluid.ITEM);
         }

         return handler == null ? null : new FluidTransfer(handler, access);
      }
   }

   public static FluidTransfer getFluidHandler(Level level, BlockPos pos, Direction side) {
      ResourceHandler<FluidResource> handler = BlockCapabilities.get(level.getBlockEntity(pos), Fluid.BLOCK, side);
      if (handler == null) {
         handler = (ResourceHandler<FluidResource>)level.getCapability(Fluid.BLOCK, pos, side);
      }

      return handler == null ? null : new FluidTransfer(handler, null);
   }

   public static FluidStack getFluidContained(ItemStack stack) {
      FluidTransfer transfer = getFluidHandler(stack);
      if (transfer == null) {
         return null;
      } else {
         for (int index = 0; index < transfer.handler.size(); index++) {
            int amount = transfer.handler.getAmountAsInt(index);
            if (amount > 0) {
               return ((FluidResource)transfer.handler.getResource(index)).toStack(amount);
            }
         }

         return null;
      }
   }

   public ItemStack getContainer() {
      if (this.access == null) {
         throw new IllegalStateException("Block fluid handler has no item container");
      } else {
         return this.access.getResource().toStack(this.access.getAmount());
      }
   }

   public int fill(FluidStack stack, boolean execute) {
      if (stack != null && !stack.isEmpty()) {
         Transaction tx = Transaction.open(null);

         int var5;
         try {
            int inserted = this.handler.insert(FluidResource.of(stack), stack.getAmount(), tx);
            if (execute) {
               tx.commit();
            }

            var5 = inserted;
         } catch (Throwable var7) {
            if (tx != null) {
               try {
                  tx.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (tx != null) {
            tx.close();
         }

         return var5;
      } else {
         return 0;
      }
   }

   public FluidStack drain(int amount, boolean execute) {
      if (amount <= 0) {
         return null;
      } else {
         for (int i = 0; i < this.handler.size(); i++) {
            FluidResource resource = (FluidResource)this.handler.getResource(i);
            if (!resource.isEmpty()) {
               FluidStack result = this.drain(resource.toStack(amount), execute);
               if (result != null) {
                  return result;
               }
            }
         }

         return null;
      }
   }

   public FluidStack drain(FluidStack stack, boolean execute) {
      if (stack != null && !stack.isEmpty()) {
         FluidResource resource = FluidResource.of(stack);
         Transaction tx = Transaction.open(null);

         FluidStack var6;
         try {
            int extracted = this.handler.extract(resource, stack.getAmount(), tx);
            if (execute) {
               tx.commit();
            }

            var6 = extracted == 0 ? null : resource.toStack(extracted);
         } catch (Throwable var8) {
            if (tx != null) {
               try {
                  tx.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (tx != null) {
            tx.close();
         }

         return var6;
      } else {
         return null;
      }
   }

   public FluidTransfer.TankContents[] getTankProperties() {
      FluidTransfer.TankContents[] result = new FluidTransfer.TankContents[this.handler.size()];

      for (int i = 0; i < result.length; i++) {
         FluidResource resource = (FluidResource)this.handler.getResource(i);
         int amount = this.handler.getAmountAsInt(i);
         result[i] = new FluidTransfer.TankContents(amount == 0 ? null : resource.toStack(amount), this.handler.getCapacityAsInt(i, resource));
      }

      return result;
   }

   public static boolean interactWithFluidHandler(Player player, InteractionHand hand, ResourceHandler<FluidResource> handler) {
      if (handler == null) {
         return false;
      } else {
         Transaction tx = Transaction.open(null);

         boolean var5;
         try {
            boolean changed = FluidUtil.interactWithFluidHandler(player, hand, player.blockPosition(), handler, tx);
            if (changed) {
               tx.commit();
            }

            var5 = changed;
         } catch (Throwable var7) {
            if (tx != null) {
               try {
                  tx.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (tx != null) {
            tx.close();
         }

         return var5;
      }
   }

   public static FluidStack tryFluidTransfer(FluidTransfer target, FluidTransfer source, int limit, boolean execute) {
      if (limit <= 0) {
         return null;
      } else {
         int i = 0;

         Transaction tx;
         FluidStack var10;
         while (true) {
            if (i >= source.handler.size()) {
               return null;
            }

            FluidResource resource = (FluidResource)source.handler.getResource(i);
            label51:
            if (!resource.isEmpty()) {
               int available = Math.min(limit, source.handler.getAmountAsInt(i));
               tx = Transaction.open(null);

               label61: {
                  try {
                     int accepted = target.handler.insert(resource, available, tx);
                     int extracted = source.handler.extract(resource, accepted, tx);
                     if (extracted == accepted) {
                        if (accepted <= 0) {
                           break label61;
                        }

                        if (execute) {
                           tx.commit();
                        }

                        var10 = resource.toStack(accepted);
                        break;
                     }
                  } catch (Throwable var12) {
                     if (tx != null) {
                        try {
                           tx.close();
                        } catch (Throwable var11) {
                           var12.addSuppressed(var11);
                        }
                     }

                     throw var12;
                  }

                  if (tx != null) {
                     tx.close();
                  }
                  break label51;
               }

               if (tx != null) {
                  tx.close();
               }
            }

            i++;
         }

         if (tx != null) {
            tx.close();
         }

         return var10;
      }
   }

   public record TankContents(FluidStack getContents, int getCapacity) {
   }
}
