package ic2.core;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import ic2.api.crops.BiomeCategory;
import ic2.core.block.WorldGenRubTree;
import ic2.core.block.type.ResourceBlock;
import ic2.core.init.MainConfig;
import ic2.core.init.NativeRegistration;
import ic2.core.ref.BlockName;
import ic2.core.util.BiomeUtil;
import ic2.core.util.Config;
import ic2.core.util.ConfigUtil;
import ic2.core.util.Ic2BlockPos;
import ic2.core.util.LevelIds;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent.Load;
import net.neoforged.neoforge.event.level.ChunkEvent.Unload;
import net.neoforged.neoforge.registries.NeoForgeRegistries.Keys;

public class Ic2WorldDecorator {
   private static final AttachmentType<CompoundTag> GENERATION = NativeRegistration.add(
      Keys.ATTACHMENT_TYPES,
      IC2.getIdentifier("world_generation"),
      AttachmentType.builder(() -> new CompoundTag()).serialize(CompoundTag.CODEC.fieldOf("data")).build()
   );
   private static final boolean DEBUG = false;
   private static final String chunkDataTag = "ic2WorldGen";
   private static final String keyRubberTree = "rubberTree";
   private static final String keyCopperOre = "copperOre";
   private static final String keyLeadOre = "leadOre";
   private static final String keyTinOre = "tinOre";
   private static final String keyUraniumOre = "uraniumOre";
   public static final int chunkSize = 16;
   public static final int chunkOffset = 8;
   private static final int referenceHeight = 64;
   private static final TIntSet rubberTreeBlacklist = new TIntHashSet(ConfigUtil.asIntArray(MainConfig.get(), "worldgen/rubberTreeBlacklist"));

   public static void registerAttachment() {
   }

   public Ic2WorldDecorator() {
      this(true);
   }

   public Ic2WorldDecorator(boolean register) {
      if (register) {
         NeoForge.EVENT_BUS.register(this);
      }
   }

   @SubscribeEvent
   public void onChunkLoad(Load event) {
      if (event.getLevel() instanceof ServerLevel world) {
         LevelChunk var6 = (LevelChunk)event.getChunk();
         WorldData worldData = WorldData.get(world);
         if (!worldData.pendingUnloadChunks.remove(var6)) {
            CompoundTag nbt = ((CompoundTag)var6.getData(GENERATION)).copy();
            worldData.worldGenData.put(var6, nbt);
            if (!event.isNewChunk() && !nbt.getBooleanOr("pendingInitial", false)) {
               checkRetroGen(var6, nbt);
            } else {
               nbt.putBoolean("pendingInitial", true);
               var6.setData(GENERATION, nbt.copy());
               worldData.initialChunksToDecorate.add(var6);
            }
         }
      }
   }

