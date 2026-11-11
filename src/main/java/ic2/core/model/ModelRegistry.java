package ic2.core.model;

import com.mojang.renderpearl.api.vertex.VertexFormat;
import ic2.core.util.ItemStacks;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ModelBakery.BakingResult;
import net.minecraft.client.resources.model.ResolvableModel.Resolver;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent.ModifyBakingResult;
import net.neoforged.neoforge.client.event.ModelEvent.RegisterStandalone;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import net.neoforged.neoforge.client.model.standalone.UnbakedStandaloneModel;
import org.joml.Matrix4f;
import org.slf4j.LoggerFactory;

@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT)
public final class ModelRegistry {
   private static final Map<Item, ModelRegistry.ItemMeshDefinition> meshes = new IdentityHashMap<>();
   private static final Map<Item, List<ModelKey>> itemVariants = new IdentityHashMap<>();
   private static final Map<Item, Map<Integer, ModelKey>> metadata = new IdentityHashMap<>();
   private static final Map<Block, ModelRegistry.IStateMapper> states = new IdentityHashMap<>();
   private static final Map<ModelKey, StandaloneModelKey<Ic2Geometry>> keys = new LinkedHashMap<>();
   private static final Map<Identifier, IReloadableModel> procedural = new LinkedHashMap<>();
   private static Map<ModelKey, Ic2Geometry> baked = Map.of();
   private static final ThreadLocal<ModelBaker> currentBaker = new ThreadLocal<>();

   private ModelRegistry() {
   }

   public static TextureAtlasSprite missingSprite() {
      Identifier id = MissingTextureAtlasSprite.getLocation();
      ModelBaker baker = currentBaker.get();
      return baker != null
         ? baker.materials().get(new Material(id), () -> "IC2 procedural geometry").sprite()
         : Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(id);
   }

   public static void registerLoader(Ic2ModelLoader loader) {
      procedural.putAll(loader.models());
   }

   private static void require(ModelKey key) {
      keys.computeIfAbsent(key, k -> new StandaloneModelKey(() -> k.toString()));
   }

   public static void setCustomMeshDefinition(Item item, ModelRegistry.ItemMeshDefinition mesh) {
      meshes.put(item, mesh);
   }

   public static void setCustomModelResourceLocation(Item item, int meta, ModelKey key) {
      metadata.computeIfAbsent(item, k -> new HashMap<>()).put(meta, key);
      require(key);
   }

   public static void registerItemVariants(Item item, ModelKey[] variants) {
      itemVariants.computeIfAbsent(item, k -> new ArrayList<>()).addAll(List.of(variants));

      for (ModelKey key : variants) {
         require(key);
      }
   }

   public static ModelKey itemModelKey(ItemStack stack) {
      Item item = stack.getItem();
      ModelRegistry.ItemMeshDefinition mesh = meshes.get(item);
      if (mesh != null) {
         return mesh.func_178113_a(stack);
      } else {
         Map<Integer, ModelKey> variants = metadata.get(item);
         if (variants == null) {
            return null;
         } else {
            ModelKey key = variants.get(ItemStacks.rawMeta(stack));
            return key != null ? key : (variants.size() == 1 ? variants.values().iterator().next() : null);
         }
      }
   }

   public static void setCustomStateMapper(Block block, ModelRegistry.IStateMapper mapper) {
      states.put(block, mapper);
      mapper.func_178130_a(block).values().forEach(ModelRegistry::require);
   }

   public static Map<Item, ModelKey> defaultItemModels() {
      IdentityHashMap<Item, ModelKey> defaults = new IdentityHashMap<>();
      metadata.forEach((item, variants) -> defaults.put(item, variants.entrySet().stream().min(Entry.comparingByKey()).orElseThrow().getValue()));
      itemVariants.forEach((item, variants) -> {
         if (!variants.isEmpty()) {
            defaults.putIfAbsent(item, variants.getFirst());
         }
      });
      return defaults;
   }

   private static IReloadableModel source(Identifier id) {
      return procedural.get(Identifier.fromNamespaceAndPath(id.getNamespace(), "models/" + id.getPath()));
   }

