package ic2.core.block.wiring;

import ic2.core.GuiIC2;
import ic2.core.IC2;
import ic2.core.init.Localization;
import ic2.core.ref.ItemName;
import java.io.IOException;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiTransformer extends GuiIC2<ContainerTransformer> {
   public String[] mode = new String[]{"", "", "", ""};
   private static final Identifier background = Identifier.fromNamespaceAndPath("ic2", "textures/gui/guitransfomer.png");

   public GuiTransformer(ContainerTransformer container) {
      super(container, 219);
      this.mode[1] = Localization.translate("ic2.Transformer.gui.switch.mode1");
      this.mode[2] = Localization.translate("ic2.Transformer.gui.switch.mode2");
      this.mode[3] = Localization.translate("ic2.Transformer.gui.switch.mode3");
   }

   @Override
   protected void func_73864_a(int i, int j, int k) throws IOException {
      super.func_73864_a(i, j, k);
      int x = i - this.leftPos;
      int y = j - this.topPos;
      if (x >= 150 && y >= 32 && x <= 167 && y <= 49) {
         IC2.network.get(false).initiateClientTileEntityEvent(this.container.base, 3);
      }
   }

   @Override
   protected void drawForegroundLayer(int mouseX, int mouseY) {
      super.drawForegroundLayer(mouseX, mouseY);
      this.getGraphics().text(this.getFont(), Localization.translate("ic2.Transformer.gui.Output"), 6, 30, -12566464, false);
      this.getGraphics().text(this.getFont(), Localization.translate("ic2.Transformer.gui.Input"), 6, 43, -12566464, false);
      this.getGraphics()
         .text(this.getFont(), this.container.base.getoutputflow() + " " + Localization.translate("ic2.generic.text.EUt"), 52, 30, -14619842, false);
      this.getGraphics()
         .text(this.getFont(), this.container.base.getinputflow() + " " + Localization.translate("ic2.generic.text.EUt"), 52, 45, -14619842, false);
      switch (this.container.base.getMode()) {
         case redstone:
            this.getGraphics().item(ItemName.wrench.getItemStack(), 152, 67);
            break;
         case stepdown:
            this.getGraphics().item(ItemName.wrench.getItemStack(), 152, 87);
            break;
         case stepup:
            this.getGraphics().item(ItemName.wrench.getItemStack(), 152, 107);
      }
   }

   @Override
   public void func_73866_w_() {
      super.func_73866_w_();

      for (int i = 0; i < 3; i++) {
         int eventId = i;
         this.addRenderableWidget(
            Button.builder(Component.literal(this.mode[i + 1]), button -> IC2.network.get(false).initiateClientTileEntityEvent(this.container.base, eventId))
               .bounds(this.leftPos + 7, this.topPos + 65 + i * 20, 144, 20)
               .build()
         );
      }
   }

   @Override
   protected Identifier getTexture() {
      return background;
   }
}
