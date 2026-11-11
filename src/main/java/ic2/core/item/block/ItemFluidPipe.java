package ic2.core.item.block;

import ic2.api.item.IBoxable;
import ic2.core.IC2;
import ic2.core.block.BlockTileEntity;
import ic2.core.block.transport.TileEntityFluidPipe;
import ic2.core.block.transport.items.PipeSize;
import ic2.core.block.transport.items.PipeType;
import ic2.core.item.ItemDefaults;
import ic2.core.item.ItemIC2;
import ic2.core.model.ModelKey;
import ic2.core.model.ModelRegistry;
import ic2.core.ref.BlockName;
import ic2.core.ref.IMultiItem;
import ic2.core.ref.ItemName;
import ic2.core.ref.TeBlock;
import ic2.core.util.ItemData;
import ic2.core.util.ItemStacks;
import ic2.core.util.LogCategory;
import ic2.core.util.NativePlacement;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ItemFluidPipe extends ItemIC2 implements IMultiItem<PipeType>, IBoxable {
   private final List<ItemStack> variants = new ArrayList<>();

   public ItemFluidPipe() {
      super(ItemName.pipe);
      ItemDefaults.variants(this, true);
   }

   private List<ItemStack> variants() {
      if (!this.variants.isEmpty()) {
         return this.variants;
      } else {
         for (PipeType type : PipeType.values) {
            for (PipeSize pipeSize : PipeSize.values) {
               this.variants.add(getPipe(type, pipeSize));
            }
         }

         return this.variants;
      }
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void registerModels(ItemName name) {
      Identifier loc = Util.getName(this);
      ModelRegistry.setCustomMeshDefinition(this, stackx -> getModelLocation(loc, stackx));

      for (ItemStack stack : this.variants()) {
         ModelRegistry.registerItemVariants(this, new ModelKey[]{getModelLocation(loc, stack)});
      }
   }

   private static ModelKey getModelLocation(Identifier loc, ItemStack itemStack) {
      return new ModelKey(Identifier.fromNamespaceAndPath(loc.getNamespace(), loc.getPath() + "/pipe_" + getSize(itemStack).name()), null);
   }

   public ItemStack getItemStack(PipeType type) {
      return getPipe(type, PipeSize.small);
   }

   @Override
   public ItemStack getItemStack(String variant) {
      int pos = 0;
      PipeType type = null;
      PipeSize size = null;

      while (pos < variant.length()) {
         int nextPos = variant.indexOf(44, pos);
         if (nextPos == -1) {
            nextPos = variant.length();
         }

         int sepPos = variant.indexOf(58, pos);
         if (sepPos == -1 || sepPos >= nextPos) {
            return null;
         }

         String key = variant.substring(pos, sepPos);
         String value = variant.substring(sepPos + 1, nextPos);
         if (key.equals("type")) {
            type = PipeType.get(value);
            if (type == null) {
               IC2.log.warn(LogCategory.Item, "Invalid pipe type: %s", value);
            }
         } else if (key.equals("size")) {
            size = PipeSize.get(value);
            if (size == null) {
               IC2.log.warn(LogCategory.Item, "Invalid pipe size: %s", value);
            }
         }

         pos = nextPos + 1;
      }

      if (type == null) {
         return null;
      } else {
         return size == null ? null : getPipe(type, size);
      }
   }

   @Override
   public String getVariant(ItemStack itemStack) {
      if (itemStack == null) {
         throw new NullPointerException("null stack");
      } else if (itemStack.getItem() != this) {
         throw new IllegalArgumentException("The stack " + itemStack + " doesn't match " + this);
      } else {
         PipeType type = getPipeType(itemStack);
         PipeSize size = getSize(itemStack);
         return "type:" + type.getName() + ", size:" + size.getName();
      }
   }

   public static ItemStack getPipe(PipeType type, PipeSize size) {
      ItemStack ret = ItemStacks.create(ItemName.pipe.getInstance(), 1, type.getId());
      CompoundTag nbt = StackUtil.getOrCreateNbtData(ret);
      nbt.putByte("type", (byte)type.ordinal());
      ItemData.commit(ret, nbt);
      nbt.putByte("size", (byte)size.ordinal());
      ItemData.commit(ret, nbt);
      return ret;
   }

   public static PipeType getPipeType(ItemStack stack) {
      CompoundTag nbt = StackUtil.getOrCreateNbtData(stack);
      int type = nbt.getByteOr("type", (byte)0) & 255;
      return type < PipeType.values.length ? PipeType.values[type] : PipeType.bronze;
   }

   private static PipeSize getSize(ItemStack stack) {
      CompoundTag nbt = StackUtil.getOrCreateNbtData(stack);
      int size = nbt.getByteOr("size", (byte)0) & 255;
      return size < PipeSize.values.length ? PipeSize.values[size] : PipeSize.small;
   }

   @Override
   public String func_77667_c(ItemStack stack) {
      return super.func_77667_c(stack) + "." + getPipeType(stack).getName(getSize(stack));
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void func_77624_a(ItemStack itemStack, Level world, List<String> info, TooltipFlag b) {
      PipeType type = getPipeType(itemStack);
      PipeSize size = getSize(itemStack);
      info.add(ChatFormatting.WHITE + "Transfer rate: " + (int)(type.transferRate * size.multiplier) + " mb/sec");
      info.add(ChatFormatting.WHITE + "Inner capacity: " + (int)(type.transferRate * size.multiplier) + " mb");
      info.add(ChatFormatting.GOLD + "Make connections with a wrench");
   }

   @Override
   public InteractionResult func_180614_a(Player player, Level world, BlockPos pos, InteractionHand hand, Direction side, float hitX, float hitY, float hitZ) {
      ItemStack itemStack = StackUtil.get(player, hand);
      BlockState oldState = world.getBlockState(pos);
      Block oldBlock = oldState.getBlock();
      if (!oldState.canBeReplaced()) {
         pos = pos.relative(side);
      }

      Block newBlock = BlockName.te.getInstance();
      if (!StackUtil.isEmpty(itemStack)
         && player.mayUseItemAt(pos, side, itemStack)
         && NativePlacement.canPlace(newBlock, world, pos, side, player, hand, false)
         && ((BlockTileEntity)newBlock).canReplace(world, pos, side, BlockName.te.getItemStack(TeBlock.fluid_pipe))) {
         NativePlacement.state(newBlock, world, pos, side, player, hand, hitX, hitY, hitZ);
         PipeType type = getPipeType(itemStack);
         PipeSize size = getSize(itemStack);
         TileEntityFluidPipe tileEntity = new TileEntityFluidPipe(type, size);
         if (ItemBlockTileEntity.placeTeBlock(itemStack, player, world, pos, side, tileEntity)) {
            SoundType soundtype = newBlock.getSoundType(world.getBlockState(pos), world, pos, player);
            world.playSound(player, pos, soundtype.getPlaceSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
            StackUtil.consumeOrError(player, hand, 1);
         }

         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   public void func_150895_a(CreativeModeTab tab, NonNullList<ItemStack> itemList) {
      if (this.func_194125_a(tab)) {
         List<ItemStack> variants = new ArrayList<>(this.variants());
         itemList.addAll(variants);
      }
   }

   @Override
   public Set<PipeType> getAllTypes() {
      return EnumSet.allOf(PipeType.class);
   }

   @Override
   public Set<ItemStack> getAllStacks() {
      return new HashSet<>(this.variants());
   }

   @Override
   public boolean canBeStoredInToolbox(ItemStack itemstack) {
      return true;
   }
}
