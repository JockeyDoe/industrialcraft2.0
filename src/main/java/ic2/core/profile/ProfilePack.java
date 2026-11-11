package ic2.core.profile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PackResources.ResourceOutput;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.InclusiveRange;

final class ProfilePack implements PackResources {
   private final TextureStyle style;
   private final PackLocationInfo location;

   ProfilePack(TextureStyle style, PackLocationInfo location) {
      this.style = style;
      this.location = location;
   }

   public PackLocationInfo location() {
      return this.location;
   }

   public IoSupplier<InputStream> getRootResource(String... parts) {
      return null;
   }

   private boolean exists(String path) {
      return this.style.target.isFile()
         ? this.style.target.asZip().getEntry(this.style.target.offset.isEmpty() ? path : this.style.target.offset + "/" + path) != null
         : Files.isRegularFile(this.style.target.asFile().toPath().resolve(path));
   }

   public IoSupplier<InputStream> getResource(PackType type, Identifier id) {
      return type == PackType.CLIENT_RESOURCES && this.style.mod.equals(id.getNamespace()) && this.exists(id.getPath())
         ? () -> this.style.target.offset(id.getPath()).asStream()
         : null;
   }

   public void listResources(PackType type, String namespace, String prefix, ResourceOutput output) {
      if (type == PackType.CLIENT_RESOURCES && this.style.mod.equals(namespace)) {
         if (this.style.target.isFile()) {
            String base = this.style.target.offset.isEmpty() ? "" : this.style.target.offset + "/";
            Enumeration<? extends ZipEntry> entries = this.style.target.asZip().entries();

            while (entries.hasMoreElements()) {
               ZipEntry entry = entries.nextElement();
               String name = entry.getName();
               if (!entry.isDirectory() && name.startsWith(base)) {
                  this.emit(name.substring(base.length()), namespace, prefix, output);
               }
            }
         } else {
            Path root = this.style.target.asFile().toPath();
            if (!Files.isDirectory(root)) {
               return;
            }

            try (Stream<Path> paths = Files.walk(root)) {
               paths.filter(x$0 -> Files.isRegularFile(x$0))
                  .forEach(path -> this.emit(root.relativize(path).toString().replace('\\', '/'), namespace, prefix, output));
            } catch (IOException var11) {
               throw new UncheckedIOException(var11);
            }
         }
      }
   }

   private void emit(String path, String namespace, String prefix, ResourceOutput output) {
      if (path.startsWith(prefix.isEmpty() ? "" : prefix + "/")) {
         Identifier id = Identifier.tryBuild(namespace, path);
         if (id != null) {
            output.accept(id, (IoSupplier)() -> this.style.target.offset(path).asStream());
         }
      }
   }

   public Set<String> getNamespaces(PackType type) {
      return type == PackType.CLIENT_RESOURCES ? Set.of(this.style.mod) : Set.of();
   }

   public <T> T getMetadataSection(MetadataSectionType<T> section) {
      if (section != PackMetadataSection.CLIENT_TYPE && section != PackMetadataSection.FALLBACK_TYPE) {
         return null;
      } else {
         PackFormat version = SharedConstants.getCurrentVersion().packVersion(PackType.CLIENT_RESOURCES);
         return (T)(new PackMetadataSection(Component.literal(this.location.id()), new InclusiveRange(version, version)));
      }
   }

   public void close() {
   }
}
