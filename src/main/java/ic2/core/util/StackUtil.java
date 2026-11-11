package ic2.core.util;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.mojang.authlib.GameProfile;
import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.Recipes;
import ic2.core.IC2;
import ic2.core.Ic2Player;
import ic2.core.block.personal.IPersonalBlock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities.Item;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

public final class StackUtil {
   public static final Predicate<ItemStack> anyStack = Predicates.alwaysTrue();
   static final Set<String> ignoredNbtKeys = new HashSet<>(Arrays.asList("damage", "charge", "energy", "advDmg"));
   public static final ItemStack emptyStack = ItemStack.EMPTY;
   private static final int[] emptySlotArray = new int[0];

   public static boolean isInventoryTile(BlockEntity te, Direction side) {
      return te instanceof Container || te != null && BlockCapabilities.has(te, Item.BLOCK, side);
   }

   public static Container findDoubleChest(ChestBlockEntity chest) {
      Level world = chest.getLevel();
      BlockPos pos = chest.getBlockPos();
      if (world != null && world.hasChunkAt(pos)) {
         BlockState state = world.getBlockState(pos);
         return (Container)(state.getBlock() instanceof ChestBlock block ? ChestBlock.getContainer(block, state, world, pos, true) : chest);
      } else {
         return null;
      }
   }

   public static StackUtil.AdjacentInv getAdjacentInventory(BlockEntity source, Direction dir) {
      BlockEntity target = source.getLevel().getBlockEntity(source.getBlockPos().relative(dir));
      if (!isInventoryTile(target, dir)) {
         return null;
      } else {
         GameProfile srcOwner;
         if (target instanceof IPersonalBlock && source instanceof IPersonalBlock && (srcOwner = ((IPersonalBlock)source).getOwner()) != null) {
            return new StackUtil.PersonalAdjacentInv(target, dir, srcOwner);
         } else {
            return target instanceof ChestBlockEntity && findDoubleChest((ChestBlockEntity)target) == null ? null : new StackUtil.AdjacentInv(target, dir);
         }
      }
   }

   public static List<StackUtil.AdjacentInv> getAdjacentInventories(BlockEntity source) {
      List<StackUtil.AdjacentInv> inventories = new ArrayList<>();

      for (Direction dir : Direction.values()) {
         StackUtil.AdjacentInv inventory = getAdjacentInventory(source, dir);
         if (inventory != null) {
            inventories.add(inventory);
         }
      }

      Collections.sort(
         inventories,
         new Comparator<StackUtil.AdjacentInv>() {
            public int compare(StackUtil.AdjacentInv a, StackUtil.AdjacentInv b) {
               if (!(a.te instanceof IPersonalBlock) && b.te instanceof IPersonalBlock) {
                  return !(b.te instanceof IPersonalBlock) && a.te instanceof IPersonalBlock
                     ? StackUtil.getInventorySize(b.te, b.dir.getOpposite(), b.getAccessor())
                        - StackUtil.getInventorySize(a.te, a.dir.getOpposite(), a.getAccessor())
                     : 1;
               } else {
                  return -1;
               }
            }
         }
      );
      return inventories;
   }

   public static GameProfile getOwner(BlockEntity te) {
      return te instanceof IPersonalBlock ? ((IPersonalBlock)te).getOwner() : null;
   }

   public static int getInventorySize(BlockEntity te, Direction side, GameProfile accessor) {
      if (te instanceof Container) {
         Container inv = getInventory(te, accessor);
         return inv == null ? 0 : inv.getContainerSize();
      } else if (BlockCapabilities.has(te, Item.BLOCK, side)) {
         ResourceHandler<ItemResource> handler = BlockCapabilities.get(te, Item.BLOCK, side);
         return handler == null ? 0 : handler.size();
      } else {
         return 0;
      }
   }

   private static Container getInventory(BlockEntity te, GameProfile accessor) {
      if (te instanceof ChestBlockEntity) {
         return findDoubleChest((ChestBlockEntity)te);
      } else if (te instanceof IPersonalBlock) {
         return ((IPersonalBlock)te).getPrivilegedInventory(accessor);
      } else {
         return te instanceof Container ? (Container)te : null;
      }
   }

   public static int distribute(BlockEntity source, ItemStack stack, boolean simulate) {
      ItemStack remaining = copy(stack);

      for (StackUtil.AdjacentInv inventory : getAdjacentInventories(source)) {
         int amount = putInInventory(source, inventory, remaining, simulate);
         remaining = decSize(remaining, amount);
         if (isEmpty(remaining)) {
            break;
         }
      }

      return getSize(stack) - getSize(remaining);
   }

   public static int fetch(BlockEntity source, ItemStack stack, boolean simulate) {
      ItemStack remaining = copy(stack);

      for (StackUtil.AdjacentInv inventory : getAdjacentInventories(source)) {
         ItemStack transferred = getFromInventory(source, inventory, remaining, true, simulate);
         if (!isEmpty(transferred)) {
            remaining = decSize(remaining, getSize(transferred));
            if (isEmpty(remaining)) {
               break;
            }
         }
      }

      return getSize(stack) - getSize(remaining);
   }

   public static int transfer(BlockEntity src, BlockEntity dst, Direction dir, int amount) {
      return transfer(src, dst, dir, amount, Predicates.alwaysTrue(), true);
   }

   public static int transfer(BlockEntity src, BlockEntity dst, Direction dir, int amount, Predicate<ItemStack> checker) {
      return transfer(src, dst, dir, amount, checker, checker == null || Predicates.alwaysTrue().equals(checker));
   }

