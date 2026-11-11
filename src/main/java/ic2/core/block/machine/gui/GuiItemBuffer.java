package ic2.core.block.machine.gui;

import ic2.core.GuiIC2;
import ic2.core.block.machine.container.ContainerItemBuffer;
import net.minecraft.resources.Identifier;

public class GuiItemBuffer extends GuiIC2<ContainerItemBuffer> {
   public GuiItemBuffer(ContainerItemBuffer container) {
      super(container, 232);
   }

   @Override
   public Identifier getTexture() {
      return Identifier.fromNamespaceAndPath("ic2", "textures/gui/guiitembuffer.png");
   }
}
