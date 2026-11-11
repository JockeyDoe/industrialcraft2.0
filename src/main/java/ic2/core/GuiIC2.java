package ic2.core;

import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.IUpgradeItem;
import ic2.api.upgrade.UpgradableProperty;
import ic2.api.upgrade.UpgradeRegistry;
import ic2.core.gui.GuiElement;
import ic2.core.gui.GuiTextures;
import ic2.core.gui.IClickHandler;
import ic2.core.gui.MouseButton;
import ic2.core.gui.ScrollDirection;
import ic2.core.init.Localization;
import ic2.core.util.InventoryNames;
import ic2.core.util.StackUtil;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public abstract class GuiIC2<T extends ContainerBase<? extends Container>> extends AbstractContainerScreen<T> {
   private GuiGraphicsExtractor graphics;
   private boolean fixKeyEvents = false;
   private boolean tick = false;
   private boolean background = false;
   private boolean mouseClick = false;
   private boolean mouseDrag = false;
   private boolean mouseRelease = false;
   private boolean mouseScroll = false;
   private boolean key = false;
   private final Queue<GuiIC2.Tooltip> queuedTooltips = new ArrayDeque<>();
   protected final T container;
   protected final List<GuiElement<?>> elements = new ArrayList<>();
   public static final int textHeight = 8;
   private long mouseClickTime;
   private boolean mouseHandled;
   private boolean keyHandled;
   private KeyEvent keyEvent;

   public final GuiGraphicsExtractor getGraphics() {
      if (this.graphics == null) {
         throw new IllegalStateException("GUI drawing outside render extraction");
      } else {
         return this.graphics;
      }
   }

   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      this.graphics = graphics;

      try {
         super.extractRenderState(graphics, mouseX, mouseY, partialTick);
      } finally {
         this.graphics = null;
         GuiTextures.clear();
      }
   }

   protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
      graphics.pose().pushMatrix();
      graphics.pose().translate(-this.leftPos, -this.topPos);

      try {
         this.func_146979_b(mouseX, mouseY);
      } finally {
         graphics.pose().popMatrix();
      }
   }

   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      GuiGraphicsExtractor previous = this.graphics;
      this.graphics = graphics;
      try {
         super.extractBackground(graphics, mouseX, mouseY, partialTick);
         this.func_146976_a(partialTick, mouseX, mouseY);
      } finally {
         this.graphics = previous;
         if (previous == null) {
            GuiTextures.clear();
         }
      }
   }

   public int getGuiLeft() {
      return this.leftPos;
   }

   public int getGuiTop() {
      return this.topPos;
   }

   public Slot getSlotUnderMouse() {
      return this.hoveredSlot;
   }

   public GuiIC2(T container) {
      this(container, 176, 166);
   }

   public GuiIC2(T container, int ySize) {
      this(container, 176, ySize);
   }

   public GuiIC2(T container, int xSize, int ySize) {
      super(container, Minecraft.getInstance().player.getInventory(), Component.literal("IC2"), xSize, ySize);
      this.container = container;
   }

   public T getContainer() {
      return this.container;
   }

   protected void init() {
      this.func_73866_w_();
   }

   public void func_73866_w_() {
      super.init();
   }

   protected void containerTick() {
      super.containerTick();
      if (this.tick) {
         for (GuiElement<?> element : this.elements) {
            if (element.isEnabled()) {
               element.tick();
            }
         }
      }
   }

   protected void func_146976_a(float partialTicks, int mouseX, int mouseY) {
      mouseX -= this.leftPos;
      mouseY -= this.topPos;
      this.drawBackgroundAndTitle(partialTicks, mouseX, mouseY);
      if (this.container.base instanceof IUpgradableBlock) {
         GuiTextures.select(Identifier.fromNamespaceAndPath("ic2", "textures/gui/infobutton.png"));
         this.drawTexturedRect(3.0, 3.0, 10.0, 10.0, 0.0, 0.0);
      }

      if (this.background) {
         for (GuiElement<?> element : this.elements) {
            if (element.isEnabled()) {
               element.drawBackground(mouseX, mouseY);
            }
         }
      }
   }

   protected void drawBackgroundAndTitle(float partialTicks, int mouseX, int mouseY) {
      this.bindTexture();
      this.func_73729_b(this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
      String name = Localization.translate(InventoryNames.get(this.container.base));
      this.drawXCenteredString(this.imageWidth / 2, 6, name, 4210752, false);
   }

   protected final void func_146979_b(int mouseX, int mouseY) {
      this.drawForegroundLayer(mouseX - this.leftPos, mouseY - this.topPos);
      this.flushTooltips();
   }

   protected void drawForegroundLayer(int mouseX, int mouseY) {
      if (this.container.base instanceof IUpgradableBlock) {
         this.handleUpgradeTooltip(mouseX, mouseY);
      }

      for (GuiElement<?> element : this.elements) {
         if (element.isEnabled()) {
            element.drawForeground(mouseX, mouseY);
         }
      }
   }

   private void handleUpgradeTooltip(int mouseX, int mouseY) {
      int areaSize = 12;
      if (mouseX >= 0 && mouseX <= 12 && mouseY >= 0 && mouseY <= 12) {
         List<String> text = new ArrayList<>();
         text.add(Localization.translate("ic2.generic.text.upgrade"));

         for (ItemStack stack : getCompatibleUpgrades((IUpgradableBlock)this.container.base)) {
            text.add(stack.getHoverName().getString());
         }

         this.drawTooltip(mouseX, mouseY, text);
      }
   }

   private static List<ItemStack> getCompatibleUpgrades(IUpgradableBlock block) {
      List<ItemStack> ret = new ArrayList<>();
      Set<UpgradableProperty> properties = block.getUpgradableProperties();

      for (ItemStack stack : UpgradeRegistry.getUpgrades()) {
         IUpgradeItem item = (IUpgradeItem)stack.getItem();
         if (item.isSuitableFor(stack, properties)) {
            ret.add(stack);
         }
      }

      return ret;
   }

   public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
      if (this.mouseScroll) {
         for (GuiElement<?> element : this.elements) {
            if (element.isEnabled() && element.contains((int)x - this.leftPos, (int)y - this.topPos)) {
               element.onMouseScroll(
                  (int)x - this.leftPos,
                  (int)y - this.topPos,
                  scrollY < 0.0 ? ScrollDirection.down : (scrollY > 0.0 ? ScrollDirection.up : ScrollDirection.stopped)
               );
               return true;
            }
         }
      }

      return super.mouseScrolled(x, y, scrollX, scrollY);
   }

   protected void func_73864_a(int mouseX, int mouseY, int buttonId) throws IOException {
      MouseButton button = MouseButton.get(buttonId);
      int x = mouseX - this.leftPos;
      int y = mouseY - this.topPos;
      if (this.mouseClick && button != null) {
         for (GuiElement<?> element : this.elements) {
            if (element.isEnabled()) {
               this.mouseHandled = this.mouseHandled | element.onMouseClick(x, y, button, element.contains(x, y));
            }
         }
      }
   }

   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      this.mouseClickTime = System.nanoTime();
      this.mouseHandled = false;

      try {
         this.func_73864_a((int)event.x(), (int)event.y(), event.button());
      } catch (IOException var4) {
         throw new UncheckedIOException(var4);
      }

      return this.mouseHandled || super.mouseClicked(event, doubleClick);
   }

   public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
      boolean handled = false;
      MouseButton button = MouseButton.get(event.button());
      int x = (int)event.x() - this.leftPos;
      int y = (int)event.y() - this.topPos;
      if (this.mouseDrag && button != null) {
         for (GuiElement<?> element : this.elements) {
            if (element.isEnabled()) {
               handled |= element.onMouseDrag(x, y, button, (System.nanoTime() - this.mouseClickTime) / 1000000L, element.contains(x, y));
            }
         }
      }

      return handled || super.mouseDragged(event, dx, dy);
   }

   public boolean mouseReleased(MouseButtonEvent event) {
      boolean handled = false;
      MouseButton button = MouseButton.get(event.button());
      int x = (int)event.x() - this.leftPos;
      int y = (int)event.y() - this.topPos;
      if (this.mouseRelease && button != null) {
         for (GuiElement<?> element : this.elements) {
            if (element.isEnabled()) {
               handled |= element.onMouseRelease(x, y, button, element.contains(x, y));
            }
         }
      }

      return handled || super.mouseReleased(event);
   }

   public KeyEvent getKeyEvent() {
      return this.keyEvent;
   }

   protected void func_73869_a(char typedChar, int keyCode) throws IOException {
      if (this.key) {
         for (GuiElement<?> element : this.elements) {
            if (element.isEnabled()) {
               this.keyHandled = this.keyHandled | element.onKeyTyped(typedChar, keyCode);
            }
         }
      }
   }

   public boolean keyPressed(KeyEvent event) {
      this.keyHandled = false;
      this.keyEvent = event;

      try {
         this.func_73869_a('\u0000', event.key());
      } catch (IOException var6) {
         throw new UncheckedIOException(var6);
      } finally {
         this.keyEvent = null;
      }

      return this.keyHandled || super.keyPressed(event);
   }

   public boolean charTyped(CharacterEvent event) {
      this.keyHandled = false;

      for (char character : event.codepointAsString().toCharArray()) {
         try {
            this.func_73869_a(character, 0);
         } catch (IOException var7) {
            throw new UncheckedIOException(var7);
         }
      }

      return this.keyHandled || super.charTyped(event);
   }

   public void drawTexturedRect(double x, double y, double width, double height, double texX, double texY) {
      this.drawTexturedRect(x, y, width, height, texX, texY, false);
   }

   public void drawTexturedRect(double x, double y, double width, double height, double texX, double texY, boolean mirrorX) {
      this.drawTexturedRect(x, y, width, height, texX / 256.0, texY / 256.0, (texX + width) / 256.0, (texY + height) / 256.0, mirrorX);
   }

   public void drawTexturedRect(double x, double y, double width, double height, double uS, double vS, double uE, double vE, boolean mirrorX) {
      if (mirrorX) {
         double swap = uS;
         uS = uE;
         uE = swap;
      }

      this.getGraphics()
         .blit(
            GuiTextures.current(),
            (int)(x + this.leftPos),
            (int)(y + this.topPos),
            (int)(x + width + this.leftPos),
            (int)(y + height + this.topPos),
            (float)uS,
            (float)uE,
            (float)vS,
            (float)vE
         );
   }

   public void func_73729_b(int x, int y, int u, int v, int width, int height) {
      this.getGraphics().blit(RenderPipelines.GUI_TEXTURED, GuiTextures.current(), x, y, u, v, width, height, 256, 256);
   }

   public void drawSprite(
      double x, double y, double width, double height, TextureAtlasSprite sprite, int color, double scale, boolean fixRight, boolean fixBottom
   ) {
      if (sprite == null) {
         sprite = this.minecraft.getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(MissingTextureAtlasSprite.getLocation());
      }

      x += this.leftPos;
      y += this.topPos;
      scale *= 16.0;
      double spriteUS = sprite.getU0();
      double spriteVS = sprite.getV0();
      double spriteWidth = sprite.getU1() - spriteUS;
      double spriteHeight = sprite.getV1() - spriteVS;
      int a = color >>> 24 & 0xFF;
      int r = color >>> 16 & 0xFF;
      int g = color >>> 8 & 0xFF;
      int b = color & 0xFF;
      double xS = x;

      while (xS < x + width) {
         double maxWidth;
         double uS;
         if (xS == x && fixRight && (maxWidth = width % scale) > 0.0) {
            uS = spriteUS + spriteWidth * (1.0 - maxWidth / scale);
         } else {
            maxWidth = scale;
            uS = spriteUS;
         }

         double xE = Math.min(xS + maxWidth, x + width);
         double uE = uS + (xE - xS) / scale * spriteWidth;
         double yS = y;

         while (yS < y + height) {
            double maxHeight;
            double vS;
            if (yS == y && fixBottom && (maxHeight = height % scale) > 0.0) {
               vS = spriteVS + spriteHeight * (1.0 - maxHeight / scale);
            } else {
               maxHeight = scale;
               vS = spriteVS;
            }

            double yE = Math.min(yS + maxHeight, y + height);
            double vE = vS + (yE - yS) / scale * spriteHeight;
            int atlasWidth = Math.round(sprite.contents().width() / (sprite.getU1() - sprite.getU0()));
            int atlasHeight = Math.round(sprite.contents().height() / (sprite.getV1() - sprite.getV0()));
            this.getGraphics()
               .blit(
                  RenderPipelines.GUI_TEXTURED,
                  sprite.atlasLocation(),
                  (int)xS,
                  (int)yS,
                  (float)(uS * atlasWidth),
                  (float)(vS * atlasHeight),
                  (int)(xE - xS),
                  (int)(yE - yS),
                  Math.round((float)((uE - uS) * atlasWidth)),
                  Math.round((float)((vE - vS) * atlasHeight)),
                  atlasWidth,
                  atlasHeight,
                  color
               );
            yS += maxHeight;
         }

         xS += maxWidth;
      }
   }

   public void drawItem(int x, int y, ItemStack stack) {
      this.getGraphics().item(stack, this.leftPos + x, this.topPos + y);
   }

   public void drawItemStack(int x, int y, ItemStack stack) {
      this.drawItem(x, y, stack);
      this.getGraphics().itemDecorations(this.getFont(), stack, this.leftPos + x, this.topPos + y);
   }

   public void drawColoredRect(int x, int y, int width, int height, int color) {
      this.getGraphics().fill(x + this.leftPos, y + this.topPos, x + width + this.leftPos, y + height + this.topPos, color);
   }

   public int drawString(int x, int y, String text, int color, boolean shadow) {
      this.getGraphics().text(this.getFont(), text, this.leftPos + x, this.topPos + y, color | 0xFF000000, shadow);
      return x + this.getFont().width(text);
   }

   public void drawXCenteredString(int x, int y, String text, int color, boolean shadow) {
      this.drawCenteredString(x, y, text, color, shadow, true, false);
   }

   public void drawXYCenteredString(int x, int y, String text, int color, boolean shadow) {
      this.drawCenteredString(x, y, text, color, shadow, true, true);
   }

   public void drawCenteredString(int x, int y, String text, int color, boolean shadow, boolean centerX, boolean centerY) {
      if (centerX) {
         x -= this.getStringWidth(text) / 2;
      }

      if (centerY) {
         y -= 4;
      }

      this.getGraphics().text(this.getFont(), text, this.leftPos + x, this.topPos + y, color | 0xFF000000, shadow);
   }

   public int getStringWidth(String text) {
      return this.getFont().width(text);
   }

   public String trimStringToWidth(String text, int width) {
      return this.getFont().plainSubstrByWidth(text, width, false);
   }

   public String trimStringToWidthReverse(String text, int width) {
      return this.getFont().plainSubstrByWidth(text, width, true);
   }

   public void drawTooltip(int x, int y, List<String> text) {
      this.queuedTooltips.add(new GuiIC2.Tooltip(text, x, y));
   }

   public void drawTooltip(int x, int y, ItemStack stack) {
      assert !StackUtil.isEmpty(stack);

      this.getGraphics().setTooltipForNextFrame(this.getFont(), stack, x + this.leftPos, y + this.topPos);
   }

   protected void flushTooltips() {
      for (GuiIC2.Tooltip tooltip : this.queuedTooltips) {
         this.getGraphics()
            .setTooltipForNextFrame(
               tooltip.text.stream().map(Component::literal).map(Component::getVisualOrderText).toList(), tooltip.x + this.leftPos, tooltip.y + this.topPos
            );
      }

      this.queuedTooltips.clear();
   }

   protected void addElement(GuiElement<?> element) {
      this.elements.add(element);
      if (!this.tick || !this.background || !this.mouseClick || !this.mouseDrag || !this.mouseRelease || !this.mouseScroll || !this.key) {
         GuiElement.Subscriptions subs = element.getSubscriptions();
         if (!this.tick) {
            this.tick = subs.tick;
         }

         if (!this.background) {
            this.background = subs.background;
         }

         if (!this.mouseClick) {
            this.mouseClick = subs.mouseClick;
         }

         if (!this.mouseDrag) {
            this.mouseDrag = subs.mouseDrag;
         }

         if (!this.mouseRelease) {
            this.mouseRelease = subs.mouseRelease;
         }

         if (!this.mouseScroll) {
            this.mouseScroll = subs.mouseScroll;
         }

         if (!this.key) {
            this.key = subs.key;
         }
      }
   }

   protected final void bindTexture() {
      GuiTextures.select(this.getTexture());
   }

   protected IClickHandler createEventSender(final int event) {
      if (this.container.base instanceof BlockEntity) {
         return new IClickHandler() {
            {
               Objects.requireNonNull(GuiIC2.this);
            }

            @Override
            public void onClick(MouseButton button) {
               IC2.network.get(false).initiateClientTileEntityEvent((BlockEntity)GuiIC2.this.container.base, event);
            }
         };
      } else {
         throw new IllegalArgumentException("not applicable for " + this.container.base);
      }
   }

   protected abstract Identifier getTexture();

   private static class Tooltip {
      final int x;
      final int y;
      final List<String> text;

      Tooltip(List<String> text, int x, int y) {
         this.text = text;
         this.x = x;
         this.y = y;
      }
   }
}