   private static int transfer(BlockEntity src, BlockEntity dst, Direction dir, int amount, Predicate<ItemStack> checker, boolean skipChecker) {
      if (amount <= 0) {
         return 0;
      } else {
         GameProfile srcOwner = getOwner(src);
         GameProfile dstOwner = getOwner(dst);
         Direction reverseDir = dir.getOpposite();
         int[] srcSlots = getInventorySlots(src, dir, false, true, dstOwner);
         if (srcSlots.length == 0) {
            return 0;
         } else {
            int[] dstSlots = getInventorySlots(dst, reverseDir, true, false, srcOwner);
            if (dstSlots.length == 0) {
               return 0;
            } else if (src instanceof Container) {
               Container srcInv = getInventory(src, dstOwner);
               if (srcInv == null) {
                  return 0;
               } else if (dst instanceof Container) {
                  Container dstInv = getInventory(dst, srcOwner);
                  return dstInv == null ? 0 : transfer(srcInv, srcSlots, dstInv, dstSlots, dir, reverseDir, amount, checker, skipChecker);
               } else if (BlockCapabilities.has(dst, Item.BLOCK, dir.getOpposite())) {
                  ResourceHandler<ItemResource> dstHandler = BlockCapabilities.get(dst, Item.BLOCK, dir.getOpposite());
                  return dstHandler == null ? 0 : transfer(srcInv, srcSlots, dstHandler, dstSlots, reverseDir, amount, checker, skipChecker);
               } else {
                  return 0;
               }
            } else if (BlockCapabilities.has(src, Item.BLOCK, dir)) {
               ResourceHandler<ItemResource> srcHandler = BlockCapabilities.get(src, Item.BLOCK, dir);
               if (srcHandler == null) {
                  return 0;
               } else if (dst instanceof Container) {
                  Container dstInv = getInventory(dst, srcOwner);
                  return dstInv == null ? 0 : transfer(srcHandler, srcSlots, dstInv, dstSlots, reverseDir, amount, checker, skipChecker);
               } else if (BlockCapabilities.has(dst, Item.BLOCK, dir.getOpposite())) {
                  ResourceHandler<ItemResource> dstHandler = BlockCapabilities.get(dst, Item.BLOCK, dir.getOpposite());
                  return dstHandler == null ? 0 : transfer(srcHandler, srcSlots, dstHandler, dstSlots, amount, checker, skipChecker);
               } else {
                  return 0;
               }
            } else {
               return 0;
            }
         }
      }
   }

   private static int transfer(
      Container src,
      int[] srcSlots,
      Container dst,
      int[] dstSlots,
      Direction dir,
      Direction reverseDir,
      int amount,
      Predicate<ItemStack> checker,
      boolean skipChecker
   ) {
      WorldlyContainer dstSided = dst instanceof WorldlyContainer ? (WorldlyContainer)dst : null;
      int total = amount;

      for (int srcSlot : srcSlots) {
         ItemStack srcStack = src.getItem(srcSlot);
         if (!isEmpty(srcStack) && (skipChecker || checker.apply(srcStack))) {
            int transferred = insert(srcStack, amount, dst, dstSided, reverseDir, dstSlots);
            if (transferred > 0) {
               amount -= transferred;
               src.setItem(srcSlot, decSize(srcStack, transferred));
               if (amount <= 0) {
                  break;
               }
            }
         }
      }

      amount = total - amount;

      assert amount >= 0;

      if (amount > 0) {
         src.setChanged();
         dst.setChanged();
      }

      return amount;
   }

   private static int transfer(
      ResourceHandler<ItemResource> src,
      int[] srcSlots,
      Container dst,
      int[] dstSlots,
      Direction reverseDir,
      int amount,
      Predicate<ItemStack> checker,
      boolean skipChecker
   ) {
      WorldlyContainer dstSided = dst instanceof WorldlyContainer ? (WorldlyContainer)dst : null;
      int total = amount;

      for (int srcSlot : srcSlots) {
         ItemStack srcStack = ItemTransfers.extract(src, srcSlot, amount, true);
         if (!isEmpty(srcStack) && (skipChecker || checker.apply(srcStack))) {
            int transferred = insert(srcStack, amount, dst, dstSided, reverseDir, dstSlots);
            if (transferred > 0) {
               amount -= transferred;
               ItemTransfers.extract(src, srcSlot, transferred, false);
               if (amount <= 0) {
                  break;
               }
            }
         }
      }

      amount = total - amount;

      assert amount >= 0;

      if (amount > 0) {
         dst.setChanged();
      }

      return amount;
   }

   private static int insert(ItemStack stack, int maxAmount, Container dst, WorldlyContainer dstSided, Direction side, int[] dstSlots) {
      int sizeLimit = Math.min(stack.getMaxStackSize(), dst.getMaxStackSize());
      int total = Math.min(maxAmount, getSize(stack));
      int remaining = total;

      for (int pass = 0; pass < 2; pass++) {
         for (int i = 0; i < dstSlots.length; i++) {
            int dstSlot = dstSlots[i];
            if (dstSlot >= 0) {
               ItemStack dstStack = dst.getItem(dstSlot);
               if ((pass != 0 || !isEmpty(dstStack) && checkItemEqualityStrict(stack, dstStack))
                  && (pass != 1 || isEmpty(dstStack))
                  && dst.canPlaceItem(dstSlot, stack)
                  && (dstSided == null || dstSided.canPlaceItemThroughFace(dstSlot, stack, side))) {
                  int amount = Math.min(remaining, sizeLimit - getSize(dstStack));
                  if (isEmpty(dstStack)) {
                     dst.setItem(dstSlot, copyWithSize(stack, amount));
                  } else {
                     if (amount <= 0) {
                        dstSlots[i] = -1;
                        continue;
                     }

                     dst.setItem(dstSlot, incSize(dstStack, amount));
                  }

                  assert amount > 0;

                  remaining -= amount;
                  if (remaining <= 0) {
                     return total;
                  }
               }
            }
         }
      }

      return total - remaining;
   }

   private static int transfer(
      ResourceHandler<ItemResource> src,
      int[] srcSlots,
      ResourceHandler<ItemResource> dst,
      int[] dstSlots,
      int amount,
      Predicate<ItemStack> checker,
      boolean skipChecker
   ) {
      int total = amount;

      for (int srcSlot : srcSlots) {
         ItemStack srcStack = ItemTransfers.extract(src, srcSlot, amount, true);
         if (!isEmpty(srcStack) && (skipChecker || checker.apply(srcStack))) {
            int transferred = insert(srcStack, Integer.MAX_VALUE, dst, dstSlots);
            if (transferred > 0) {
               amount -= transferred;
               ItemTransfers.extract(src, srcSlot, transferred, false);
               if (amount <= 0) {
                  break;
               }
            }
         }
      }

      amount = total - amount;

      assert amount >= 0;

      return amount;
   }

