package ic2.core.entity;

import ic2.core.IC2;
import ic2.core.block.EntityDynamite;
import ic2.core.block.EntityItnt;
import ic2.core.block.EntityNuke;
import ic2.core.block.EntityStickyDynamite;
import ic2.core.init.NativeRegistration;
import ic2.core.item.EntityBoatCarbon;
import ic2.core.item.EntityBoatElectric;
import ic2.core.item.EntityBoatRubber;
import ic2.core.item.ItemIC2Boat;
import ic2.core.item.tool.EntityMiningLaser;
import ic2.core.item.tool.EntityParticle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.level.Level;

public final class Ic2EntityTypes {
   private static final Map<String, EntityType<?>> types = new HashMap<>();

   private Ic2EntityTypes() {
   }

   public static void registerAll() {
      for (String id : List.of(
         "mining_laser", "dynamite", "sticky_dynamite", "itnt", "nuke", "particle", "beam", "rubber_boat", "carbon_boat", "electric_boat", "fireproof_item"
      )) {
         get(id);
      }
   }

   public static EntityType<?> get(String id) {
      return types.computeIfAbsent(
         id,
         key -> {
            float size = switch (key) {
               case "mining_laser" -> 0.8F;
               case "dynamite", "sticky_dynamite" -> 0.5F;
               case "itnt", "nuke" -> 0.98F;
               case "rubber_boat", "carbon_boat", "electric_boat" -> 1.375F;
               case "fireproof_item" -> 0.25F;
               default -> 0.2F;
            };
            Identifier identifier = IC2.getIdentifier(key);
            Builder<Entity> builder = Builder.of((entityType, world) -> create(key, world), MobCategory.MISC)
               .sized(size, key.endsWith("_boat") ? 0.5625F : size)
               .clientTrackingRange(!key.endsWith("_boat") && !key.equals("fireproof_item") ? 10 : 5)
               .updateInterval(key.endsWith("_boat") ? 3 : (!key.equals("particle") && !key.equals("beam") && !key.equals("fireproof_item") ? 5 : 1));
            if (key.equals("electric_boat") || key.equals("fireproof_item")) {
               builder.fireImmune();
            }

            EntityType<Entity> type = builder.build(ResourceKey.create(Registries.ENTITY_TYPE, identifier));
            return NativeRegistration.add(Registries.ENTITY_TYPE, identifier, type);
         }
      );
   }

   public static <T extends Entity> EntityType<T> get(String id, Class<T> cls) {
      return (EntityType<T>)get(id);
   }

   private static Entity create(String id, Level world) {
      return (Entity)(switch (id) {
         case "mining_laser" -> new EntityMiningLaser(world);
         case "dynamite" -> new EntityDynamite(world);
         case "sticky_dynamite" -> new EntityStickyDynamite(world);
         case "itnt" -> new EntityItnt(world);
         case "nuke" -> new EntityNuke(world);
         case "particle" -> new EntityParticle(world);
         case "beam" -> new ic2.core.block.beam.EntityParticle(world);
         case "rubber_boat" -> new EntityBoatRubber(world);
         case "carbon_boat" -> new EntityBoatCarbon(world);
         case "electric_boat" -> new EntityBoatElectric(world);
         case "fireproof_item" -> new ItemIC2Boat.FireproofItem(world);
         default -> throw new IllegalArgumentException(id);
      });
   }
}
