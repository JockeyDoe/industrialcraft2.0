package ic2.core.item.tool;

import ic2.core.GuiIC2;
import ic2.core.gui.Text;
import ic2.core.ref.ItemName;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiToolbox extends GuiIC2<ContainerToolbox> {
   private static final Identifier background = Identifier.fromNamespaceAndPath("ic2", "textures/gui/guitoolbox.png");

   public GuiToolbox(ContainerToolbox container) {
      super(container);
      this.addElement(Text.create(this, 65, 11, ItemName.tool_box.getItemStack().getDisplayName(), 0, false));
   }

   @Override
   protected void drawBackgroundAndTitle(float partialTicks, int mouseX, int mouseY) {
      this.bindTexture();
      this.func_73729_b(this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
   }

   @Override
   protected Identifier getTexture() {
      return background;
   }
}
