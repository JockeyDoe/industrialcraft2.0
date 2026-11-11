package ic2.core.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import ic2.core.item.armor.jetpack.JetpackAttachmentRecipe;
import ic2.core.util.ItemStacks;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
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
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;

@EventBusSubscriber(modid = "ic2")
public final class NativeRecipePack implements PackResources {
   private static final Map<Identifier, Recipe<?>> recipes = new LinkedHashMap<>();
   private final PackLocationInfo location;
   private final Map<Identifier, byte[]> resources;

   private NativeRecipePack(PackLocationInfo location, boolean metadataOnly) {
      this.location = location;
      this.resources = metadataOnly ? Map.of() : encodeResources();
   }

   public static synchronized void add(Identifier id, Recipe<?> recipe) {
      if (recipes.putIfAbsent(id, recipe) != null) {
         throw new IllegalArgumentException("Duplicate IC2 recipe: " + id);
      } else {
         if (recipe instanceof LegacyCraftingRecipe legacy) {
            legacy.setRegistryName(id);
         }
      }
   }

   public static synchronized Map<Identifier, Recipe<?>> configuredRecipes() {
      return Map.copyOf(recipes);
   }

   public static int priority(Recipe<?> recipe) {
      if (recipe instanceof SmeltingRecipe) {
         return SmeltingRecipeManager.priority(recipe);
      } else if (recipe instanceof JetpackAttachmentRecipe) {
         return 100;
      } else if (recipe instanceof ColourCarryingRecipe) {
         return -20;
      } else if (recipe instanceof AdvRecipe) {
         return -10;
      } else {
         return recipe instanceof AdvShapelessRecipe ? -30 : -40;
      }
   }

   public static synchronized Map<Identifier, byte[]> encodeResources() {
      Map<Identifier, byte[]> data = new LinkedHashMap<>();
      RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, ItemStacks.registries());
      JsonObject priorities = new JsonObject();
      recipes.forEach((id, recipe) -> {
         JsonElement json = (JsonElement)Recipe.DIRECT_CODEC.encodeStart(ops, recipe).getOrThrow();
         data.put(Identifier.fromNamespaceAndPath(id.getNamespace(), "recipe/" + id.getPath() + ".json"), bytes(json.toString()));
         if (recipe instanceof LegacyCraftingRecipe || recipe instanceof SmeltingRecipe) {
            priorities.addProperty(id.toString(), priority((Recipe<?>)recipe));
         }
      });
      JsonObject priorityFile = new JsonObject();
      priorityFile.addProperty("replace", false);
      priorityFile.add("entries", priorities);
      data.put(Identifier.fromNamespaceAndPath("neoforge", "recipe_priorities.json"), bytes(priorityFile.toString()));
      return Collections.unmodifiableMap(data);
   }

   private static byte[] bytes(String json) {
      return json.getBytes(StandardCharsets.UTF_8);
   }

   @SubscribeEvent
   public static void addPacks(AddPackFindersEvent event) {
      if (event.getPackType() == PackType.SERVER_DATA) {
         event.addRepositorySource(
            output -> {
               PackLocationInfo location = new PackLocationInfo(
                  "ic2-configured-recipes", Component.literal("IC2 configured recipes"), PackSource.BUILT_IN, Optional.empty()
               );
               var supplier = new ResourcesSupplier() {
                  public PackMetadataResources openMetadata(PackLocationInfo info) {
                     return new NativeRecipePack(info, true);
                  }

                  public Stream<PackResources> openResources(PackLocationInfo info, Metadata metadata) {
                     return Stream.of(new NativeRecipePack(info, false));
                  }
               };
               Pack pack = Pack.readMetaAndCreate(location, supplier, PackType.SERVER_DATA, new PackSelectionConfig(true, Position.TOP, true));
               if (pack == null) {
                  throw new IllegalStateException("Unable to create IC2 recipe pack");
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
      byte[] data = type == PackType.SERVER_DATA ? this.resources.get(id) : null;
      return data == null ? null : () -> new ByteArrayInputStream(data);
   }

   public void listResources(PackType type, String namespace, String prefix, ResourceOutput output) {
      if (type == PackType.SERVER_DATA) {
         this.resources.forEach((id, data) -> {
            if (id.getNamespace().equals(namespace) && id.getPath().startsWith(prefix.isEmpty() ? "" : prefix + "/")) {
               output.accept(id, (IoSupplier)() -> new ByteArrayInputStream(data));
            }
         });
      }
   }

   public Set<String> getNamespaces(PackType type) {
      if (type != PackType.SERVER_DATA) {
         return Set.of();
      } else {
         Set<String> names = new HashSet<>();
         this.resources.keySet().forEach(id -> names.add(id.getNamespace()));
         return Set.copyOf(names);
      }
   }

   public <T> T getMetadataSection(MetadataSectionType<T> section) {
      if (section != PackMetadataSection.SERVER_TYPE && section != PackMetadataSection.FALLBACK_TYPE) {
         return null;
      } else {
         PackFormat format = SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA);
         return (T)(new PackMetadataSection(Component.literal("IC2 configured recipes"), new InclusiveRange(format, format)));
      }
   }

   public void close() {
   }
}
