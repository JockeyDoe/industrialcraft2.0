package ic2.api.crops;

import java.util.EnumSet;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.Tags.Biomes;

public enum BiomeCategory {
   WATER(Biomes.IS_AQUATIC),
   WET(Biomes.IS_WET),
   DRY(Biomes.IS_DRY),
   JUNGLE(Biomes.IS_JUNGLE),
   SWAMP(Biomes.IS_SWAMP),
   MUSHROOM(Biomes.IS_MUSHROOM),
   FOREST(Biomes.IS_FOREST),
   RIVER(Biomes.IS_RIVER),
   PLAINS(Biomes.IS_PLAINS),
   SAVANNA(Biomes.IS_SAVANNA),
   HILLS(Biomes.IS_HILL),
   MOUNTAIN(Biomes.IS_MOUNTAIN),
   WASTELAND(Biomes.IS_WASTELAND),
   END(Biomes.IS_END),
   NETHER(Biomes.IS_NETHER),
   DEAD(Biomes.IS_DEAD),
   HOT(Biomes.IS_HOT),
   COLD(Biomes.IS_COLD),
   SANDY(Biomes.IS_SANDY),
   OCEAN(Biomes.IS_OCEAN);

   public final TagKey<Biome> tag;

   private BiomeCategory(TagKey<Biome> tag) {
      this.tag = tag;
   }

   public static boolean hasType(Holder<Biome> biome, BiomeCategory category) {
      return biome.is(category.tag);
   }

   public static Set<BiomeCategory> getTypes(Holder<Biome> biome) {
      Set<BiomeCategory> result = EnumSet.noneOf(BiomeCategory.class);

      for (BiomeCategory category : values()) {
         if (biome.is(category.tag)) {
            result.add(category);
         }
      }

      return result;
   }
}
