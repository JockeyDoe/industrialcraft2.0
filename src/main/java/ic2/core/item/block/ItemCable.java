package ic2.core.item.block;

import ic2.api.item.IBoxable;
import ic2.core.IC2;
import ic2.core.block.BlockTileEntity;
import ic2.core.block.TileEntityBlock;
import ic2.core.block.wiring.CableType;
import ic2.core.block.wiring.TileEntityCable;
import ic2.core.block.wiring.TileEntityCableDetector;
import ic2.core.block.wiring.TileEntityCableSplitter;
import ic2.core.block.wiring.TileEntityClassicCable;
import ic2.core.init.Localization;
import ic2.core.item.ItemDefaults;
import ic2.core.item.ItemIC2;
import ic2.core.item.tool.ItemToolPainter;
import ic2.core.model.ModelKey;
import ic2.core.model.ModelRegistry;
import ic2.core.ref.BlockName;
import ic2.core.ref.IMultiItem;
import ic2.core.ref.ItemName;
import ic2.core.ref.TeBlock;
import ic2.core.util.Ic2Color;
import ic2.core.util.ItemData;
import ic2.core.util.ItemStacks;
import ic2.core.util.LogCategory;
import ic2.core.util.NativePlacement;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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

public class ItemCable extends ItemIC2 implements IMultiItem<CableType>, IBoxable {
   private final List<ItemStack> variants = new ArrayList<>();
   private static final NumberFormat lossFormat = new DecimalFormat("0.00#");

   public ItemCable() {
      super(ItemName.cable);
      ItemDefaults.variants(this, true);
   }

   private List<ItemStack> variants() {
      if (!this.variants.isEmpty()) {
         return this.variants;
      } else {
         for (CableType type : CableType.values) {
            for (int insulation = 0; insulation <= type.maxInsulation; insulation++) {
               this.variants.add(getCable(type, insulation));
            }
         }

         return this.variants;
      }
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void registerModels(ItemName name) {
      final Identifier loc = Util.getName(this);
      ModelRegistry.setCustomMeshDefinition(this, new ModelRegistry.ItemMeshDefinition() {
         {
            Objects.requireNonNull(ItemCable.this);
         }

         @Override
         public ModelKey func_178113_a(ItemStack stack) {
            return ItemCable.getModelLocation(loc, stack);
         }
      });

      for (ItemStack stack : this.variants()) {
         ModelRegistry.registerItemVariants(this, new ModelKey[]{getModelLocation(loc, stack)});
      }
   }

   static ModelKey getModelLocation(Identifier loc, ItemStack stack) {
      return new ModelKey(Identifier.fromNamespaceAndPath(loc.getNamespace(), loc.getPath() + "/" + getVariantName(stack)), null);
   }

   public ItemStack getItemStack(CableType type) {
      return getCable(type, 0);
   }

   @Override
   public ItemStack getItemStack(String variant) {
      int pos = 0;
      CableType type = null;
      int insulation = 0;

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
            type = CableType.get(value);
            if (type == null) {
               IC2.log.warn(LogCategory.Item, "Invalid cable type: %s", value);
            }
         } else if (key.equals("insulation")) {
            try {
               insulation = Integer.valueOf(value);
            } catch (NumberFormatException var10) {
               IC2.log.warn(LogCategory.Item, "Invalid cable insulation: %s", value);
            }
         }

         pos = nextPos + 1;
      }

