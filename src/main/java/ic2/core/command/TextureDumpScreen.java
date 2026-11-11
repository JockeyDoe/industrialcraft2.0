package ic2.core.command;

import com.mojang.blaze3d.platform.NativeImage;
import ic2.core.IC2;
import ic2.core.ref.IMultiItem;
import ic2.core.util.ItemStacks;
import ic2.core.util.LogCategory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent.Post;
import net.neoforged.neoforge.common.NeoForge;

public final class TextureDumpScreen extends Screen {
   private final List<ItemStack> stacks = new ArrayList<>();
   private final int size;
   private final Screen previous;
   private int index;
   private int readyFrames;
   private boolean capturePending;
   private boolean closed;

   private TextureDumpScreen(Pattern pattern, int size, Integer meta, Screen previous) {
      super(Component.literal("IC2 texture dump"));
      this.size = size;
      this.previous = previous;
      if (size > 0 && size <= 16384) {
         for (Item item : BuiltInRegistries.ITEM) {
            if (pattern.matcher(BuiltInRegistries.ITEM.getKey(item).toString()).matches()) {
               if (meta != null) {
                  this.stacks.add(ItemStacks.create(item, 1, meta));
               } else if (item instanceof IMultiItem<?> multi) {
                  this.stacks.addAll(multi.getAllStacks());
               } else {
                  this.stacks.add(new ItemStack(item));
               }
            }
         }

         this.stacks.removeIf(ItemStack::isEmpty);
      } else {
         throw new IllegalArgumentException("Sprite size must be between 1 and 16384");
      }
   }

   public static void open(Pattern pattern, int size, Integer meta) {
      Minecraft client = Minecraft.getInstance();
      client.execute(() -> {
         TextureDumpScreen screen = new TextureDumpScreen(pattern, size, meta, client.gui.screen());
         if (screen.stacks.isEmpty()) {
            IC2.log.info(LogCategory.General, "No items matched the texture dump.");
         } else {
            client.gui.setScreen(screen);
            NeoForge.EVENT_BUS.register(screen);
         }
      });
   }

   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      graphics.fill(0, 0, this.width, this.height, -16777216);
      if (this.index < this.stacks.size()) {
         double scale = this.minecraft.getWindow().getGuiScale();
         int available = Math.min(this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
         float itemScale = (float)(Math.min(this.size, available) / (16.0 * scale));
         graphics.pose().pushMatrix();
         graphics.pose().scale(itemScale);
         graphics.item(this.stacks.get(this.index), 0, 0);
         graphics.pose().popMatrix();
         this.readyFrames++;
      }
   }

   @SubscribeEvent
   public void frame(Post event) {
      if (!this.closed && this.minecraft.gui.screen() == this && !this.capturePending && this.readyFrames >= 2) {
         this.capturePending = true;
         ItemStack stack = this.stacks.get(this.index).copy();
         Screenshot.takeScreenshot(
            this.minecraft.gameRenderer.mainRenderTarget(),
            image -> {
               try {
                  NativeImage twrVar0$ = image;

                  try {
                     NativeImage sprite = new NativeImage(this.size, this.size, false);

                     try {
                        int captured = Math.min(this.size, Math.min(image.getWidth(), image.getHeight()));
                        image.resizeSubRectTo(0, 0, captured, captured, sprite);
                        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                        Path file = SpriteFiles.reserve(
                           IC2.platform.getMinecraftDir().toPath().resolve("sprites"), id.getNamespace(), stack.getHoverName().getString(), this.size
                        );
                        sprite.writeToFile(file);
                        this.minecraft.execute(() -> {
                           if (!this.closed) {
                              this.index++;
                              this.readyFrames = 0;
                              this.capturePending = false;
                              if (this.index == this.stacks.size()) {
                                 IC2.log.info(LogCategory.General, "Dumped %d sprites.", this.index);
                                 this.minecraft.gui.setScreen(this.previous);
                              }
                           }
                        });
                     } catch (Throwable var10) {
                        try {
                           sprite.close();
                        } catch (Throwable var9) {
                           var10.addSuppressed(var9);
                        }

                        throw var10;
                     }

                     sprite.close();
                  } catch (Throwable var11) {
                     if (image != null) {
                        try {
                           twrVar0$.close();
                        } catch (Throwable var8) {
                           var11.addSuppressed(var8);
                        }
                     }

                     throw var11;
                  }

                  if (image != null) {
                     image.close();
                  }
               } catch (Exception var12) {
                  IC2.log.warn(LogCategory.General, var12, "Texture dump failed.");
                  this.minecraft.execute(() -> {
                     if (!this.closed) {
                        this.minecraft.gui.setScreen(this.previous);
                     }
                  });
               }
            }
         );
      }
   }

   public void removed() {
      this.closed = true;
      NeoForge.EVENT_BUS.unregister(this);
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void onClose() {
      this.minecraft.gui.setScreen(this.previous);
   }
}
