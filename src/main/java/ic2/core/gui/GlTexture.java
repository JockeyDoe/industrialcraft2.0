package ic2.core.gui;

import com.mojang.blaze3d.platform.NativeImage;
import ic2.core.IC2;
import ic2.core.util.LogCategory;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT)
public class GlTexture implements Closeable {
   private static final Map<Identifier, GlTexture> textures = new HashMap<>();
   private final Identifier loc;
   private DynamicTexture nativeTexture;
   private Identifier nativeId;
   protected int width;
   protected int height;
   protected int canvasWidth;
   protected int canvasHeight;

   public static void init() {
   }

   @SubscribeEvent
   public static void registerReload(AddClientReloadListenersEvent event) {
      event.addListener(Identifier.fromNamespaceAndPath("ic2", "gui_textures"), new ResourceManagerReloadListener() {
         public void onResourceManagerReload(ResourceManager manager) {
            for (GlTexture texture : GlTexture.textures.values()) {
               if (texture != null) {
                  texture.close();
               }
            }

            GlTexture.textures.clear();
         }
      });
   }

   public static GlTexture get(Identifier identifier) {
      GlTexture ret = textures.get(identifier);
      return ret != null ? ret : add(identifier, new GlTexture(identifier));
   }

   public static GlTexture add(Identifier identifier, GlTexture texture) {
      try {
         texture.load(Minecraft.getInstance().getResourceManager());
      } catch (IOException var3) {
         IC2.log.warn(LogCategory.General, "Can't load texture %s", identifier);
         texture.close();
         texture = null;
      }

      textures.put(identifier, texture);
      return texture;
   }

   public GlTexture(Identifier loc) {
      this.loc = loc;
   }

   protected void load(ResourceManager manager) throws IOException {
      Resource resource = manager.getResourceOrThrow(this.loc);

      try (InputStream is = resource.open()) {
         this.load(ImageIO.read(is));
      }
   }

   protected void load(BufferedImage img) {
      this.width = img.getWidth();
      this.height = img.getHeight();
      this.canvasWidth = canvasSize(this.width);
      this.canvasHeight = canvasSize(this.height);
      NativeImage pixels = new NativeImage(this.canvasWidth, this.canvasHeight, true);
      pixels.fillRect(0, 0, this.canvasWidth, this.canvasHeight, 0);

      for (int y = 0; y < this.height; y++) {
         for (int x = 0; x < this.width; x++) {
            pixels.setPixel(x, y, img.getRGB(x, y));
         }
      }

      this.nativeTexture = new DynamicTexture(() -> this.loc.toString(), pixels);
      this.nativeId = Identifier.fromNamespaceAndPath("ic2", "dynamic/gui/" + this.loc.getNamespace() + "/" + this.loc.getPath());
      Minecraft.getInstance().getTextureManager().register(this.nativeId, this.nativeTexture);
   }

   public static int canvasSize(int size) {
      return TextureSizes.canvasSize(size);
   }

   @Override
   public void close() {
      if (this.nativeTexture != null) {
         Minecraft.getInstance().getTextureManager().release(this.nativeId);
         this.nativeTexture = null;
      }
   }

   public void bind() {
      if (this.nativeTexture == null) {
         throw new IllegalStateException("uninitialized texture");
      } else {
         GuiTextures.select(this.nativeId);
      }
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public int getCanvasWidth() {
      return this.canvasWidth;
   }

   public int getCanvasHeight() {
      return this.canvasHeight;
   }
}
