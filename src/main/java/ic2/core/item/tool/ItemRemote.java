package ic2.core.item.tool;

import ic2.core.IC2;
import ic2.core.audio.PositionSpec;
import ic2.core.block.BlockDynamite;
import ic2.core.item.ItemDefaults;
import ic2.core.item.ItemIC2;
import ic2.core.ref.BlockName;
import ic2.core.ref.ItemName;
import ic2.core.util.ItemData;
import ic2.core.util.ItemStacks;
import ic2.core.util.NbtChecks;
import ic2.core.util.StackUtil;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ItemRemote extends ItemIC2 {
   public ItemRemote() {
      super(ItemName.remote);
      ItemDefaults.stackSize(this, 1);
   }

   @Override
   public InteractionResult func_180614_a(Player player, Level world, BlockPos pos, InteractionHand hand, Direction side, float hitX, float hitY, float hitZ) {
      if (world.isClientSide()) {
         return InteractionResult.SUCCESS;
      } else {
         BlockState state = world.getBlockState(pos);
         Block block = state.getBlock();
         if (block != BlockName.dynamite.getInstance()) {
            return InteractionResult.SUCCESS;
         } else {
            ItemStack stack = StackUtil.get(player, hand);
            if (!(Boolean)state.getValue(BlockDynamite.linked)) {
               addRemote(pos, stack);
               world.setBlock(pos, (BlockState)state.setValue(BlockDynamite.linked, true), 3);
            } else {
               int index = hasRemote(pos, stack);
               if (index > -1) {
                  world.setBlock(pos, (BlockState)state.setValue(BlockDynamite.linked, false), 3);
                  removeRemote(index, stack);
               } else {
                  IC2.platform.messagePlayer(player, "This dynamite stick is not linked to this remote, cannot unlink.");
               }
            }

            return InteractionResult.SUCCESS;
         }
      }
   }

   public InteractionResult use(Level world, Player player, InteractionHand hand) {
      ItemStack stack = StackUtil.get(player, hand);
      if (world.isClientSide()) {
         return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
      } else {
         IC2.audioManager.playOnce(player, PositionSpec.Hand, "Tools/dynamiteomote.ogg", true, IC2.audioManager.getDefaultVolume());
         launchRemotes(world, stack, player);
         return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
      }
   }

   public static void addRemote(BlockPos pos, ItemStack freq) {
      CompoundTag compound = StackUtil.getOrCreateNbtData(freq);
      if (!compound.contains("coords")) {
         compound.put("coords", new ListTag());
         ItemData.commit(freq, compound);
      }

      ListTag coords = NbtChecks.getList(compound, "coords", 10);
      CompoundTag coord = new CompoundTag();
      coord.putInt("x", pos.getX());
      coord.putInt("y", pos.getY());
      coord.putInt("z", pos.getZ());
      coords.add(coord);
      ItemData.commit(freq, compound);
      compound.put("coords", coords);
      ItemData.commit(freq, compound);
      ItemStacks.setRawMeta(freq, coords.size());
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void func_77624_a(ItemStack stack, Level world, List<String> tooltip, TooltipFlag advanced) {
      if (ItemStacks.rawMeta(stack) > 0) {
         tooltip.add("Linked to " + ItemStacks.rawMeta(stack) + " dynamite");
      }
   }

   public static void launchRemotes(Level world, ItemStack freq, Player player) {
      CompoundTag compound = StackUtil.getOrCreateNbtData(freq);
      if (compound.contains("coords")) {
         ListTag coords = NbtChecks.getList(compound, "coords", 10);
         int i = 0;

         while (i < coords.size()) {
            CompoundTag coord = coords.getCompoundOrEmpty(i);
            BlockPos pos = new BlockPos(coord.getIntOr("x", 0), coord.getIntOr("y", 0), coord.getIntOr("z", 0));
            if (world.hasChunkAt(pos)) {
               BlockState state = world.getBlockState(pos);
               if (state.getBlock() == BlockName.dynamite.getInstance() && (Boolean)state.getValue(BlockDynamite.linked)) {
                  state.onDestroyedByPlayer(world, pos, player, player.getMainHandItem(), false, state.getFluidState());
                  world.removeBlock(pos, false);
               }

               coords.remove(i);
               ItemData.commit(freq, compound);
            } else {
               i++;
            }
         }

         ItemStacks.setRawMeta(freq, 0);
      }
   }

   public static int hasRemote(BlockPos pos, ItemStack freq) {
      CompoundTag compound = StackUtil.getOrCreateNbtData(freq);
      if (!compound.contains("coords")) {
         return -1;
      } else {
         ListTag coords = NbtChecks.getList(compound, "coords", 10);

         for (int i = 0; i < coords.size(); i++) {
            CompoundTag coord = coords.getCompoundOrEmpty(i);
            if (coord.getIntOr("x", 0) == pos.getX() && coord.getIntOr("y", 0) == pos.getY() && coord.getIntOr("z", 0) == pos.getZ()) {
               return i;
            }
         }

         return -1;
      }
   }

   public static void removeRemote(int index, ItemStack freq) {
      CompoundTag compound = StackUtil.getOrCreateNbtData(freq);
      if (compound.contains("coords")) {
         ListTag coords = NbtChecks.getList(compound, "coords", 10);
         ListTag newCoords = new ListTag();

         for (int i = 0; i < coords.size(); i++) {
            if (i != index) {
               newCoords.add(coords.getCompoundOrEmpty(i));
            }
         }

         compound.put("coords", newCoords);
         ItemData.commit(freq, compound);
         ItemStacks.setRawMeta(freq, newCoords.size());
      }
   }
}
