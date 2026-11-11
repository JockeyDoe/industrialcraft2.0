package ic2.core.block.personal;

import ic2.core.GuiIC2;
import ic2.core.IC2;
import ic2.core.init.Localization;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiTradeOMatOpen extends GuiIC2<ContainerTradeOMatOpen> {
   private static final Identifier background = Identifier.fromNamespaceAndPath("ic2", "textures/gui/guitradeomatopen.png");
   private final boolean isAdmin;

   public GuiTradeOMatOpen(ContainerTradeOMatOpen container, boolean isAdmin) {
      super(container);
      this.isAdmin = isAdmin;
   }

   @Override
   public void func_73866_w_() {
      super.func_73866_w_();
      if (this.isAdmin) {
         this.addRenderableWidget(
            Button.builder(Component.literal("∞"), button -> IC2.network.get(false).initiateClientTileEntityEvent(this.container.base, 0))
               .bounds((this.width - this.imageWidth) / 2 + 152, (this.height - this.imageHeight) / 2 + 4, 20, 20)
               .build()
         );
      }
   }

   @Override
   protected void drawForegroundLayer(int mouseX, int mouseY) {
      super.drawForegroundLayer(mouseX, mouseY);
      this.getGraphics().text(this.getFont(), Localization.translate("container.inventory"), 8, this.imageHeight - 96 + 2, -12566464, false);
      this.getGraphics().text(this.getFont(), Localization.translate("ic2.container.personalTrader.want"), 12, 23, -12566464, false);
      this.getGraphics().text(this.getFont(), Localization.translate("ic2.container.personalTrader.offer"), 12, 57, -12566464, false);
      this.getGraphics().text(this.getFont(), Localization.translate("ic2.container.personalTrader.totalTrades0"), 108, 28, -12566464, false);
      this.getGraphics().text(this.getFont(), Localization.translate("ic2.container.personalTrader.totalTrades1"), 108, 36, -12566464, false);
      this.getGraphics().text(this.getFont(), this.container.base.totalTradeCount + "", 112, 44, -12566464, false);
      this.getGraphics()
         .text(
            this.getFont(),
            Localization.translate("ic2.container.personalTrader.stock") + " " + (this.container.base.stock < 0 ? "Ã¢Ë†Å¾" : this.container.base.stock + ""),
            108,
            60,
            -12566464,
            false
         );
   }

   @Override
   protected Identifier getTexture() {
      return background;
   }
}