   private static int transfer(
      Container src,
      int[] srcSlots,
      ResourceHandler<ItemResource> dst,
      int[] dstSlots,
      Direction dir,
      int amount,
      Predicate<ItemStack> checker,
      boolean skipChecker
   ) {
      int total = amount;

      for (int srcSlot : srcSlots) {
         ItemStack srcStack = src.getItem(srcSlot);
         if (!isEmpty(srcStack) && (skipChecker || checker.apply(srcStack))) {
            int transferred = insert(srcStack, amount, dst, dstSlots);
            if (transferred > 0) {
               amount -= transferred;
               src.setItem(srcSlot, decSize(srcStack, transferred));
               if (amount <= 0) {
                  break;
               }
            }
         }
      }

      amount = total - amount;

      assert amount >= 0;

      if (amount > 0) {
         src.setChanged();
      }

      return amount;
   }

   private static int insert(ItemStack stack, int maxAmount, ResourceHandler<ItemResource> dst, int[] dstSlots) {
      int total = Math.min(maxAmount, getSize(stack));
      int remaining = total;

      assert !isEmpty(stack);

      for (int pass = 0; pass < 2; pass++) {
         for (int dstSlot : dstSlots) {
            if (dstSlot >= 0) {
               ItemStack dstStack = ItemTransfers.get(dst, dstSlot);
               if ((pass != 0 || !isEmpty(dstStack) && checkItemEqualityStrict(stack, dstStack)) && (pass != 1 || isEmpty(dstStack))) {
                  ItemStack leftOver = ItemTransfers.insert(dst, dstSlot, copyWithSize(stack, remaining), false);
                  int transferred = remaining - getSize(leftOver);
                  remaining -= transferred;
                  if (remaining <= 0) {
                     return total;
                  }
               }
            }
         }
      }

      return total - remaining;
   }

   public static void distributeDrops(BlockEntity source, List<ItemStack> stacks) {
      ListIterator<ItemStack> it = stacks.listIterator();

      while (it.hasNext()) {
         ItemStack stack = it.next();
         int amount = distribute(source, stack, false);
         if (amount == getSize(stack)) {
            it.remove();
         } else {
            it.set(decSize(stack, amount));
         }
      }

      for (ItemStack stack : stacks) {
         dropAsEntity(source.getLevel(), source.getBlockPos(), stack);
      }

      stacks.clear();
   }

   private static ItemStack getFromInventory(BlockEntity source, StackUtil.AdjacentInv inventory, ItemStack stack, boolean ignoreMaxStackSize, boolean simulate) {
      return getFromInventory(inventory.te, inventory.dir.getOpposite(), stack, getSize(stack), ignoreMaxStackSize, inventory.getAccessor(), simulate);
   }

   public static ItemStack getFromInventory(BlockEntity te, Direction side, ItemStack stackDestination, int max, boolean ignoreMaxStackSize, boolean simulate) {
      return getFromInventory(te, side, stackDestination, max, ignoreMaxStackSize, null, simulate);
   }

   public static ItemStack getFromInventory(
      BlockEntity te, Direction side, ItemStack stackDestination, int max, boolean ignoreMaxStackSize, GameProfile accessor, boolean simulate
   ) {
      if (!isEmpty(stackDestination) && !ignoreMaxStackSize) {
         max = Math.min(max, stackDestination.getMaxStackSize() - getSize(stackDestination));
      }

      int[] slots = getInventorySlots(te, side, false, true, accessor);
      if (slots.length == 0) {
         return emptyStack;
      } else {
         ItemStack ret = emptyStack;
         if (te instanceof Container) {
            Container inv = getInventory(te, accessor);
            if (inv == null) {
               return emptyStack;
            }

            for (int slot : slots) {
               if (max <= 0) {
                  break;
               }

               ItemStack stack = inv.getItem(slot);
               if (!isEmpty(stack) && (isEmpty(stackDestination) || checkItemEqualityStrict(stack, stackDestination))) {
                  boolean extra = isEmpty(ret);
                  if (extra) {
                     ret = copyWithSize(stack, 1);
                     if (isEmpty(stackDestination)) {
                        if (!ignoreMaxStackSize) {
                           max = Math.min(max, ret.getMaxStackSize());
                        }

                        stackDestination = ret;
                     }
                  }

                  int transfer = Math.min(max, getSize(stack));
                  if (!simulate) {
                     stack = decSize(stack, transfer);
                     inv.setItem(slot, stack);
                  }

                  max -= transfer;
                  ret = incSize(ret, extra ? transfer - 1 : transfer);
               }
            }

            if (!simulate && !isEmpty(ret)) {
               inv.setChanged();
            }
         } else if (BlockCapabilities.has(te, Item.BLOCK, side)) {
            ResourceHandler<ItemResource> handler = BlockCapabilities.get(te, Item.BLOCK, side);
            if (handler == null) {
               return emptyStack;
            }

            for (int slot : slots) {
               if (max <= 0) {
                  break;
               }

               if (!isEmpty(stackDestination)) {
                  ItemStack stack = ItemTransfers.get(handler, slot);
                  if (isEmpty(stack) || !checkItemEqualityStrict(stack, stackDestination)) {
                     continue;
                  }
               }

               ItemStack stack = ItemTransfers.extract(handler, slot, max, simulate);
               if (!isEmpty(stack)) {
                  boolean extrax = isEmpty(ret);
                  if (extrax) {
                     ret = copyWithSize(stack, 1);
                     if (isEmpty(stackDestination)) {
                        if (!ignoreMaxStackSize) {
                           max = Math.min(max, ret.getMaxStackSize());
                        }

                        stackDestination = ret;
                     }
                  } else {
                     assert checkItemEqualityStrict(stack, ret);
                  }

                  int transfer = getSize(stack);
                  max -= transfer;
                  ret = incSize(ret, extrax ? transfer - 1 : transfer);
               }
            }
         }

         return ret;
      }
   }

   private static int putInInventory(BlockEntity source, StackUtil.AdjacentInv inventory, ItemStack stackSource, boolean simulate) {
      return putInInventory(inventory.te, inventory.dir.getOpposite(), stackSource, inventory.getAccessor(), simulate);
   }

