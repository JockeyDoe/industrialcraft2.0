package ic2.core.profile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforgespi.locating.IModFile;
import org.apache.commons.lang3.StringUtils;

public class ProfileTarget {
   public final File root;
   public final String offset;
   private static final Map<File, Map<String, ZipFile>> ZIP_HOLDER = new HashMap<>();

   public static ProfileTarget fromJar(String offset) {
      ModContainer container = ModLoadingContext.get().getActiveContainer();
      if (container != null && "ic2".equals(container.getModId())) {
         IModFile file = container.getModInfo().getOwningFile().getFile();

         for (Path path : file.getContents().getContentRoots()) {
            if (Files.isDirectory(path) && Files.exists(path.resolve(offset))) {
               return new ProfileTarget(path.toFile(), offset);
            }
         }

         return new ProfileTarget(file.getFilePath().toFile(), offset);
      } else {
         throw new IllegalStateException("IC2 profile read outside its mod container");
      }
   }

   public ProfileTarget(File root, String offset) {
      this.root = root;
      this.offset = offset;
   }

   public boolean isFile() {
      return !this.root.isDirectory();
   }

   public File asFile() {
      return new File(this.root, this.offset);
   }

   public ZipFile asZip() {
      Map<String, ZipFile> offsetMap = ZIP_HOLDER.computeIfAbsent(this.root, k -> new HashMap<>());
      ZipFile ret = offsetMap.get(this.offset);
      if (ret == null) {
         try {
            offsetMap.put(this.offset, ret = this.makeZip());
         } catch (IOException var4) {
            throw new RuntimeException("Failed to get zip!", var4);
         }
      }

      return ret;
   }

   protected ZipFile makeZip() throws IOException {
      return new ZipFile(this.root) {
         {
            Objects.requireNonNull(ProfileTarget.this);
         }

         @Override
         public ZipEntry getEntry(String name) {
            return super.getEntry(StringUtils.isNotBlank(name) ? ProfileTarget.this.offset + "/" + name : ProfileTarget.this.offset);
         }
      };
   }

   public InputStream asStream() throws IOException {
      if (this.isFile()) {
         ZipFile zip = this.asZip();
         ZipEntry entry = zip.getEntry(null);
         return entry == null ? null : zip.getInputStream(entry);
      } else {
         File file = this.asFile();
         return file.canRead() && file.isFile() ? new BufferedInputStream(new FileInputStream(file)) : null;
      }
   }

   public ProfileTarget offset(String extra) {
      return new ProfileTarget(this.root, this.offset + "/" + extra);
   }

   @Override
   public String toString() {
      return "ProfileTarget<" + this.root + " -> " + this.offset + ">";
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else {
         return !(obj instanceof ProfileTarget other) ? false : Objects.equals(this.root, other.root) && Objects.equals(this.offset, other.offset);
      }
   }
}
