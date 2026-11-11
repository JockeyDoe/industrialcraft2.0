package ic2.core;

import ic2.api.item.ElectricItem;
import ic2.api.item.HudMode;
import ic2.api.item.IElectricItem;
import ic2.api.item.IItemHudInfo;
import ic2.api.item.IItemHudProvider;
import ic2.core.ref.ItemName;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Post;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public class GuiOverlayer {
   private final Minecraft mc;
   private static final Identifier background = Identifier.fromNamespaceAndPath("ic2", "textures/gui/guioverlay.png");

   public GuiOverlayer(Minecraft mc) {
      this.mc = mc;
   }

   @SubscribeEvent
   public void onRenderHotBar(Post event) {
      if (this.mc.player != null) {
         GuiGraphicsExtractor graphics = event.getGuiGraphics();
         if (event.getName().equals(VanillaGuiLayers.HOTBAR)) {
            ItemStack helm = this.mc.player.getItemBySlot(EquipmentSlot.HEAD);
            if (!StackUtil.isEmpty(helm) && helm.getItem() instanceof IItemHudProvider && ((IItemHudProvider)helm.getItem()).doesProvideHUD(helm)) {
               HudMode hudMode = ((IItemHudProvider)helm.getItem()).getHudMode(helm);
               if (hudMode.shouldDisplay()) {
                  ItemStack boots = this.mc.player.getItemBySlot(EquipmentSlot.FEET);
                  ItemStack legs = this.mc.player.getItemBySlot(EquipmentSlot.LEGS);
                  ItemStack chestplate = this.mc.player.getItemBySlot(EquipmentSlot.CHEST);
                  graphics.blit(RenderPipelines.GUI_TEXTURED, background, 0, 0, 0.0F, 0.0F, 71, 69, 256, 256);
                  graphics.item(helm, 5, 4);
                  graphics.text(this.mc.font, mapCharge(helm) + "%", 25, 9, -1);
                  if (StackUtil.getOrCreateNbtData(helm).getBooleanOr("Nightvision", false)) {
                     graphics.item(ItemName.nightvision_goggles.getItemStack(), 50, 4);
                  }

                  if (!StackUtil.isEmpty(chestplate)) {
                     int charge = getCharge(chestplate);
                     if (charge >= 0) {
                        graphics.text(this.mc.font, charge + "%", 25, 25, -1);
                        graphics.item(chestplate, 5, 20);
                        CompoundTag nbtDatachestplate = StackUtil.getOrCreateNbtData(chestplate);
                        if (nbtDatachestplate.getBooleanOr("jetpack", false)) {
                           ItemStack jetpack;
                           if (nbtDatachestplate.getBooleanOr("hoverMode", false)) {
                              jetpack = ItemName.jetpack_electric.getItemStack();
                           } else {
                              jetpack = ItemName.jetpack.getItemStack();
                           }

                           graphics.item(jetpack, 50, 20);
                        }
                     }
                  }

                  if (!StackUtil.isEmpty(legs)) {
                     int charge = getCharge(legs);
                     if (charge >= 0) {
                        graphics.text(this.mc.font, charge + "%", 25, 41, -1);
                        graphics.item(legs, 5, 36);
                     }
                  }

                  if (!StackUtil.isEmpty(boots)) {
                     int charge = getCharge(boots);
                     if (charge >= 0) {
                        graphics.text(this.mc.font, charge + "%", 25, 56, -1);
                        graphics.item(boots, 5, 52);
                     }
                  }

                  if (hudMode.hasTooltip()) {
                     ItemStack rightItem = this.mc.player.getMainHandItem();
                     ItemStack leftItem = this.mc.player.getOffhandItem();
                     int nextLine = 83;
                     if (!StackUtil.isEmpty(rightItem)) {
                        graphics.item(rightItem, 5, 74);
                        graphics.text(this.mc.font, rightItem.getHoverName().getString(), 30, 78, -1);
                        List<String> info = new LinkedList<>();
                        if (rightItem.getItem() instanceof IItemHudInfo) {
                           info.addAll(((IItemHudInfo)rightItem.getItem()).getHudInfo(rightItem, hudMode == HudMode.ADVANCED));
                           if (info.size() > 0) {
                              for (int l = 0; l < info.size(); l++) {
                                 graphics.text(this.mc.font, info.get(l), 8, 83 + (l + 1) * 14, -1);
                              }
                           }

                           nextLine += (info.size() + 1) * 14;
                        } else {
                           info.addAll(
                              rightItem.getTooltipLines(
                                    TooltipContext.of(this.mc.level), this.mc.player, hudMode == HudMode.ADVANCED ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL
                                 )
                                 .stream()
                                 .map(Component::getString)
                                 .toList()
                           );
                           if (info.size() > 1) {
                              for (int l = 1; l < info.size(); l++) {
                                 graphics.text(this.mc.font, info.get(l), 8, 83 + l * 14, -1);
                              }
                           }

                           nextLine += info.size() * 14;
                        }

                        nextLine += 8;
                     }

                     if (!StackUtil.isEmpty(leftItem)) {
                        graphics.item(leftItem, 5, nextLine - 9);
                        graphics.text(this.mc.font, leftItem.getHoverName().getString(), 30, nextLine - 5, -1);
                        List<String> info = new LinkedList<>();
                        if (leftItem.getItem() instanceof IItemHudInfo) {
                           info.addAll(((IItemHudInfo)leftItem.getItem()).getHudInfo(leftItem, hudMode == HudMode.ADVANCED));
                           if (info.size() > 0) {
                              for (int l = 0; l < info.size(); l++) {
                                 graphics.text(this.mc.font, info.get(l), 8, nextLine + (l + 1) * 14, -1);
                              }
                           }
                        } else {
                           info.addAll(
                              leftItem.getTooltipLines(
                                    TooltipContext.of(this.mc.level), this.mc.player, hudMode == HudMode.ADVANCED ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL
                                 )
                                 .stream()
                                 .map(Component::getString)
                                 .toList()
                           );
                           if (info.size() > 1) {
                              for (int l = 1; l < info.size(); l++) {
                                 graphics.text(this.mc.font, info.get(l), 8, nextLine + l * 14, -1);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static final int getCharge(ItemStack stack) {
      Item item = stack.getItem();

      assert item != null;

      if (item instanceof IItemHudProvider.IItemHudBarProvider) {
         return ((IItemHudProvider.IItemHudBarProvider)item).getBarPercent(stack);
      } else if (item instanceof IElectricItem) {
         return mapCharge(stack);
      } else {
         return stack.isDamageableItem() ? (int)Util.map(1.0 - (double)stack.getDamageValue() / stack.getMaxDamage(), 1.0, 100.0) : -1;
      }
   }

   private static final int mapCharge(ItemStack stack) {
      assert stack.getItem() instanceof IElectricItem;

      double charge = ElectricItem.manager.getCharge(stack);
      double maxCharge = charge + ElectricItem.manager.charge(stack, Double.POSITIVE_INFINITY, Integer.MAX_VALUE, true, true);
      return (int)Util.map(charge, maxCharge, 100.0);
   }
}
