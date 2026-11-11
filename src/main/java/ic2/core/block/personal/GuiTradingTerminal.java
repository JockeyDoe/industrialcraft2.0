package ic2.core.block.personal;

import com.google.common.base.Supplier;
import ic2.core.GuiIC2;
import ic2.core.gui.ItemImage;
import ic2.core.gui.MouseButton;
import ic2.core.gui.ScrollableList;
import ic2.core.init.Localization;
import ic2.core.ref.ItemName;
import java.util.Objects;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public class GuiTradingTerminal extends GuiIC2<ContainerTradingTerminal> {
   private final ScrollableList list;
   private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("ic2", "textures/gui/gui_trading_terminal.png");

   public GuiTradingTerminal(ContainerTradingTerminal container) {
      super(container, 176, 227);
      this.addElement(this.list = new ScrollableList(this, 4, 20, 168, 99));
      this.addElement((new ItemImage(this, 156, 4, () -> ItemName.wrench.getItemStack()) {
         private int count;

         {
            Objects.requireNonNull(GuiTradingTerminal.this);
            this.count = 1;
         }

         @Override
         protected boolean onMouseClick(int mouseX, int mouseY, MouseButton button) {
            switch (button) {
               case left:
                  GuiTradingTerminal.this.list.addItem(new ScrollableList.IListItem() {
                     private final int item;

                     {
                        Objects.requireNonNull(<VAR_NAMELESS_ENCLOSURE>);
                        this.item = count++;
                     }

                     @Override
                     public void onClick(MouseButton button) {
                        System.out.println(this.item + " clicked with " + button);
                     }

                     public String getSerializedName() {
                        return "Trader " + this.item;
                     }
                  });
                  break;
               case right:
                  if (this.count > 1) {
                     GuiTradingTerminal.this.list.removeItem(this.count-- - 2);
                  }
            }

            return true;
         }
      }).withTooltip("Settings"));
   }

   @Override
   protected void drawBackgroundAndTitle(float partialTicks, int mouseX, int mouseY) {
      this.bindTexture();
      this.func_73729_b(this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
      String name = Localization.translate(this.container.base.getName());
      this.drawXCenteredString(this.imageWidth / 2, 8, name, 4210752, false);
   }

   @Override
   protected Identifier getTexture() {
      return TEXTURE;
   }
}
