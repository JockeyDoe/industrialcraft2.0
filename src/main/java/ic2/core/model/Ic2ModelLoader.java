package ic2.core.model;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

public class Ic2ModelLoader {
   private static final Map<Identifier, IReloadableModel> models = new HashMap<>();

   public void register(String path, IReloadableModel model) {
      this.register(Identifier.fromNamespaceAndPath("ic2", path), model);
   }

   public void register(Identifier location, IReloadableModel model) {
      models.put(location, model);
   }

   public void func_110549_a(ResourceManager resourceManager) {
      for (IReloadableModel model : models.values()) {
         model.onReload();
      }

      ModelComparator.onReload();
   }

   public Map<Identifier, IReloadableModel> models() {
      return Map.copyOf(models);
   }

   public boolean accepts(Identifier modelLocation) {
      return models.containsKey(modelLocation);
   }

   public IReloadableModel loadModel(Identifier modelLocation) throws IOException {
      return models.get(modelLocation);
   }
}
