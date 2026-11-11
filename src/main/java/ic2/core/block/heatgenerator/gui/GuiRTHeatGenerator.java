package ic2.core.block.heatgenerator.gui;

import com.google.common.base.Supplier;
import ic2.core.GuiIC2;
import ic2.core.block.heatgenerator.container.ContainerRTHeatGenerator;
import ic2.core.gui.Text;
import ic2.core.gui.dynamic.TextProvider;
import java.util.Objects;
import net.minecraft.resources.Identifier;

public class GuiRTHeatGenerator extends GuiIC2<ContainerRTHeatGenerator> {
   private static final Identifier background = Identifier.fromNamespaceAndPath("ic2", "textures/gui/guirtheatgenerator.png");

   public GuiRTHeatGenerator(final ContainerRTHeatGenerator container) {
      super(container);
      this.addElement(Text.create(this, 49, 66, 79, 13, TextProvider.of(new Supplier<String>() {
         {
            Objects.requireNonNull(GuiRTHeatGenerator.this);
         }

         public String get() {
            return container.base.gettransmitHeat() + " / " + container.base.getMaxHeatEmittedPerTick();
         }
      }), 5752026, false, 0, 0, true, true).withTooltip("ic2.RTHeatGenerator.gui.tooltipheat"));
   }

   @Override
   protected Identifier getTexture() {
      return background;
   }
}
