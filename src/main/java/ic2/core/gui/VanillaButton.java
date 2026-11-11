package ic2.core.gui;

import ic2.core.GuiIC2;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class VanillaButton extends Button<VanillaButton> {
   protected IEnableHandler disableHandler;

   public VanillaButton(GuiIC2<?> gui, int x, int y, int width, int height, IClickHandler handler) {
      super(gui, x, y, width, height, handler);
   }

   public VanillaButton withDisableHandler(IEnableHandler handler) {
      this.disableHandler = handler;
      return this;
   }

   public boolean isDisabled() {
      return this.disableHandler != null && !this.disableHandler.isEnabled();
   }

   @Override
   public void drawBackground(int mouseX, int mouseY) {
      Identifier sprite = Identifier.withDefaultNamespace(
         this.isDisabled() ? "widget/button_disabled" : (this.isActive(mouseX, mouseY) ? "widget/button_highlighted" : "widget/button")
      );
      this.gui
         .getGraphics()
         .blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.gui.getLeftPos() + this.x, this.gui.getTopPos() + this.y, this.width, this.height);
      super.drawBackground(mouseX, mouseY);
   }

   protected boolean isActive(int mouseX, int mouseY) {
      return this.contains(mouseX, mouseY);
   }

   @Override
   protected int getTextColor(int mouseX, int mouseY) {
      return this.isDisabled() ? 10526880 : (this.isActive(mouseX, mouseY) ? 16777120 : 14737632);
   }

   @Override
   protected boolean onMouseClick(int mouseX, int mouseY, MouseButton button) {
      return this.isDisabled() ? false : super.onMouseClick(mouseX, mouseY, button);
   }
}