   @SubscribeEvent
   public static void register(RegisterStandalone event) {
      for (Entry<ModelKey, StandaloneModelKey<Ic2Geometry>> entry : keys.entrySet()) {
         ModelKey key = entry.getKey();
         final Variant selection = ModelBindings.resolve(key);
         final Identifier id = selection.modelLocation();
         event.register(
            entry.getValue(),
            new UnbakedStandaloneModel<Ic2Geometry>() {
               public void resolveDependencies(Resolver resolver) {
                  IReloadableModel source = ModelRegistry.source(id);
                  if (source == null) {
                     resolver.markDependency(id);
                  } else {
                     source.onReload();
                     source.getTextures();
                     source.getDependencies().forEach(resolver::markDependency);
                  }
               }

               public Ic2Geometry bake(ModelBaker baker, ModelDebugName name) {
                  ModelRegistry.currentBaker.set(baker);

                  Ic2Geometry nativeModel;
                  try {
                     IReloadableModel source = ModelRegistry.source(id);
                     if (source == null) {
                        SingleVariant nativeModelx = new SingleVariant(selection.bake(baker));
                        ItemTransforms transforms = baker.getModel(id).getTopTransforms();
                        return new NativeGeometry(nativeModelx, transforms, baker.getModel(id).getTopGuiLight().lightLikeBlock());
                     }

                     nativeModel = source.bake(
                        new Matrix4f(selection.modelState().asModelState().transformation().getMatrix()),
                        VdUtil.vertexFormat,
                        texture -> baker.materials().get(new Material(texture), name).sprite()
                     );
                  } finally {
                     ModelRegistry.currentBaker.remove();
                  }

                  return nativeModel;
               }
            }
         );
      }
   }

   @SubscribeEvent
   public static void finish(ModifyBakingResult event) {
      BakingResult result = event.getBakingResult();
      HashMap<ModelKey, Ic2Geometry> models = new HashMap<>();
      keys.forEach((key, standalone) -> {
         Ic2Geometry model = (Ic2Geometry)result.standaloneModels().get(standalone);
         if (model == null) {
            LoggerFactory.getLogger(ModelRegistry.class).error("IC2 model {} failed to bake; using the native missing model", key);
            model = new NativeGeometry(result.missingModels().block());
         }

         models.put(key, model);
      });
      baked = Map.copyOf(models);
      states.forEach((block, mapper) -> mapper.func_178130_a(block).forEach((state, key) -> {
         Ic2Geometry model = baked.get(key);
         if (model != null) {
            result.blockStateModels().put(state, model);
         }
      }));
      Set<Item> items = Collections.newSetFromMap(new IdentityHashMap<>());
      items.addAll(metadata.keySet());
      items.addAll(meshes.keySet());

      for (Item item : items) {
         Identifier id = BuiltInRegistries.ITEM.getKey(item);
         ItemModel fallback = (ItemModel)result.itemStackModels().get(id);
         result.itemStackModels().put(id, (output, stack, resolver, context, level, owner, seed) -> {
            ModelKey key = itemModelKey(stack);
            Ic2Geometry model = key == null ? null : baked.get(key);
            if (model != null) {
               model.update(output, stack, resolver, context, level, owner, seed);
            } else {
               (fallback != null ? fallback : result.missingModels().item()).update(output, stack, resolver, context, level, owner, seed);
            }
         });
      }

      ModelComparator.onReload();
   }

   public static Ic2Geometry get(ModelKey key) {
      Ic2Geometry model = baked.get(key);
      if (model == null) {
         throw new IllegalArgumentException("Unbaked IC2 model " + key);
      } else {
         return model;
      }
   }

   public static ModelSource getModel(final Identifier id) {
      IReloadableModel source = source(id);
      if (source != null) {
         return source;
      } else {
         final ModelBaker baker = currentBaker.get();
         if (baker == null) {
            throw new IllegalStateException("Model lookup outside native baking");
         } else {
            return new ModelSource() {
               @Override
               public Collection<Identifier> getDependencies() {
                  return List.of(id);
               }

               @Override
               public Collection<Identifier> getTextures() {
                  return List.of();
               }

               @Override
               public Matrix4f getDefaultState() {
                  return new Matrix4f();
               }

               @Override
               public Ic2Geometry bake(Matrix4f matrix, VertexFormat format, Function<Identifier, TextureAtlasSprite> textures) {
                  return new NativeGeometry(
                     (BlockStateModel)SimpleUnbakedStandaloneModel.blockStateModel(id).bake(baker, () -> id.toString()),
                     baker.getModel(id).getTopTransforms(),
                     baker.getModel(id).getTopGuiLight().lightLikeBlock()
                  );
               }
            };
         }
      }
   }

   public interface IStateMapper {
      Map<BlockState, ModelKey> func_178130_a(Block var1);
   }

   public interface ItemMeshDefinition {
      ModelKey func_178113_a(ItemStack var1);
   }
}
