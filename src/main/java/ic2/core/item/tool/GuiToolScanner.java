package ic2.core.item.tool;

import ic2.core.GuiIC2;
import ic2.core.init.Localization;
import ic2.core.util.Tuple;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiToolScanner extends GuiIC2<ContainerToolScanner> {
   public GuiToolScanner(ContainerToolScanner container) {
      super(container, 230);
   }

   @Override
   protected void drawForegroundLayer(int mouseX, int mouseY) {
      super.drawForegroundLayer(mouseX, mouseY);
      this.getGraphics().text(this.getFont(), Localization.translate("ic2.itemScanner.found"), 10, 20, -14619842, false);
      if (this.container.scanResults != null) {
         int count = 0;

         for (Tuple.T2<ItemStack, Integer> result : this.container.scanResults) {
            String name = result.a.getHoverName().getString();
            this.getGraphics().text(this.getFont(), result.b + "x " + name, 10, 34 + count * 11, -11025190, false);
            if (++count == 10) {
               break;
            }
         }
      }
   }

   @Override
   protected void func_146976_a(float f, int x, int y) {
      super.func_146976_a(f, x, y);
      if (this.container.scanResults != null) {
         int count = 0;

         for (Tuple.T2<ItemStack, Integer> result : this.container.scanResults) {
            int xPos = 135 + (count & 1) * 15;
            this.drawItem(xPos, 11 * count + 28, result.a);
            if (++count == 10) {
               break;
            }
         }
      }
   }

   @Override
   public Identifier getTexture() {
      return Identifier.fromNamespaceAndPath("ic2", "textures/gui/guitoolscanner.png");
   }
}
