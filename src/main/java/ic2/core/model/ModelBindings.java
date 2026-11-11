package ic2.core.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

public final class ModelBindings {
   private ModelBindings() {
   }

   public static Variant select(JsonObject definition, String variant) {
      HashMap<String, String> properties = new HashMap<>();

      for (String pair : variant.split(",")) {
         int split = pair.indexOf(61);
         if (split > 0) {
            properties.put(pair.substring(0, split), pair.substring(split + 1));
         }
      }

      if (!definition.has("variants")) {
         return null;
      } else {
         for (Entry<String, JsonElement> entry : definition.getAsJsonObject("variants").entrySet()) {
            boolean matches = true;

            for (String condition : entry.getKey().split(",")) {
               int split = condition.indexOf(61);
               if (split < 0) {
                  if (!condition.isEmpty()) {
                     matches = false;
                  }
               } else {
                  String value = properties.get(condition.substring(0, split));
                  if (value == null || !Arrays.asList(condition.substring(split + 1).split("\\|")).contains(value)) {
                     matches = false;
                  }
               }
            }

            if (matches) {
               JsonElement value = entry.getValue();
               if (value.isJsonArray()) {
                  value = value.getAsJsonArray().get(0);
               }

               return (Variant)Variant.CODEC.parse(JsonOps.INSTANCE, value).getOrThrow();
            }
         }

         return null;
      }
   }

   public static Variant resolve(ModelKey key) {
      String path = key.getPath();
      if (!path.startsWith("item/") && !path.startsWith("block/") && !path.startsWith("models/")) {
         Identifier id = Identifier.fromNamespaceAndPath(key.getNamespace(), "blockstates/" + path + ".json");
         Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(id);
         if (resource.isPresent()) {
            try (BufferedReader reader = resource.get().openAsReader()) {
               Variant selected = select(JsonParser.parseReader(reader).getAsJsonObject(), key.getVariant());
               if (selected != null) {
                  return selected;
               }

               return new Variant(
                  Identifier.fromNamespaceAndPath(
                     key.getNamespace(),
                     path.startsWith("models/") ? path.substring(7) : (!path.startsWith("item/") && !path.startsWith("block/") ? "item/" + path : path)
                  )
               );
            } catch (IOException var9) {
               throw new IllegalStateException("Reading IC2 model definition " + id, var9);
            }
         }
      }

      return new Variant(
         Identifier.fromNamespaceAndPath(
            key.getNamespace(),
            path.startsWith("models/") ? path.substring(7) : (!path.startsWith("item/") && !path.startsWith("block/") ? "item/" + path : path)
         )
      );
   }
}
