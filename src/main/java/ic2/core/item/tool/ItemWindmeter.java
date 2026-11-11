package ic2.core.item.tool;

import ic2.api.item.ElectricItem;
import ic2.core.IC2;
import ic2.core.WorldData;
import ic2.core.block.generator.tileentity.TileEntityWindGenerator;
import ic2.core.block.kineticgenerator.tileentity.TileEntityWindKineticGenerator;
import ic2.core.init.Localization;
import ic2.core.item.ItemDefaults;
import ic2.core.profile.NotClassic;
import ic2.core.ref.ItemName;
import ic2.core.util.StackUtil;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@NotClassic
public class ItemWindmeter extends ItemElectricTool {
   public ItemWindmeter() {
      super(ItemName.wind_meter, 50);
      ItemDefaults.stackSize(this, 1);
      this.maxCharge = 10000;
      this.transferLimit = 100;
      this.tier = 1;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void func_77624_a(ItemStack stack, Level world, List<String> tooltip, TooltipFlag advanced) {
      tooltip.add(Localization.translate("ic2.wind_meter.tooltipA"));
      tooltip.add(Localization.translate("ic2.wind_meter.tooltipB"));
   }

   public InteractionResult onItemUseFirst(Player player, Level world, BlockPos pos, Direction side, float hitX, float hitY, float hitZ, InteractionHand hand) {
      if (!world.isClientSide() && !player.isShiftKeyDown()) {
         ItemStack stack = StackUtil.get(player, hand);
         if (!ElectricItem.manager.canUse(stack, this.operationEnergyCost)) {
            return InteractionResult.PASS;
         } else {
            BlockEntity te = world.getBlockEntity(pos);
            if (te instanceof TileEntityWindKineticGenerator windyTE) {
               if (!windyTE.getActive()) {
                  if (windyTE.hasRotor()) {
                     IC2.platform.messagePlayer(player, Localization.translate("ic2.wind_meter.info.rotor.blocked"));
                  } else {
                     IC2.platform.messagePlayer(player, Localization.translate("ic2.wind_meter.info.rotor.none"));
                  }

                  return InteractionResult.FAIL;
               } else {
                  ElectricItem.manager.use(stack, this.operationEnergyCost, player);
                  if (windyTE.getObstructions() >= 0) {
                     float displayWind = roundWind(windyTE.calcWindStrength());
                     if (displayWind <= 0.0F) {
                        IC2.platform.messagePlayer(player, Localization.translate("ic2.wind_meter.info.obstructed", windyTE.getObstructions()));
                     } else {
                        IC2.platform.messagePlayer(player, Localization.translate("ic2.wind_meter.info.effective", displayWind));
                     }
                  } else {
                     IC2.platform.messagePlayer(player, Localization.translate("ic2.wind_meter.info.blocked", windyTE.getRotorDiameter() * 3));
                  }

                  return InteractionResult.SUCCESS;
               }
            } else if (te instanceof TileEntityWindGenerator) {
               ElectricItem.manager.use(stack, this.operationEnergyCost, player);
               TileEntityWindGenerator windyTEx = (TileEntityWindGenerator)te;
               double obstructiveFactor = windyTEx.getObstructions() / 567.0;
               double wind = obstructiveFactor >= 1.0 ? 0.0 : WorldData.get(world).windSim.getWindAt(pos.getY()) * (1.0 - obstructiveFactor);
               float displayWind = roundWind(wind);
               if (displayWind <= 0.0F) {
                  IC2.platform.messagePlayer(player, Localization.translate("ic2.wind_meter.info.obstructed", windyTEx.getObstructions()));
               } else {
                  IC2.platform.messagePlayer(player, Localization.translate("ic2.wind_meter.info.effective", displayWind));
               }

               return InteractionResult.SUCCESS;
            } else {
               return InteractionResult.PASS;
            }
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   public InteractionResult use(Level world, Player player, InteractionHand hand) {
      ItemStack stack = StackUtil.get(player, hand);
      if (!IC2.platform.isSimulating()) {
         return InteractionResult.PASS;
      } else if (!ElectricItem.manager.canUse(stack, this.operationEnergyCost)) {
         return InteractionResult.PASS;
      } else {
         ElectricItem.manager.use(stack, this.operationEnergyCost, player);
         double windStrength = WorldData.get(world).windSim.getWindAt(player.getY());
         if (windStrength < 0.0) {
            windStrength = 0.0;
         }

         IC2.platform.messagePlayer(player, Localization.translate("ic2.wind_meter.info", roundWind(windStrength)));
         return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
      }
   }

   private static float roundWind(double windStrength) {
      return (float)Math.round(windStrength * 100.0) / 100.0F;
   }
}