   private static void checkRetroGen(LevelChunk chunk, CompoundTag nbt) {
      if (chunk.getPersistedStatus().isOrAfter(ChunkStatus.FULL)) {
         Config config = MainConfig.get().getSub("worldgen");
         if (getCheckLimit(config) > 0 && getUpdateLimit(config) > 0) {
            float epsilon = 1.0E-5F;
            float treeScale = getTreeScale(config) - epsilon;
            float oreScale = getOreScale(config, getBaseHeight(config, chunk.getLevel())) - epsilon;
            if ((!(treeScale <= 0.0F) || !(oreScale <= 0.0F))
               && (
                  rubberTreeGenEnabled(config, chunk.getLevel()) && nbt.getFloatOr("rubberTree", 0.0F) < treeScale
                     || ConfigUtil.getBool(config, "copper/enabled") && nbt.getFloatOr("copperOre", 0.0F) < oreScale
                     || ConfigUtil.getBool(config, "lead/enabled") && nbt.getFloatOr("leadOre", 0.0F) < oreScale
                     || ConfigUtil.getBool(config, "tin/enabled") && nbt.getFloatOr("tinOre", 0.0F) < oreScale
                     || ConfigUtil.getBool(config, "uranium/enabled") && nbt.getFloatOr("uraniumOre", 0.0F) < oreScale
               )) {
               WorldData.get(chunk.getLevel()).chunksToDecorate.add(chunk);
            }
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public void onChunkUnload(Unload event) {
      if (event.getLevel() instanceof ServerLevel world) {
         LevelChunk chunk = (LevelChunk)event.getChunk();
         WorldData worldData = WorldData.get(world, false);
         if (worldData != null) {
            worldData.pendingUnloadChunks.add(chunk);
         }
      }
   }

   private static void applyPendingUnloads(WorldData worldData) {
      Collection<LevelChunk> chunks = worldData.pendingUnloadChunks;
      if (!chunks.isEmpty()) {
         for (LevelChunk chunk : chunks) {
            worldData.worldGenData.remove(chunk);
            worldData.initialChunksToDecorate.remove(chunk);
            if (worldData.chunksToDecorate.remove(chunk)) {
            }
         }

         chunks.clear();
      }
   }

   public static void onTick(Level world, WorldData worldData) {
      applyPendingUnloads(worldData);
      if (world instanceof ServerLevel server) {
         for (LevelChunk chunk : List.copyOf(worldData.initialChunksToDecorate)) {
            if (hasNeighborChunks(chunk)) {
               Random seed = new Random(server.getSeed());
               long xSeed = seed.nextLong() >> 3;
               long zSeed = seed.nextLong() >> 3;
               seed.setSeed(xSeed * chunk.getPos().x() + zSeed * chunk.getPos().z() ^ server.getSeed());
               new Ic2WorldDecorator(false)
                  .generate(seed, chunk.getPos().x(), chunk.getPos().z(), server, server.getChunkSource().getGenerator(), server.getChunkSource());
               CompoundTag nbt = worldData.worldGenData.get(chunk);
               nbt.remove("pendingInitial");
               chunk.setData(GENERATION, nbt.copy());
               chunk.markUnsaved();
               worldData.initialChunksToDecorate.remove(chunk);
            }
         }

         if (!worldData.chunksToDecorate.isEmpty()) {
            Config config = MainConfig.get().getSub("worldgen");
            int chunksToCheck = getCheckLimit(config);
            int chunksToDecorate = getUpdateLimit(config);
            if (chunksToCheck <= 0 || chunksToDecorate <= 0) {
               return;
            }

            long worldSeed = server.getSeed();
            Random rnd = new Random(worldSeed);
            long xSeed = rnd.nextLong() >> 3;
            long zSeed = rnd.nextLong() >> 3;
            int baseHeight = getBaseHeight(config, world);
            int worldHeight = world.getHeight();
            float treeScale = getTreeScale(config);
            float oreScale = getOreScale(config, baseHeight);
            int skip = worldData.chunksToDecorate.size() - chunksToCheck;
            if (skip > 0) {
               skip = IC2.random.nextInt(skip + 1);
            }

            Iterator<LevelChunk> it = List.copyOf(worldData.chunksToDecorate).iterator();

            while (skip > 0) {
               skip--;
               it.next();
            }

            while (it.hasNext()) {
               LevelChunk chunkx = it.next();
               if (hasNeighborChunks(chunkx)) {
                  CompoundTag nbt = worldData.worldGenData.get(chunkx);
                  if (nbt == null) {
                     nbt = new CompoundTag();
                  }

                  long chunkSeed = xSeed * chunkx.getPos().x() + zSeed * chunkx.getPos().z() ^ worldSeed;
                  rnd.setSeed(chunkSeed);
                  long rubberTreeSeed = rnd.nextLong();
                  long copperOreSeed = rnd.nextLong();
                  long tinOreSeed = rnd.nextLong();
                  long uraniumOreSeed = rnd.nextLong();
                  long leadOreSeed = rnd.nextLong();
                  float extra;
                  if (rubberTreeGenEnabled(config, world) && (extra = treeScale - nbt.getFloatOr("rubberTree", 0.0F)) > 0.0F) {
                     genRubberTree(rnd, rubberTreeSeed, chunkx, extra);
                  }

                  if ((extra = oreScale - nbt.getFloatOr("copperOre", 0.0F)) > 0.0F) {
                     genOre(
                        rnd,
                        copperOreSeed,
                        chunkx,
                        BlockName.resource.getBlockState(ResourceBlock.copper_ore),
                        "copperOre",
                        config.getSub("copper"),
                        baseHeight,
                        worldHeight,
                        extra
                     );
                  }

                  if ((extra = oreScale - nbt.getFloatOr("leadOre", 0.0F)) > 0.0F) {
                     genOre(
                        rnd,
                        leadOreSeed,
                        chunkx,
                        BlockName.resource.getBlockState(ResourceBlock.lead_ore),
                        "leadOre",
                        config.getSub("lead"),
                        baseHeight,
                        worldHeight,
                        extra
                     );
                  }

                  if ((extra = oreScale - nbt.getFloatOr("tinOre", 0.0F)) > 0.0F) {
                     genOre(
                        rnd,
                        tinOreSeed,
                        chunkx,
                        BlockName.resource.getBlockState(ResourceBlock.tin_ore),
                        "tinOre",
                        config.getSub("tin"),
                        baseHeight,
                        worldHeight,
                        extra
                     );
                  }

                  if ((extra = oreScale - nbt.getFloatOr("uraniumOre", 0.0F)) > 0.0F) {
                     genOre(
                        rnd,
                        uraniumOreSeed,
                        chunkx,
                        BlockName.resource.getBlockState(ResourceBlock.uranium_ore),
                        "uraniumOre",
                        config.getSub("uranium"),
                        baseHeight,
                        worldHeight,
                        extra
                     );
                  }

                  worldData.chunksToDecorate.remove(chunkx);
                  if (--chunksToDecorate == 0) {
                     break;
                  }
               }

               if (--chunksToCheck == 0) {
                  break;
               }
            }
         }
      }
   }

   private static boolean hasNeighborChunks(LevelChunk chunk) {
      Level world = chunk.getLevel();
      Ic2BlockPos pos = new Ic2BlockPos();

      for (int dx = 0; dx <= 1; dx++) {
         for (int dz = 0; dz <= 1; dz++) {
            if (dx != 0 || dz != 0) {
               pos.set((chunk.getPos().x() + dx) * 16, 0, (chunk.getPos().z() + dz) * 16);
               if (!world.hasChunkAt(pos)) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   public void generate(Random rnd, int chunkX, int chunkZ, Level world, ChunkGenerator chunkGenerator, ServerChunkCache chunkProvider) {
      LevelChunk chunk = world.getChunk(chunkX, chunkZ);

      assert hasNeighborChunks(chunk);

      long rubberTreeSeed = rnd.nextLong();
      long copperOreSeed = rnd.nextLong();
      long tinOreSeed = rnd.nextLong();
      long uraniumOreSeed = rnd.nextLong();
      long leadOreSeed = rnd.nextLong();
      Config config = MainConfig.get().getSub("worldgen");
      int baseHeight = getBaseHeight(config, world);
      float treeScale = getTreeScale(config);
      float oreScale = getOreScale(config, baseHeight);
      if (rubberTreeGenEnabled(config, world) && treeScale > 0.0F) {
         genRubberTree(rnd, rubberTreeSeed, chunk, treeScale);
      }

      if (oreScale > 0.0F) {
         int worldHeight = world.getHeight();
         genOre(
            rnd,
            copperOreSeed,
            chunk,
            BlockName.resource.getBlockState(ResourceBlock.copper_ore),
            "copperOre",
            config.getSub("copper"),
            baseHeight,
            worldHeight,
            oreScale
         );
         genOre(
            rnd,
            leadOreSeed,
            chunk,
            BlockName.resource.getBlockState(ResourceBlock.lead_ore),
            "leadOre",
            config.getSub("lead"),
            baseHeight,
            worldHeight,
            oreScale
         );
         genOre(
            rnd, tinOreSeed, chunk, BlockName.resource.getBlockState(ResourceBlock.tin_ore), "tinOre", config.getSub("tin"), baseHeight, worldHeight, oreScale
         );
         genOre(
            rnd,
            uraniumOreSeed,
            chunk,
            BlockName.resource.getBlockState(ResourceBlock.uranium_ore),
            "uraniumOre",
            config.getSub("uranium"),
            baseHeight,
            worldHeight,
            oreScale
         );
      }
   }

   private static void genRubberTree(Random rnd, long seed, LevelChunk chunk, float baseScale) {
      rnd.setSeed(seed);
      Holder<Biome>[] biomes = new Holder[4];

      for (int i = 0; i < 4; i++) {
         int x = chunk.getPos().x() * 16 + 8 + (i & 1) * 15;
         int z = chunk.getPos().z() * 16 + 8 + ((i & 2) >>> 1) * 15;
         BlockPos pos = new BlockPos(x, chunk.getLevel().getSeaLevel(), z);
         biomes[i] = BiomeUtil.getOriginalBiome(chunk.getLevel(), pos);
      }

      int rubberTrees = 0;

      for (Holder<Biome> biome : biomes) {
         if (biome != null) {
            if (BiomeCategory.hasType(biome, BiomeCategory.SWAMP)) {
               rubberTrees += rnd.nextInt(10) + 5;
            }

            if (BiomeCategory.hasType(biome, BiomeCategory.FOREST) || BiomeCategory.hasType(biome, BiomeCategory.JUNGLE)) {
               rubberTrees += rnd.nextInt(5) + 1;
            }
         }
      }

      rubberTrees = Math.round(rubberTrees * baseScale);
      rubberTrees /= 2;
      if (rubberTrees > 0 && rnd.nextInt(100) < rubberTrees) {
         WorldGenRubTree gen = new WorldGenRubTree(false);

         for (int i = 0; i < rubberTrees; i++) {
            if (!gen.func_180709_b(chunk.getLevel(), rnd, new BlockPos(randomX(chunk, rnd), chunk.getLevel().getSeaLevel(), randomZ(chunk, rnd)))) {
               rubberTrees -= 3;
            }
         }
      }

      updateScale(chunk, "rubberTree", baseScale);
   }

   private static void genOre(
      Random rnd, long seed, LevelChunk chunk, BlockState ore, String oreScaleKey, Config config, int baseHeight, int worldHeight, float baseScale
   ) {
      if (ConfigUtil.getBool(config, "enabled")) {
         rnd.setSeed(seed);
         int count = ConfigUtil.getInt(config, "count");
         int size = ConfigUtil.getInt(config, "size");
         int minHeight = ConfigUtil.getInt(config, "minHeight");
         int maxHeight = ConfigUtil.getInt(config, "maxHeight");
         Ic2WorldDecorator.OreDistribution distribution = Ic2WorldDecorator.OreDistribution.of(ConfigUtil.getString(config, "distribution"));
         float baseCount = count * baseScale / 64.0F;
         count = (int)Math.round(rnd.nextGaussian() * Math.sqrt(baseCount) + baseCount);
         OreHeightSampler.Range range = OreHeightSampler.Range.clipped(
            minHeight, maxHeight, baseHeight, chunk.getLevel().getMinY(), chunk.getLevel().getMinY() + worldHeight
         );
         minHeight = range.min();
         maxHeight = range.maxExclusive();
         OreHeightSampler favored = null;
         if (distribution == Ic2WorldDecorator.OreDistribution.FAVORED && !range.isEmpty()) {
            favored = new OreHeightSampler(
               range,
               Math.toIntExact((long)ConfigUtil.getInt(config, "favoredMinHeight") * baseHeight / 64L),
               Math.toIntExact((long)ConfigUtil.getInt(config, "favoredMaxHeight") * baseHeight / 64L)
            );
         }

         int heightSpan = maxHeight - minHeight;
         if (heightSpan != 0) {
            Feature gen = new OreFeature(new TagMatchTest(BlockTags.BASE_STONE_OVERWORLD), ore, size);
            MutableBlockPos pos = new MutableBlockPos();

            for (int n = 0; n < count; n++) {
               int x = randomX(chunk, rnd);
               int z = randomZ(chunk, rnd);

               pos.set(x, switch (distribution) {
                  case UNIFORM -> minHeight + rnd.nextInt(heightSpan);
                  case TRIANGLE -> {
                     int halfHeightSpan = heightSpan >>> 1;
                     yield minHeight + rnd.nextInt(halfHeightSpan + 1) + rnd.nextInt(heightSpan - halfHeightSpan);
                  }
                  case RAMP -> minHeight + (heightSpan - 1 - (int)Math.sqrt(rnd.nextInt(heightSpan * heightSpan)));
                  case REVRAMP -> minHeight + (int)Math.sqrt(rnd.nextInt(heightSpan * heightSpan));
                  case SMOOTH -> {
                     int maxA = (heightSpan * 4 + 6) / 7;
                     int var32 = minHeight + rnd.nextInt(maxA);
                     int maxB = ((heightSpan - maxA + 1) * 2 + 2) / 3;
                     var32 += rnd.nextInt(maxB);
                     int maxC = heightSpan - maxA - maxB + 2;
                     yield var32 + rnd.nextInt(maxC);
                  }
                  case FAVORED -> favored.sample(rnd);
                  default -> throw new IllegalStateException();
               }, z);
               ServerLevel server = (ServerLevel)chunk.getLevel();
               gen.place(server, server.getChunkSource().getGenerator(), RandomSource.create(rnd.nextLong()), pos);
            }

            updateScale(chunk, oreScaleKey, baseScale);
         } else {
            updateScale(chunk, oreScaleKey, baseScale);
         }
      }
   }

   private static int getBaseHeight(Config config, Level world) {
      return ConfigUtil.getBool(config, "normalizeHeight") ? world.getSeaLevel() + 1 : 64;
   }

   private static boolean rubberTreeGenEnabled(Config config, Level world) {
      return ConfigUtil.getBool(config, "rubberTree") && !rubberTreeBlacklist.contains(LevelIds.id(world));
   }

   private static float getTreeScale(Config config) {
      return ConfigUtil.getFloat(config, "treeDensityFactor");
   }

   private static float getOreScale(Config config, int baseHeight) {
      return ConfigUtil.getFloat(config, "oreDensityFactor") * baseHeight;
   }

   private static int getCheckLimit(Config config) {
      return ConfigUtil.getInt(config, "retrogenCheckLimit");
   }

   private static int getUpdateLimit(Config config) {
      return ConfigUtil.getInt(config, "retrogenUpdateLimit");
   }

   private static void updateScale(LevelChunk chunk, String key, float scale) {
      WorldData worldData = WorldData.get(chunk.getLevel());
      CompoundTag nbt = worldData.worldGenData.get(chunk);
      if (nbt == null) {
         nbt = new CompoundTag();
         worldData.worldGenData.put(chunk, nbt);
      }

      nbt.putFloat(key, nbt.getFloatOr(key, 0.0F) + scale);
      chunk.setData(GENERATION, nbt.copy());
      chunk.markUnsaved();
   }

   private static int zeroRnd(Random rnd, int limit) {
      if (limit < 0) {
         throw new IllegalArgumentException("The limit must not be negative: " + limit);
      } else {
         return limit == 0 ? 0 : rnd.nextInt(limit);
      }
   }

   private static int randomX(LevelChunk chunk, Random rnd) {
      return chunk.getPos().x() * 16 + rnd.nextInt(16);
   }

   private static int randomZ(LevelChunk chunk, Random rnd) {
      return chunk.getPos().z() * 16 + rnd.nextInt(16);
   }

   private static enum OreDistribution {
      UNIFORM("uniform"),
      TRIANGLE("triangle"),
      RAMP("ramp"),
      REVRAMP("revramp"),
      SMOOTH("smooth"),
      FAVORED("favored");

      private static final Ic2WorldDecorator.OreDistribution[] values = values();
      final String name;

      private OreDistribution(String name) {
         this.name = name;
      }

      public static Ic2WorldDecorator.OreDistribution of(String name) {
         for (Ic2WorldDecorator.OreDistribution value : values) {
            if (value.name.equalsIgnoreCase(name)) {
               return value;
            }
         }

         throw new RuntimeException("Invalid/unknown worldgen distribution configured: " + name);
      }
   }
}