      if (type == null) {
         return null;
      } else if (insulation >= 0 && insulation <= type.maxInsulation) {
         return getCable(type, insulation);
      } else {
         IC2.log.warn(LogCategory.Item, "Invalid cable insulation: %d", insulation);
         return null;
      }
   }

   @Override
   public String getVariant(ItemStack stack) {
      if (stack == null) {
         throw new NullPointerException("null stack");
      } else if (stack.getItem() != this) {
         throw new IllegalArgumentException("The stack " + stack + " doesn't match " + this);
      } else {
         CableType type = getCableType(stack);
         int insulation = getInsulation(stack);
         return "type:" + type.getName() + ",insulation:" + insulation;
      }
   }

   public static ItemStack getCable(CableType type, int insulation) {
      ItemStack ret = ItemStacks.create(ItemName.cable.getInstance(), 1, type.getId());
      CompoundTag nbt = StackUtil.getOrCreateNbtData(ret);
      nbt.putByte("type", (byte)type.ordinal());
      ItemData.commit(ret, nbt);
      nbt.putByte("insulation", (byte)insulation);
      ItemData.commit(ret, nbt);
      return ret;
   }

   private static CableType getCableType(ItemStack stack) {
      CompoundTag nbt = StackUtil.getOrCreateNbtData(stack);
      int type = nbt.getByteOr("type", (byte)0) & 255;
      return type < CableType.values.length ? CableType.values[type] : CableType.copper;
   }

   private static int getInsulation(ItemStack stack) {
      CableType type = getCableType(stack);
      CompoundTag nbt = StackUtil.getOrCreateNbtData(stack);
      int insulation = nbt.getByteOr("insulation", (byte)0) & 255;
      return Math.min(insulation, type.maxInsulation);
   }

   private static String getVariantName(ItemStack stack) {
      CableType type = getCableType(stack);
      int insulation = getInsulation(stack);
      return type.getName(insulation, null);
   }

   @Override
   public String func_77667_c(ItemStack stack) {
      return super.func_77667_c(stack) + "." + getVariantName(stack);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void func_77624_a(ItemStack stack, Level world, List<String> info, TooltipFlag b) {
      CableType type = getCableType(stack);
      int capacity;
      double loss;
      if (!IC2.version.isClassic()) {
         capacity = type.capacity;
         loss = type.loss;
      } else {
         capacity = TileEntityClassicCable.getCableCapacity(type);
         loss = TileEntityClassicCable.getConductionLoss(type, getInsulation(stack));
      }

      info.add(capacity + " " + Localization.translate("ic2.generic.text.EUt"));
      info.add(Localization.translate("ic2.cable.tooltip.loss", lossFormat.format(loss)));
   }

   @Override
   public InteractionResult func_180614_a(Player player, Level world, BlockPos pos, InteractionHand hand, Direction side, float hitX, float hitY, float hitZ) {
      ItemStack stack = StackUtil.get(player, hand);
      BlockState oldState = world.getBlockState(pos);
      Block oldBlock = oldState.getBlock();
      if (!oldState.canBeReplaced()) {
         pos = pos.relative(side);
      }

      Block newBlock = BlockName.te.getInstance();
      if (!StackUtil.isEmpty(stack)
         && player.mayUseItemAt(pos, side, stack)
         && NativePlacement.canPlace(newBlock, world, pos, side, player, hand, false)
         && ((BlockTileEntity)newBlock).canReplace(world, pos, side, BlockName.te.getItemStack(TeBlock.cable))) {
         NativePlacement.state(newBlock, world, pos, side, player, hand, hitX, hitY, hitZ);
         CableType type = getCableType(stack);
         int insulation = getInsulation(stack);
         Runnable damage = null;

         if (ItemBlockTileEntity.placeTeBlock(stack, player, world, pos, side, switch (type) {
            case detector -> (TileEntityCable)TileEntityBlock.instantiate(TileEntityCableDetector.delegate());
            case splitter -> (TileEntityCable)TileEntityBlock.instantiate(TileEntityCableSplitter.delegate());
            default -> {
               label33: {
                  if (hand == InteractionHand.MAIN_HAND) {
                     ItemStack offStack = StackUtil.get(player, InteractionHand.OFF_HAND);
                     if (!StackUtil.isEmpty(offStack) && offStack.getItem() == ItemName.painter.getInstance()) {
                        ItemToolPainter painter = (ItemToolPainter)offStack.getItem();
                        Ic2Color color = painter.getColor(offStack);
                        if (color != null) {
                           damage = () -> painter.damagePainter(player, InteractionHand.OFF_HAND, color);
                           yield TileEntityCable.delegate(type, insulation, color);
                           break label33;
                        }
                     }
                  }

                  yield TileEntityCable.delegate(type, insulation);
               }
            }
         })) {
            SoundType soundtype = newBlock.getSoundType(world.getBlockState(pos), world, pos, player);
            world.playSound(player, pos, soundtype.getPlaceSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
            StackUtil.consumeOrError(player, hand, 1);
            if (damage != null) {
               damage.run();
            }
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
         if (IC2.version.isClassic()) {
            variants.remove(11);
         }

         itemList.addAll(variants);
      }
   }

   @Override
   public Set<CableType> getAllTypes() {
      return EnumSet.allOf(CableType.class);
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
