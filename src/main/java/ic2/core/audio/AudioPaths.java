package ic2.core.audio;

import java.util.Locale;
import net.minecraft.resources.Identifier;

public final class AudioPaths {
   private AudioPaths() {
   }

   public static Identifier location(String file) {
      int colon = file.indexOf(58);
      String namespace = colon < 0 ? "ic2" : file.substring(0, colon);
      String path = (colon < 0 ? file : file.substring(colon + 1)).replace('\\', '/').toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
      if (path.endsWith(".ogg")) {
         path = path.substring(0, path.length() - 4);
      }

      return Identifier.fromNamespaceAndPath(namespace, path);
   }
}