   public static int putInInventory(BlockEntity te, Direction side, ItemStack stackSource, boolean simulate) {
      return putInInventory(te, side, stackSource, null, simulate);
   }

   public static int putInInventory(BlockEntity te, Direction side, ItemStack stackSource, GameProfile accessor, boolean simulate) {
      if (isEmpty(stackSource)) {
         return 0;
      } else {
         int[] slots = getInventorySlots(te, side, true, false, accessor);
         if (slots.length == 0) {
            return 0;
         } else if (te instanceof Container) {
            Container inv = getInventory(te, accessor);
            if (inv == null) {
               return 0;
            } else {
               int toTransfer = getSize(stackSource);

               for (int slot : slots) {
                  if (toTransfer <= 0) {
                     break;
                  }

                  if (inv.canPlaceItem(slot, stackSource)
                     && (!(inv instanceof WorldlyContainer) || ((WorldlyContainer)inv).canPlaceItemThroughFace(slot, stackSource, side))) {
                     ItemStack stack = inv.getItem(slot);
                     if (!isEmpty(stack) && checkItemEqualityStrict(stack, stackSource)) {
                        int transfer = Math.min(toTransfer, Math.min(inv.getMaxStackSize(), stack.getMaxStackSize()) - getSize(stack));
                        if (!simulate) {
                           inv.setItem(slot, incSize(stack, transfer));
                        }

                        toTransfer -= transfer;
                     }
                  }
               }

               for (int slot : slots) {
                  if (toTransfer <= 0) {
                     break;
                  }

                  if (inv.canPlaceItem(slot, stackSource)
                     && (!(inv instanceof WorldlyContainer) || ((WorldlyContainer)inv).canPlaceItemThroughFace(slot, stackSource, side))) {
                     ItemStack stack = inv.getItem(slot);
                     if (isEmpty(stack)) {
                        int transfer = Math.min(toTransfer, Math.min(inv.getMaxStackSize(), stackSource.getMaxStackSize()));
                        if (!simulate) {
                           ItemStack dest = copyWithSize(stackSource, transfer);
                           inv.setItem(slot, dest);
                        }

                        toTransfer -= transfer;
                     }
                  }
               }

               if (!simulate && toTransfer != getSize(stackSource)) {
                  inv.setChanged();
               }

               return getSize(stackSource) - toTransfer;
            }
         } else if (!BlockCapabilities.has(te, Item.BLOCK, side)) {
            return 0;
         } else {
            ResourceHandler<ItemResource> handler = BlockCapabilities.get(te, Item.BLOCK, side);
            if (handler == null) {
               return 0;
            } else {
               ItemStack src = stackSource.copy();

               for (int slot : slots) {
                  if (isEmpty(src)) {
                     break;
                  }

                  ItemStack stack = ItemTransfers.get(handler, slot);
                  if (!isEmpty(stack)) {
                     ItemStack remaining = ItemTransfers.insert(handler, slot, src.copy(), simulate);
                     if (isEmpty(remaining)) {
                        src = emptyStack;
                     } else if (getSize(remaining) < getSize(src)) {
                        src = setSize(src, getSize(remaining));
                     }
                  }
               }

               for (int slot : slots) {
                  if (isEmpty(src)) {
                     break;
                  }

                  ItemStack stack = ItemTransfers.get(handler, slot);
                  if (isEmpty(stack)) {
                     ItemStack remaining = ItemTransfers.insert(handler, slot, src.copy(), simulate);
                     if (isEmpty(remaining)) {
                        src = emptyStack;
                     } else if (getSize(remaining) < getSize(src)) {
                        src = setSize(src, getSize(remaining));
                     }
                  }
               }

               return getSize(stackSource) - getSize(src);
            }
         }
      }
   }

   private static int[] getInventorySlots(BlockEntity te, Direction side, boolean checkInsert, boolean checkExtract, GameProfile accessor) {
      if (te instanceof Container) {
         Container inv = getInventory(te, accessor);
         if (inv != null && inv.getMaxStackSize() > 0) {
            int[] ret;
            if (inv instanceof WorldlyContainer sidedInv) {
               ret = sidedInv.getSlotsForFace(side);
               if (ret.length == 0) {
                  return emptySlotArray;
               }

               ret = Arrays.copyOf(ret, ret.length);
            } else {
               int size = inv.getContainerSize();
               if (size <= 0) {
                  return emptySlotArray;
               }

               sidedInv = null;
               ret = new int[size];
               int i = 0;

               while (i < ret.length) {
                  ret[i] = i++;
               }
            }

            if (checkInsert || checkExtract) {
               int writeIdx = 0;

               for (int readIdx = 0; readIdx < ret.length; readIdx++) {
                  int slot = ret[readIdx];
                  ItemStack stack = inv.getItem(slot);
                  if ((!checkExtract || !isEmpty(stack) && (sidedInv == null || sidedInv.canTakeItemThroughFace(slot, stack, side)))
                     && (
                        !checkInsert
                           || isEmpty(stack)
                           || getSize(stack) < stack.getMaxStackSize()
                              && getSize(stack) < inv.getMaxStackSize()
                              && (sidedInv == null || sidedInv.canPlaceItemThroughFace(slot, stack, side))
                     )) {
                     ret[writeIdx] = slot;
                     writeIdx++;
                  }
               }

               if (writeIdx != ret.length) {
                  ret = Arrays.copyOf(ret, writeIdx);
               }
            }

            return ret;
         } else {
            return emptySlotArray;
         }
      } else if (!BlockCapabilities.has(te, Item.BLOCK, side)) {
         return emptySlotArray;
      } else {
         ResourceHandler<ItemResource> handler = BlockCapabilities.get(te, Item.BLOCK, side);
         if (handler == null) {
            return emptySlotArray;
         } else {
            int size = handler.size();
            if (size <= 0) {
               return emptySlotArray;
            } else {
               int[] retx = new int[size];
               int i = 0;

               while (i < retx.length) {
                  retx[i] = i++;
               }

               if (checkInsert || checkExtract) {
                  i = 0;

                  for (int readIdxx = 0; readIdxx < retx.length; readIdxx++) {
                     int slot = retx[readIdxx];
                     ItemStack stack = ItemTransfers.get(handler, slot);
                     if ((!checkExtract || !isEmpty(stack) && !isEmpty(ItemTransfers.extract(handler, slot, Integer.MAX_VALUE, true)))
                        && (!checkInsert || checkInsert(handler, slot, stack))) {
                        retx[i] = slot;
                        i++;
                     }
                  }

                  if (i != retx.length) {
                     retx = Arrays.copyOf(retx, i);
                  }
               }

               return retx;
            }
         }
      }
   }

