package ic2.core.item;

import ic2.api.crops.CropCard;
import ic2.api.crops.Crops;
import ic2.api.crops.ICropSeed;
import ic2.core.crop.TileEntityCrop;
import ic2.core.crop.cropcard.GenericCropCard;
import ic2.core.init.Localization;
import ic2.core.ref.ItemName;
import ic2.core.util.ItemData;
import ic2.core.util.NbtChecks;
import ic2.core.util.StackUtil;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ItemCropSeed extends ItemIC2 implements ICropSeed {
   public ItemCropSeed() {
      super(ItemName.crop_seed_bag);
      ItemDefaults.stackSize(this, 1);
   }

   @Override
   public String func_77667_c(ItemStack itemstack) {
      if (itemstack == null) {
         return "ic2.crop.unknown";
      } else {
         CropCard cropCard = Crops.instance.getCropCard(itemstack);
         int level = this.getScannedFromStack(itemstack);
         if (level == 0) {
            return "ic2.crop.unknown";
         } else {
            return level >= 0 && cropCard != null ? cropCard.getUnlocalizedName() : "ic2.crop.invalid";
         }
      }
   }

   @Override
   public String func_77653_i(ItemStack stack) {
      CropCard crop = Crops.instance.getCropCard(stack);
      return Localization.translate(crop == null ? "ic2.crop.seeds" : crop.getSeedType(), super.func_77653_i(stack));
   }

   @Override
   public Component getName(ItemStack stack) {
      CropCard crop = Crops.instance.getCropCard(stack);
      return Component.translatable(crop == null ? "ic2.crop.seeds" : crop.getSeedType(), new Object[]{super.getName(stack)});
   }

   public boolean func_77645_m() {
      return true;
   }

   public boolean isRepairable() {
      return false;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void func_77624_a(ItemStack stack, Level world, List<String> info, TooltipFlag debugTooltips) {
      if (this.getScannedFromStack(stack) >= 4) {
         info.add("Ã‚Â§2GrÃ‚Â§7 " + this.getGrowthFromStack(stack));
         info.add("Ã‚Â§6GaÃ‚Â§7 " + this.getGainFromStack(stack));
         info.add("Ã‚Â§3ReÃ‚Â§7 " + this.getResistanceFromStack(stack));
      }

      if (this.getScannedFromStack(stack) >= 1) {
         CropCard cropCard = this.getCropFromStack(stack);
         if (cropCard instanceof GenericCropCard) {
            info.addAll(((GenericCropCard)cropCard).getInformation());
         }
      }
   }

   @Override
   public InteractionResult func_180614_a(Player player, Level world, BlockPos pos, InteractionHand hand, Direction side, float a, float b, float c) {
      if (world.getBlockEntity(pos) instanceof TileEntityCrop crop) {
         ItemStack stack = StackUtil.get(player, hand);
         if (crop.tryPlantIn(
            Crops.instance.getCropCard(stack),
            1,
            this.getGrowthFromStack(stack),
            this.getGainFromStack(stack),
            this.getResistanceFromStack(stack),
            this.getScannedFromStack(stack)
         )) {
            if (!player.getAbilities().instabuild) {
               player.getInventory().getNonEquipmentItems().set(player.getInventory().getSelectedSlot(), StackUtil.emptyStack);
            }

            return InteractionResult.SUCCESS;
         }
      }

      return InteractionResult.PASS;
   }

   @Override
   public void func_150895_a(CreativeModeTab tabs, NonNullList<ItemStack> items) {
      if (this.func_194125_a(tabs)) {
         for (CropCard crop : Crops.instance.getCrops()) {
            items.add(generateItemStackFromValues(crop, 1, 1, 1, 4));
         }
      }
   }

   public static ItemStack generateItemStackFromValues(CropCard crop, int statGrowth, int statGain, int statResistance, int scan) {
      ItemStack stack = ItemName.crop_seed_bag.getItemStack();
      CompoundTag tag = new CompoundTag();
      tag.putString("owner", crop.getOwner());
      tag.putString("id", crop.getId());
      tag.putByte("growth", (byte)statGrowth);
      tag.putByte("gain", (byte)statGain);
      tag.putByte("resistance", (byte)statResistance);
      tag.putByte("scan", (byte)scan);
      ItemData.store(stack, tag);
      return stack;
   }

   @Override
   public CropCard getCropFromStack(ItemStack is) {
      CompoundTag nbt = ItemData.read(is);
      if (nbt != null && NbtChecks.hasType(nbt, "owner", 8) && NbtChecks.hasType(nbt, "id", 8)) {
         String owner = nbt.getStringOr("owner", "");
         String id = nbt.getStringOr("id", "");
         return Crops.instance.getCropCard(owner, id);
      } else {
         return null;
      }
   }

   @Override
   public void setCropFromStack(ItemStack is, CropCard crop) {
      if (ItemData.read(is) != null) {
         ItemData.put(is, "owner", StringTag.valueOf(crop.getOwner()));
         ItemData.put(is, "id", StringTag.valueOf(crop.getId()));
      }
   }

   @Override
   public int getGrowthFromStack(ItemStack is) {
      return ItemData.read(is) == null ? -1 : ItemData.read(is).getByteOr("growth", (byte)0);
   }

   @Override
   public void setGrowthFromStack(ItemStack is, int value) {
      if (ItemData.read(is) != null) {
         ItemData.put(is, "growth", ByteTag.valueOf((byte)value));
      }
   }

   @Override
   public int getGainFromStack(ItemStack is) {
      return ItemData.read(is) == null ? -1 : ItemData.read(is).getByteOr("gain", (byte)0);
   }

   @Override
   public void setGainFromStack(ItemStack is, int value) {
      if (ItemData.read(is) != null) {
         ItemData.put(is, "gain", ByteTag.valueOf((byte)value));
      }
   }

   @Override
   public int getResistanceFromStack(ItemStack is) {
      return ItemData.read(is) == null ? -1 : ItemData.read(is).getByteOr("resistance", (byte)0);
   }

   @Override
   public void setResistanceFromStack(ItemStack is, int value) {
      if (ItemData.read(is) != null) {
         ItemData.put(is, "resistance", ByteTag.valueOf((byte)value));
      }
   }

   @Override
   public int getScannedFromStack(ItemStack is) {
      return ItemData.read(is) == null ? -1 : ItemData.read(is).getByteOr("scan", (byte)0);
   }

   @Override
   public void setScannedFromStack(ItemStack is, int value) {
      if (ItemData.read(is) != null) {
         ItemData.put(is, "scan", ByteTag.valueOf((byte)value));
      }
   }

   @Override
   public void incrementScannedFromStack(ItemStack is) {
      if (ItemData.read(is) != null) {
         ItemData.put(is, "scan", ByteTag.valueOf((byte)(this.getScannedFromStack(is) + 1)));
      }
   }
}
