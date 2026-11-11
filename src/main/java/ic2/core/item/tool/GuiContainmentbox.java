package ic2.core.item.tool;

import ic2.core.GuiIC2;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiContainmentbox extends GuiIC2<ContainerContainmentbox> {
   public GuiContainmentbox(ContainerContainmentbox container) {
      super(container);
   }

   @Override
   public Identifier getTexture() {
      return Identifier.fromNamespaceAndPath("ic2", "textures/gui/guicontainmentbox.png");
   }
}