   private static boolean checkInsert(ResourceHandler<ItemResource> handler, int slot, ItemStack stack) {
      if (!isEmpty(stack) && getSize(stack) < stack.getMaxStackSize()) {
         int testSize = Integer.MAX_VALUE;
         ItemStack result = ItemTransfers.insert(handler, slot, copyWithSize(stack, Integer.MAX_VALUE), true);
         return isEmpty(result) || getSize(result) < Integer.MAX_VALUE;
      } else {
         return true;
      }
   }

   public static boolean consumeFromPlayerInventory(Player player, Predicate<ItemStack> request, int amount, boolean simulate) {
      NonNullList<ItemStack> contents = player.getInventory().getNonEquipmentItems();
      int pass = 0;

      label44:
      while (pass < 2) {
         int amountNeeded = amount;

         for (int i = 0; i < contents.size(); i++) {
            ItemStack stack = (ItemStack)contents.get(i);
            if (request.apply(stack)) {
               if (player.getAbilities().instabuild) {
                  return true;
               }

               int cAmount = Math.min(getSize(stack), amountNeeded);
               amountNeeded -= cAmount;
               if (pass == 1) {
                  contents.set(i, decSize(stack, cAmount));
               }

               if (amountNeeded <= 0) {
                  if (amountNeeded > 0) {
                     if (pass == 1) {
                        IC2.log
                           .warn(LogCategory.General, "Inconsistent inventory transaction for player %s, request %s: %d missing", player, request, amountNeeded);
                     }

                     return false;
                  }

                  if (simulate) {
                     return true;
                  }

                  pass++;
                  continue label44;
               }
            }
         }

         return true;
      }

      return true;
   }

   public static Predicate<ItemStack> sameStack(final ItemStack stack) {
      if (isEmpty(stack)) {
         throw new IllegalArgumentException("empty stack");
      } else {
         return new Predicate<ItemStack>() {
            public boolean apply(ItemStack input) {
               return StackUtil.checkItemEquality(input, stack);
            }

            @Override
            public String toString() {
               return "stack==" + stack;
            }
         };
      }
   }

   public static Predicate<ItemStack> sameItem(final net.minecraft.world.item.Item item) {
      if (item == null) {
         throw new NullPointerException("null item");
      } else {
         return new Predicate<ItemStack>() {
            public boolean apply(ItemStack input) {
               return input.getItem() == item;
            }

            @Override
            public String toString() {
               return "item==" + item;
            }
         };
      }
   }

   public static Predicate<ItemStack> sameItem(Block block) {
      if (block == null) {
         throw new NullPointerException("null block");
      } else {
         net.minecraft.world.item.Item item = net.minecraft.world.item.Item.byBlock(block);
         if (item == null || item == Items.AIR && block != Blocks.AIR) {
            throw new IllegalArgumentException("block " + block + " doesn't have an associated item");
         } else {
            return sameItem(item);
         }
      }
   }

   public static Predicate<ItemStack> oreDict(String name) {
      return recipeInput(Recipes.inputFactory.forOreDict(name));
   }

   public static Predicate<ItemStack> recipeInput(final IRecipeInput item) {
      return new Predicate<ItemStack>() {
         public boolean apply(ItemStack input) {
            return item.matches(input);
         }

         @Override
         public String toString() {
            return item.toString();
         }
      };
   }

   public static boolean consume(Player player, InteractionHand hand, Predicate<ItemStack> request, int amount) {
      return consume0(player, hand, request, amount, false) != emptyStack;
   }

   public static ItemStack consumeAndGet(Player player, Predicate<ItemStack> request, int amount) {
      return consumeAndGet(player, InteractionHand.MAIN_HAND, request, amount);
   }

   public static ItemStack consumeAndGet(Player player, InteractionHand hand, Predicate<ItemStack> request, int amount) {
      return consume0(player, hand, request, amount, true);
   }

   public static void consumeOrError(Player player, InteractionHand hand, int amount) {
      consumeOrError(player, hand, anyStack, amount);
   }

   public static void consumeOrError(Player player, InteractionHand hand, Predicate<ItemStack> request, int amount) {
      if (!consume(player, hand, request, amount)) {
         throw new IllegalStateException("consume failed");
      }
   }

   private static ItemStack consume0(Player player, InteractionHand hand, Predicate<ItemStack> request, int amount, boolean copyOutput) {
      if (amount <= 0) {
         throw new IllegalArgumentException("negative/zero amount");
      } else {
         ItemStack stack = get(player, hand);
         if (isEmpty(stack)) {
            return emptyStack;
         } else if (!request.apply(stack)) {
            return emptyStack;
         } else if (player.getAbilities().instabuild) {
            return copyOutput ? copyWithSize(stack, amount) : stack;
         } else if (getSize(stack) < amount) {
            return emptyStack;
         } else {
            ItemStack ret;
            if (getSize(stack) == amount) {
               ret = stack;
               clear(player, hand);
            } else {
               ret = copyOutput ? copyWithSize(stack, amount) : stack;
               set(player, hand, decSize(stack, amount));
            }

            return ret;
         }
      }
   }

   public static boolean damage(Player player, InteractionHand hand, Predicate<ItemStack> request, int amount) {
      return damage0(player, hand, request, amount, false) != emptyStack;
   }

   public static void damageOrError(Player player, InteractionHand hand, int amount) {
      damageOrError(player, hand, anyStack, amount);
   }

   public static void damageOrError(Player player, InteractionHand hand, Predicate<ItemStack> request, int amount) {
      if (!damage(player, hand, request, amount)) {
         throw new IllegalStateException("damage failed");
      }
   }

