package ic2.core.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ic2.core.block.BlockTileEntity;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackMetadataResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PackResources.ResourceOutput;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.Pack.Metadata;
import net.minecraft.server.packs.repository.Pack.Position;
import net.minecraft.server.packs.repository.Pack.ResourcesSupplier;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.InclusiveRange;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;

@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT)
public final class NativeModelPack implements PackResources {
   private final PackLocationInfo location;
   private final Map<Identifier, byte[]> resources;

   private NativeModelPack(PackLocationInfo location, boolean metadataOnly) {
      this.location = location;
      this.resources = metadataOnly ? Map.of() : encodeResources();
   }

   static Identifier defaultModel(ModelKey key) {
      String path = key.getPath();

      try {
         Identifier var5;
         try (InputStream stream = NativeModelPack.class.getResourceAsStream("/assets/" + key.getNamespace() + "/blockstates/" + path + ".json")) {
            if (stream == null) {
               return Identifier.fromNamespaceAndPath(
                  key.getNamespace(),
                  path.startsWith("models/") ? path.substring(7) : (!path.startsWith("item/") && !path.startsWith("block/") ? "item/" + path : path)
               );
            }

            JsonObject definition = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Variant selected = ModelBindings.select(definition, key.getVariant());
            if (selected != null) {
               return selected.modelLocation();
            }

            if (!definition.has("multipart")) {
               return Identifier.fromNamespaceAndPath(
                  key.getNamespace(),
                  path.startsWith("models/") ? path.substring(7) : (!path.startsWith("item/") && !path.startsWith("block/") ? "item/" + path : path)
               );
            }

            var5 = Identifier.parse(definition.getAsJsonArray("multipart").get(0).getAsJsonObject().getAsJsonObject("apply").get("model").getAsString());
         }

         return var5;
      } catch (IOException var8) {
         throw new IllegalStateException("IC2 item definition " + key, var8);
      }
   }

   public static Map<Identifier, byte[]> encodeResources() {
      LinkedHashMap<Identifier, byte[]> result = new LinkedHashMap<>();
      Map<Item, ModelKey> defaults = ModelRegistry.defaultItemModels();

      for (Item item : BuiltInRegistries.ITEM) {
         Identifier id = BuiltInRegistries.ITEM.getKey(item);
         if (id.getNamespace().equals("ic2")) {
            ModelKey key = defaults.get(item);
            Identifier model = key == null ? Identifier.fromNamespaceAndPath("ic2", "item/" + id.getPath()) : defaultModel(key);
            JsonObject nativeModel = new JsonObject();
            nativeModel.addProperty("type", "minecraft:model");
            nativeModel.addProperty("model", model.toString());
            JsonObject definition = new JsonObject();
            definition.add("model", nativeModel);
            result.put(Identifier.fromNamespaceAndPath("ic2", "items/" + id.getPath() + ".json"), definition.toString().getBytes(StandardCharsets.UTF_8));
         }
      }

      for (Block block : BuiltInRegistries.BLOCK) {
         Identifier id = BuiltInRegistries.BLOCK.getKey(block);
         if (id.getNamespace().equals("ic2") && block instanceof BlockTileEntity) {
            try (InputStream stream = NativeModelPack.class.getResourceAsStream("/assets/ic2/blockstates/" + id.getPath() + ".json")) {
               if (stream == null) {
                  throw new IllegalStateException("Missing IC2 blockstate definition " + id);
               }

               JsonObject definition = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
               JsonObject variants = definition.getAsJsonObject("variants");
               variants.entrySet().removeIf(entry -> !validCondition(block, (String)entry.getKey()));
               result.put(
                  Identifier.fromNamespaceAndPath("ic2", "blockstates/" + id.getPath() + ".json"), definition.toString().getBytes(StandardCharsets.UTF_8)
               );
            } catch (IOException var11) {
               throw new IllegalStateException("IC2 blockstate definition " + id, var11);
            }
         }
      }

      return Map.copyOf(result);
   }

   public static boolean validCondition(Block block, String condition) {
      for (String pair : condition.split(",")) {
         if (!pair.isEmpty()) {
            String[] parts = pair.split("=", 2);
            if (parts.length != 2) {
               return false;
            }

            Property<? extends Comparable<?>> property = block.getStateDefinition().getProperty(parts[0]);
            if (property == null) {
               return false;
            }

            for (String value : parts[1].split("\\|")) {
               if (property.getValue(value).isEmpty()) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   @SubscribeEvent
   public static void addPacks(AddPackFindersEvent event) {
      if (event.getPackType() == PackType.CLIENT_RESOURCES) {
         event.addRepositorySource(
            output -> {
               PackLocationInfo location = new PackLocationInfo(
                  "ic2-native-item-models", Component.literal("IC2 native item models"), PackSource.BUILT_IN, Optional.empty()
               );
               var supplier = new ResourcesSupplier() {
                  public PackMetadataResources openMetadata(PackLocationInfo info) {
                     return new NativeModelPack(info, true);
                  }

                  public Stream<PackResources> openResources(PackLocationInfo info, Metadata metadata) {
                     return Stream.of(new NativeModelPack(info, false));
                  }
               };
               Pack pack = Pack.readMetaAndCreate(location, supplier, PackType.CLIENT_RESOURCES, new PackSelectionConfig(true, Position.TOP, true));
               if (pack == null) {
                  throw new IllegalStateException("Unable to create IC2 native item model pack");
               } else {
                  output.accept(pack);
               }
            }
         );
      }
   }

   public PackLocationInfo location() {
      return this.location;
   }

   public IoSupplier<InputStream> getRootResource(String... paths) {
      return null;
   }

   public IoSupplier<InputStream> getResource(PackType type, Identifier id) {
      byte[] data = type == PackType.CLIENT_RESOURCES ? this.resources.get(id) : null;
      return data == null ? null : () -> new ByteArrayInputStream(data);
   }

   public void listResources(PackType type, String namespace, String prefix, ResourceOutput output) {
      if (type == PackType.CLIENT_RESOURCES) {
         this.resources.forEach((id, data) -> {
            if (id.getNamespace().equals(namespace) && id.getPath().startsWith(prefix.isEmpty() ? "" : prefix + "/")) {
               output.accept(id, (IoSupplier)() -> new ByteArrayInputStream(data));
            }
         });
      }
   }

   public Set<String> getNamespaces(PackType type) {
      return type == PackType.CLIENT_RESOURCES ? Set.of("ic2") : Set.of();
   }

   public <T> T getMetadataSection(MetadataSectionType<T> section) {
      if (section != PackMetadataSection.CLIENT_TYPE && section != PackMetadataSection.FALLBACK_TYPE) {
         return null;
      } else {
         PackFormat format = SharedConstants.getCurrentVersion().packVersion(PackType.CLIENT_RESOURCES);
         return (T)(new PackMetadataSection(Component.literal("IC2 native item models"), new InclusiveRange(format, format)));
      }
   }

   public void close() {
   }
}
