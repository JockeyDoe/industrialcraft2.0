package ic2.core.block.machine.gui;

import ic2.core.block.machine.container.ContainerWeightedItemDistributor;
import net.minecraft.resources.Identifier;

public class GuiWeightedItemDistributor extends GuiWeightedDistributor<ContainerWeightedItemDistributor> {
   private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("ic2", "textures/gui/guiweighteditemdistributor.png");

   public GuiWeightedItemDistributor(ContainerWeightedItemDistributor container) {
      super(container, 211);
   }

   @Override
   protected Identifier getTexture() {
      return TEXTURE;
   }
}
