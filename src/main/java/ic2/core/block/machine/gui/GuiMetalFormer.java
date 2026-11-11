package ic2.core.block.machine.gui;

import com.google.common.base.Supplier;
import ic2.core.GuiIC2;
import ic2.core.block.machine.container.ContainerMetalFormer;
import ic2.core.block.wiring.CableType;
import ic2.core.gui.CustomGauge;
import ic2.core.gui.EnergyGauge;
import ic2.core.gui.Gauge;
import ic2.core.gui.IEnableHandler;
import ic2.core.gui.RecipeButton;
import ic2.core.gui.VanillaButton;
import ic2.core.init.Localization;
import ic2.core.ref.ItemName;
import java.util.Objects;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiMetalFormer extends GuiIC2<ContainerMetalFormer> {
   public GuiMetalFormer(final ContainerMetalFormer container) {
      super(container);
      this.addElement(EnergyGauge.asBolt(this, 20, 37, container.base));
      this.addElement(CustomGauge.create(this, 52, 39, new CustomGauge.IGaugeRatioProvider() {
         {
            Objects.requireNonNull(GuiMetalFormer.this);
         }

         @Override
         public double getRatio() {
            return container.base.getProgress();
         }
      }, Gauge.GaugeStyle.ProgressMetalFormer));
      this.addElement(new VanillaButton(this, 65, 53, 20, 20, this.createEventSender(0)).withIcon(new Supplier<ItemStack>() {
         {
            Objects.requireNonNull(GuiMetalFormer.this);
         }

         public ItemStack get() {
            switch (container.base.getMode()) {
               case 0:
                  return ItemName.cable.getItemStack(CableType.copper);
               case 1:
                  return ItemName.forge_hammer.getItemStack();
               case 2:
                  return ItemName.cutter.getItemStack();
               default:
                  return null;
            }
         }
      }).withTooltip(new Supplier<String>() {
         {
            Objects.requireNonNull(GuiMetalFormer.this);
         }

         public String get() {
            switch (container.base.getMode()) {
               case 0:
                  return Localization.translate("ic2.MetalFormer.gui.switch.Extruding");
               case 1:
                  return Localization.translate("ic2.MetalFormer.gui.switch.Rolling");
               case 2:
                  return Localization.translate("ic2.MetalFormer.gui.switch.Cutting");
               default:
                  return null;
            }
         }
      }));
      if (RecipeButton.canUse()) {
         for (final int i = 0; i < 3; i++) {
            this.addElement(new RecipeButton(this, 52, 39, 46, 9, new String[]{"metal_former" + i}).withEnableHandler(new IEnableHandler() {
               {
                  Objects.requireNonNull(GuiMetalFormer.this);
               }

               @Override
               public boolean isEnabled() {
                  return container.base.getMode() == i;
               }
            }));
         }
      }
   }

   @Override
   protected Identifier getTexture() {
      return Identifier.fromNamespaceAndPath("ic2", "textures/gui/guimetalformer.png");
   }
}
