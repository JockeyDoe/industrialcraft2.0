package ic2.core.command;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SpriteFiles {
   private SpriteFiles() {
   }

   public static Path reserve(Path directory, String namespace, String displayName, int size) throws IOException {
      Files.createDirectories(directory);
      String base = ("Sprite_" + namespace + "_" + displayName + "_" + size).replaceAll("[^\\w\\- ]+", "");
      int suffix = -1;

      while (true) {
         Path file = directory.resolve(base + (suffix < 0 ? "" : "_" + suffix) + ".png");

         try {
            return Files.createFile(file);
         } catch (FileAlreadyExistsException var8) {
            suffix++;
         }
      }
   }
}
