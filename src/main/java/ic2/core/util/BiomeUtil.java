package ic2.core.util;

import ic2.api.crops.BiomeCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public final class BiomeUtil {
   public static Holder<Biome> getOriginalBiome(Level world, BlockPos pos) {
      return world instanceof ServerLevel server
         ? server.getChunkSource()
            .getGenerator()
            .getBiomeSource()
            .createUncachedResolver(server.getChunkSource().randomState())
            .getNoiseBiome(QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(pos.getY()), QuartPos.fromBlock(pos.getZ()))
         : world.getBiome(pos);
   }

   public static Holder<Biome> getBiome(Level world, BlockPos pos) {
      return world.getBiome(pos);
   }

   public static void setBiome(Level world, BlockPos pos, ResourceKey<Biome> biome) {
      setBiome(world, pos, world.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(biome));
   }

   public static void setBiome(Level world, BlockPos pos, Holder<Biome> biome) {
      LevelChunk chunk = world.getChunkAt(pos);
      int qx = QuartPos.fromBlock(pos.getX());
      int qz = QuartPos.fromBlock(pos.getZ());

      for (int index = 0; index < chunk.getSectionsCount(); index++) {
         LevelChunkSection section = chunk.getSection(index);
         int qy = QuartPos.fromSection(chunk.getSectionYFromSectionIndex(index));
         section.fillBiomesFromNoise(
            (x, y, z) -> x == qx && z == qz ? biome : section.getNoiseBiome(x & 3, y & 3, z & 3),
            QuartPos.fromSection(chunk.getPos().x()),
            qy,
            QuartPos.fromSection(chunk.getPos().z())
         );
      }

      chunk.markUnsaved();
   }

   public static int getBiomeTemperature(Level world, BlockPos pos) {
      Holder<Biome> biome = getBiome(world, pos);
      return biome.is(BiomeCategory.HOT.tag) ? 45 : (biome.is(BiomeCategory.COLD.tag) ? 0 : 25);
   }
}