   private static ItemStack damage0(Player player, InteractionHand hand, Predicate<ItemStack> request, int amount, boolean copyOutput) {
      if (amount <= 0) {
         throw new IllegalArgumentException("negative/zero amount");
      } else {
         ItemStack stack = get(player, hand);
         if (isEmpty(stack)) {
            return emptyStack;
         } else {
            int maxDamage = stack.getMaxDamage();
            if (maxDamage <= 0) {
               return emptyStack;
            } else if (!request.apply(stack)) {
               return emptyStack;
            } else if (!player.getAbilities().instabuild && stack.isDamageableItem()) {
               stack.hurtAndBreak(amount, player, hand);
               ItemStack ret;
               if (isEmpty(stack)) {
                  ret = stack;
                  clear(player, hand);
               } else {
                  ret = copyOutput ? copy(stack) : stack;
                  set(player, hand, stack);
               }

               return ret;
            } else {
               return copyOutput ? copy(stack) : stack;
            }
         }
      }
   }

   public static ItemStack get(Player player, InteractionHand hand) {
      return player.getItemInHand(hand);
   }

   public static void set(Player player, InteractionHand hand, ItemStack stack) {
      if (isEmpty(stack)) {
         stack = emptyStack;
      }

      Inventory inv = player.getInventory();
      if (hand == InteractionHand.MAIN_HAND) {
         player.setItemInHand(hand, stack);
      } else {
         if (hand != InteractionHand.OFF_HAND) {
            throw new IllegalArgumentException("invalid hand: " + hand);
         }

         player.setItemInHand(hand, stack);
      }
   }

   public static void clear(Player player, InteractionHand hand) {
      set(player, hand, emptyStack);
   }

   public static void clearEmpty(Player player, InteractionHand hand) {
      if (isEmpty(player, hand)) {
         clear(player, hand);
      }
   }

   public static void dropAsEntity(Level world, BlockPos pos, ItemStack stack) {
      if (!isEmpty(stack)) {
         double f = 0.7;
         double dx = world.getRandom().nextFloat() * f + (1.0 - f) * 0.5;
         double dy = world.getRandom().nextFloat() * f + (1.0 - f) * 0.5;
         double dz = world.getRandom().nextFloat() * f + (1.0 - f) * 0.5;
         ItemEntity entityItem = new ItemEntity(world, pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz, stack.copy());
         entityItem.setDefaultPickUpDelay();
         world.addFreshEntity(entityItem);
      }
   }

   public static ItemStack copy(ItemStack stack) {
      return stack.copy();
   }

   public static ItemStack copyWithSize(ItemStack stack, int newSize) {
      if (isEmpty(stack)) {
         throw new IllegalArgumentException("empty stack: " + toStringSafe(stack));
      } else {
         return setSize(copy(stack), newSize);
      }
   }

   public static ItemStack copyShrunk(ItemStack stack, int amount) {
      if (isEmpty(stack)) {
         throw new IllegalArgumentException("empty stack: " + toStringSafe(stack));
      } else {
         return setSize(copy(stack), getSize(stack) - amount);
      }
   }

   public static ItemStack copyWithWildCard(ItemStack stack) {
      ItemStack ret = copy(stack);
      setRawMeta(ret, 32767);
      return ret;
   }

   public static Collection<ItemStack> copy(Collection<ItemStack> c) {
      List<ItemStack> ret = new ArrayList<>(c.size());

      for (ItemStack stack : c) {
         ret.add(copy(stack));
      }

      return ret;
   }

   public static CompoundTag getOrCreateNbtData(ItemStack stack) {
      return ItemData.readOrEmpty(stack);
   }

   public static boolean checkItemEquality(ItemStack a, ItemStack b) {
      return isEmpty(a) && isEmpty(b)
         || !isEmpty(a)
            && !isEmpty(b)
            && a.getItem() == b.getItem()
            && (ItemData.read(a) == null || !ItemData.read(a).contains("ic2:variant") || ItemStacks.rawMeta(a) == ItemStacks.rawMeta(b))
            && checkNbtEquality(a, b);
   }

   public static boolean checkItemEquality(ItemStack a, net.minecraft.world.item.Item b) {
      return isEmpty(a) && b == null || !isEmpty(a) && b != null && a.getItem() == b;
   }

   public static boolean checkItemEqualityStrict(ItemStack a, ItemStack b) {
      return isEmpty(a) && isEmpty(b)
         || !isEmpty(a) && !isEmpty(b) && a.getItem() == b.getItem() && ItemStacks.rawMeta(a) == ItemStacks.rawMeta(b) && checkNbtEqualityStrict(a, b);
   }

   private static boolean checkNbtEquality(ItemStack a, ItemStack b) {
      return checkNbtEquality(ItemData.read(a), ItemData.read(b));
   }

   public static boolean checkNbtEquality(CompoundTag a, CompoundTag b) {
      if (a == b) {
         return true;
      } else {
         Set<String> keysA = a != null ? a.keySet() : Collections.emptySet();
         Set<String> keysB = b != null ? b.keySet() : Collections.emptySet();
         Set<String> toCheck = new HashSet<>(Math.max(keysA.size(), keysB.size()));

         for (String key : keysA) {
            if (!ignoredNbtKeys.contains(key)) {
               if (!keysB.contains(key)) {
                  return false;
               }

               toCheck.add(key);
            }
         }

         for (String keyx : keysB) {
            if (!ignoredNbtKeys.contains(keyx)) {
               if (!keysA.contains(keyx)) {
                  return false;
               }

               toCheck.add(keyx);
            }
         }

         for (String keyxx : toCheck) {
            if (!a.get(keyxx).equals(b.get(keyxx))) {
               return false;
            }
         }

         return true;
      }
   }

   public static boolean checkNbtEqualityStrict(ItemStack a, ItemStack b) {
      CompoundTag nbtA = ItemData.read(a);
      CompoundTag nbtB = ItemData.read(b);
      return nbtA == nbtB ? true : nbtA != null && nbtB != null && nbtA.equals(nbtB);
   }

   public static ItemStack getPickStack(Level world, BlockPos pos, BlockState state, Player player) {
      ItemStack ret = state.getCloneItemStack(pos, world, false, player);
      return isEmpty(ret) ? emptyStack : ret;
   }

