package ic2.core.item.tool;

import ic2.api.crops.CropCard;
import ic2.api.item.ElectricItem;
import ic2.core.IC2;
import ic2.core.IHasGui;
import ic2.core.crop.TileEntityCrop;
import ic2.core.init.Localization;
import ic2.core.item.BaseElectricItem;
import ic2.core.item.IHandHeldInventory;
import ic2.core.ref.ItemName;
import ic2.core.util.StackUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ItemCropnalyzer extends BaseElectricItem implements IHandHeldInventory {
   public ItemCropnalyzer() {
      super(ItemName.cropnalyzer, 100000.0, 128.0, 2);
   }

   public InteractionResult use(Level world, Player player, InteractionHand hand) {
      ItemStack stack = StackUtil.get(player, hand);
      if (IC2.platform.isSimulating()) {
         IC2.platform.launchGui(player, this.getInventory(player, stack));
      }

      return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Rarity func_77613_e(ItemStack stack) {
      return Rarity.UNCOMMON;
   }

   @Override
   public IHasGui getInventory(Player player, ItemStack stack) {
      return new HandHeldCropnalyzer(player, stack);
   }

   public boolean onDroppedByPlayer(ItemStack stack, Player player) {
      if (player instanceof ServerPlayer && !StackUtil.isEmpty(stack) && player.containerMenu instanceof ContainerCropnalyzer) {
         HandHeldCropnalyzer cropnalyzer = ((ContainerCropnalyzer)player.containerMenu).base;
         if (cropnalyzer.isThisContainer(stack)) {
            cropnalyzer.saveAsThrown(stack);
            ((ServerPlayer)player).closeContainer();
         }
      }

      return true;
   }

   public InteractionResult onItemUseFirst(Player player, Level world, BlockPos pos, Direction side, float hitX, float hitY, float hitZ, InteractionHand hand) {
      if (!world.isClientSide() && !player.isShiftKeyDown()) {
         if (world.getBlockEntity(pos) instanceof TileEntityCrop crop) {
            if (crop.getCrop() == null) {
               return InteractionResult.PASS;
            }

            if (ElectricItem.manager.discharge(StackUtil.get(player, hand), HandHeldCropnalyzer.energyForLevel(2), 3, true, false, false) > 0.0) {
               CropCard plant = crop.getCrop();
               IC2.platform.messagePlayer(player, "Crop name: " + Localization.translate(plant.getUnlocalizedName()) + " (by " + plant.getDiscoveredBy() + ")");
               IC2.platform.messagePlayer(player, "Crop size: " + crop.getCurrentSize() + "/" + plant.getMaxSize());
               IC2.platform.messagePlayer(player, "Nutrient storage: " + crop.getStorageNutrients() + "/100");
               IC2.platform.messagePlayer(player, "Water storage: " + crop.getStorageWater() + "/200");
               IC2.platform.messagePlayer(player, "Weed-Ex storage: " + crop.getStorageWeedEX() + "/100");
               IC2.platform.messagePlayer(player, "Growth points: " + crop.getGrowthPoints() + "/" + plant.getGrowthDuration(crop));
               return InteractionResult.SUCCESS;
            }
         }

         return InteractionResult.PASS;
      } else {
         return InteractionResult.PASS;
      }
   }
}