   public static List<ItemStack> getDrops(BlockGetter world, BlockPos pos, BlockState state, int fortune) {
      return getDrops(world, pos, state, state.getBlock(), fortune);
   }

   public static List<ItemStack> getDrops(BlockGetter world, BlockPos pos, BlockState state, Block block, int fortune) {
      NonNullList<ItemStack> drops = NonNullList.create();

      assert world.getBlockState(pos).getBlock() == block;

      if (Util.getWorld(world) instanceof ServerLevel server) {
         drops.addAll(Block.getDrops(state, server, pos, world.getBlockEntity(pos), null, NativeHarvest.tool(server, fortune, false)));
         return drops;
      } else {
         throw new IllegalArgumentException("Block drops require a server level");
      }
   }

   public static List<ItemStack> getDrops(BlockGetter world, BlockPos pos, BlockState state, Player player, int fortune, boolean silkTouch) {
      Block block = state.getBlock();
      if (state.isAir()) {
         return Collections.emptyList();
      } else if (Util.getWorld(world) instanceof ServerLevel server) {
         if (player == null) {
            player = Ic2Player.get(server);
         }

         return Block.getDrops(state, server, pos, world.getBlockEntity(pos), player, NativeHarvest.tool(server, fortune, silkTouch));
      } else {
         throw new IllegalArgumentException("Block drops require a server level");
      }
   }

   public static boolean placeBlock(ItemStack stack, Level world, BlockPos pos) {
      if (isEmpty(stack)) {
         return false;
      } else {
         net.minecraft.world.item.Item item = stack.getItem();
         if (!(item instanceof BlockItem) && !(item instanceof StandingAndWallBlockItem)) {
            return false;
         } else {
            int oldSize = getSize(stack);
            Player player = Ic2Player.get(world);
            InteractionHand hand = InteractionHand.MAIN_HAND;
            ItemStack prev = player.getItemInHand(hand);
            player.setItemInHand(hand, stack);

            InteractionResult result;
            try {
               result = stack.useOn(new UseOnContext(world, player, hand, stack, new BlockHitResult(Vec3.atCenterOf(pos), Direction.DOWN, pos, false)));
            } finally {
               player.setItemInHand(hand, prev);
            }

            stack = setSize(stack, oldSize);
            return result.consumesAction();
         }
      }
   }

   public static boolean isEmpty(ItemStack stack) {
      return stack == emptyStack || stack == null || stack.getItem() == null || stack.getCount() <= 0;
   }

   public static boolean isEmpty(Player player, InteractionHand hand) {
      return isEmpty(player.getItemInHand(hand));
   }

   public static int getSize(ItemStack stack) {
      return isEmpty(stack) ? 0 : stack.getCount();
   }

   public static ItemStack setSize(ItemStack stack, int size) {
      if (size <= 0) {
         return emptyStack;
      } else {
         stack.setCount(size);
         return stack;
      }
   }

   public static ItemStack incSize(ItemStack stack) {
      return incSize(stack, 1);
   }

   public static ItemStack incSize(ItemStack stack, int amount) {
      return setSize(stack, getSize(stack) + amount);
   }

   public static ItemStack decSize(ItemStack stack) {
      return decSize(stack, 1);
   }

   public static ItemStack decSize(ItemStack stack, int amount) {
      return incSize(stack, -amount);
   }

   public static boolean check2(Iterable<List<ItemStack>> list) {
      for (List<ItemStack> list2 : list) {
         if (!check(list2)) {
            return false;
         }
      }

      return true;
   }

   public static boolean check(ItemStack[] array) {
      return check(Arrays.asList(array));
   }

   public static boolean check(Iterable<ItemStack> list) {
      for (ItemStack stack : list) {
         if (!check(stack)) {
            return false;
         }
      }

      return true;
   }

   public static boolean check(ItemStack stack) {
      return stack.getItem() != null;
   }

   public static String toStringSafe2(Iterable<List<ItemStack>> list) {
      StringBuilder ret = new StringBuilder("[");

      for (List<ItemStack> list2 : list) {
         if (ret.length() > 1) {
            ret.append(", ");
         }

         ret.append(toStringSafe(list2));
      }

      return ret.append(']').toString();
   }

   public static String toStringSafe(ItemStack[] array) {
      return toStringSafe(Arrays.asList(array));
   }

   public static String toStringSafe(Iterable<ItemStack> list) {
      StringBuilder ret = new StringBuilder("[");

      for (ItemStack stack : list) {
         if (ret.length() > 1) {
            ret.append(", ");
         }

         ret.append(toStringSafe(stack));
      }

      return ret.append(']').toString();
   }

   public static String toStringSafe(ItemStack stack) {
      if (stack == null) {
         return "(null)";
      } else {
         return stack.getItem() == null ? getSize(stack) + "x(null)@(unknown)" : stack.toString();
      }
   }

   public static boolean storeInventoryItem(ItemStack stack, Player player, boolean simulate) {
      if (!simulate) {
         return player.getInventory().add(stack);
      } else {
         int sizeLeft = getSize(stack);
         int maxStackSize = Math.min(player.getInventory().getMaxStackSize(), stack.getMaxStackSize());

         for (int i = 0; i < player.getInventory().getNonEquipmentItems().size() && sizeLeft > 0; i++) {
            ItemStack invStack = (ItemStack)player.getInventory().getNonEquipmentItems().get(i);
            if (isEmpty(invStack)) {
               sizeLeft -= maxStackSize;
            } else if (checkItemEqualityStrict(stack, invStack) && getSize(invStack) < maxStackSize) {
               sizeLeft -= maxStackSize - getSize(invStack);
            }
         }

         return sizeLeft <= 0;
      }
   }

   public static int getRawMeta(ItemStack stack) {
      return ItemStacks.rawMeta(stack);
   }

   public static void setRawMeta(ItemStack stack, int meta) {
      if (meta < 0) {
         throw new IllegalArgumentException("negative meta");
      } else {
         ItemStacks.setRawMeta(stack, meta);
      }
   }

   public static TIntSet getSlotsFromInv(Container inv) {
      TIntSet set = new TIntHashSet();

      for (int i = 0; i < inv.getContainerSize(); i++) {
         set.add(i);
      }

      return set;
   }

   public static Tuple.T2<List<ItemStack>, ? extends TIntCollection> balanceStacks(Container craftMatrix) {
      return balanceStacks(craftMatrix, Collections.emptySet());
   }

   public static Tuple.T2<List<ItemStack>, ? extends TIntCollection> balanceStacks(Container craftMatrix, ItemStack sourceItemStack) {
      return balanceStacks(craftMatrix, Collections.singleton(sourceItemStack));
   }

   public static Tuple.T2<List<ItemStack>, ? extends TIntCollection> balanceStacks(final Container inv, Collection<ItemStack> additionalItems) {
      return balanceStacks(inv, new Predicate<Tuple.T2<ItemStack, Integer>>() {
         public boolean apply(Tuple.T2<ItemStack, Integer> input) {
            return !StackUtil.isEmpty(inv.getItem(input.b));
         }
      }, getSlotsFromInv(inv), additionalItems);
   }

   public static Tuple.T2<List<ItemStack>, ? extends TIntCollection> balanceStacks(Container inv, Predicate<Tuple.T2<ItemStack, Integer>> canInsert) {
      return balanceStacks(inv, canInsert, getSlotsFromInv(inv), Collections.emptySet());
   }

   public static Tuple.T2<List<ItemStack>, ? extends TIntCollection> balanceStacks(
      Container inv, Predicate<Tuple.T2<ItemStack, Integer>> canInsert, TIntSet originalAvailableSlots, Collection<ItemStack> additionalStacksOriginal
   ) {
      List<ItemStack> additionalStacks = new LinkedList<>(additionalStacksOriginal);
      TIntSet availableSlots = new TIntHashSet(originalAvailableSlots);
      List<ItemStack> leftOvers = new ArrayList<>();

      for (int i = 0; i < inv.getContainerSize(); i++) {
         if (availableSlots.contains(i)) {
            ItemStack stack = inv.getItem(i);
            if (!isEmpty(stack)) {
               int amount = 0;
               ListIterator<ItemStack> iter = additionalStacks.listIterator();

               while (iter.hasNext()) {
                  ItemStack currentStack = iter.next();
                  if (checkItemEqualityStrict(currentStack, stack)) {
                     iter.remove();
                     amount += getSize(currentStack);
                  }
               }

               amount = distributeStackToSlots(inv, stack, availableSlots, canInsert, amount);

               while (amount > 0) {
                  int size = Math.min(stack.getMaxStackSize(), amount);
                  amount -= size;
                  leftOvers.add(copyWithSize(stack, size));
               }
            }
         }
      }

      for (ItemStack stack : additionalStacks) {
         int amount = distributeStackToSlots(inv, stack, availableSlots, canInsert, getSize(stack));
         if (amount > 0) {
            leftOvers.add(copyWithSize(stack, amount));
         }
      }

      originalAvailableSlots.removeAll(availableSlots);
      return new Tuple.T2<>(leftOvers, originalAvailableSlots);
   }

   private static int distributeStackToSlots(
      final Container inv, ItemStack stack, TIntSet availableSlots, Predicate<Tuple.T2<ItemStack, Integer>> canInsert, int amount
   ) {
      TIntList currentWorkingSet = new TIntArrayList();
      TIntIterator iter = availableSlots.iterator();

      while (iter.hasNext()) {
         int currentSlot = iter.next();
         ItemStack currentStack = inv.getItem(currentSlot);
         if ((checkItemEqualityStrict(stack, currentStack) || isEmpty(currentStack)) && canInsert.apply(new Tuple.T2<>(stack, currentSlot))) {
            amount += getSize(currentStack);
            currentWorkingSet.add(currentSlot);
            iter.remove();
         }
      }

      currentWorkingSet.sort();
      int maxStackSize = Math.min(stack.getMaxStackSize(), inv.getMaxStackSize());
      int slotsLeft = currentWorkingSet.size();
      TIntIterator iterx = currentWorkingSet.iterator();

      while (iterx.hasNext() && amount > 0) {
         int currentSlot = iterx.next();
         int itemsToPut = amount / slotsLeft;
         if (amount % slotsLeft > 0) {
            itemsToPut++;
         }

         itemsToPut = Math.min(itemsToPut, maxStackSize);
         inv.setItem(currentSlot, copyWithSize(stack, itemsToPut));
         amount -= itemsToPut;
         slotsLeft--;
         iterx.remove();
      }

      if (!currentWorkingSet.isEmpty()) {
         assert amount <= 0;

         currentWorkingSet.forEach(new TIntProcedure() {
            public boolean execute(int currentSlot) {
               inv.setItem(currentSlot, StackUtil.emptyStack);
               return true;
            }
         });
      }

      assert amount <= 0 || slotsLeft == 0;

      return amount;
   }

   public static ItemStack setImmutableSize(ItemStack stack, int size) {
      if (getSize(stack) != size) {
         stack = copyWithSize(stack, size);
      }

      return stack;
   }

   public static boolean matchesNBT(CompoundTag subject, CompoundTag target) {
      if (subject == null) {
         return target == null || target.isEmpty();
      } else if (target == null) {
         return true;
      } else {
         for (String key : target.keySet()) {
            Tag targetNBT = target.get(key);
            if (!subject.contains(key) || targetNBT.getId() != subject.get(key).getId()) {
               return false;
            }

            Tag subjectNBT = subject.get(key);
            if (!targetNBT.equals(subjectNBT)) {
               return false;
            }
         }

         return true;
      }
   }

   public static ItemStack wrapEmpty(ItemStack stack) {
      return stack == null ? emptyStack : stack;
   }

   public static class AdjacentInv {
      public final BlockEntity te;
      public final Direction dir;

      AdjacentInv(BlockEntity te, Direction dir) {
         this.te = te;
         this.dir = dir;
      }

      public GameProfile getAccessor() {
         return null;
      }
   }

   public static class PersonalAdjacentInv extends StackUtil.AdjacentInv {
      public final GameProfile accessor;

      PersonalAdjacentInv(BlockEntity te, Direction dir, GameProfile accessor) {
         super(te, dir);
         this.accessor = accessor;
      }

      @Override
      public GameProfile getAccessor() {
         return this.accessor;
      }
   }
}
