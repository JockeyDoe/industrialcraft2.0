package ic2.core.uu;

import ic2.core.IC2;
import ic2.core.Ic2Player;
import ic2.core.Ic2WorldDecorator;
import ic2.core.WorldData;
import ic2.core.init.MainConfig;
import ic2.core.util.Config;
import ic2.core.util.ConfigUtil;
import ic2.core.util.ItemComparableItemStack;
import ic2.core.util.LogCategory;
import ic2.core.util.StackUtil;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Random;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess;
import org.apache.commons.lang3.mutable.MutableLong;

public class DropScan {
   private final ServerLevel parentWorld;
   private final int range;
   private final Path tmpDir;
   private final LevelStorageAccess storage;
   private ServerLevel world;
   private Player player;
   private boolean sampledArea;
   private final Map<ItemComparableItemStack, MutableLong> drops = new HashMap<>();
   private final Map<BlockState, DropScan.DropDesc> typicalDrops = new IdentityHashMap<>();

   public DropScan(ServerLevel parentWorld, int range) {
      if (parentWorld == null) {
         throw new NullPointerException("null world");
      } else if (range < 4) {
         throw new IllegalArgumentException("range has to be at least 4");
      } else {
         this.parentWorld = parentWorld;
         this.range = range;

         try {
            this.tmpDir = Files.createTempDirectory("ic2uuscan-").toRealPath();
            this.storage = LevelStorageSource.createDefault(this.tmpDir).createAccess("scan");
            this.world = this.createScanWorld();
            this.world.noSave = true;
            this.player = Ic2Player.get(this.world);
         } catch (IOException var4) {
            throw new UncheckedIOException("Cannot open isolated UU scan world", var4);
         }

         IC2.log.info(LogCategory.Uu, "Using %s for isolated native world generation.", this.tmpDir);
      }
   }

   private ServerLevel createScanWorld() {
      ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, IC2.getIdentifier("uu_scan_" + UUID.randomUUID().toString()));
      return new ServerLevel(
         this.parentWorld.getServer(),
         Runnable::run,
         this.storage,
         new DerivedLevelData(this.parentWorld.getServer().getWorldData(), (ServerLevelData)this.parentWorld.getLevelData()),
         key,
         new LevelStem(
            this.parentWorld.dimensionTypeRegistration(), this.parentWorld.getChunkSource().getGenerator(), OptionalLong.of(this.parentWorld.getSeed())
         ),
         false,
         this.parentWorld.getSeed(),
         List.of(),
         false
      );
   }

   public void start(int area, int areaCount) {
      if (area > 0 && areaCount > 0) {
         long lastPrint = 0L;

         for (int i = 0; i < areaCount; i++) {
            if (Thread.currentThread().isInterrupted() || !this.parentWorld.getServer().isRunning()) {
               return;
            }

            int x = IC2.random.nextInt(area) - area / 2;
            int z = IC2.random.nextInt(area) - area / 2;

            try {
               this.scanArea(x, z);
            } catch (Exception var9) {
               IC2.log.warn(LogCategory.Uu, var9, "Scan failed.");
            }

            if (i % 4 == 0 && lastPrint <= System.nanoTime() - 10000000000L) {
               lastPrint = System.nanoTime();
               IC2.log.info(LogCategory.Uu, "World scan progress: %.1f%%.", 100.0F * i / areaCount);
            }
         }

         this.parentWorld.getServer().execute(this::analyze);
      } else {
         throw new IllegalArgumentException("Invalid scan area");
      }
   }

   public void cleanup() {
      try {
         this.world.close();
         this.storage.close();
         WorldData.onWorldUnload(this.world);
         Path root = this.tmpDir.toRealPath();

         try (Stream<Path> files = Files.walk(root)) {
            for (Path path : files.sorted(Comparator.reverseOrder()).toList()) {
               if (!path.toAbsolutePath().normalize().startsWith(root)) {
                  throw new IOException("Scan cleanup escaped temporary directory");
               }

               Files.deleteIfExists(path);
            }
         }
      } catch (IOException var7) {
         throw new UncheckedIOException("Cannot close UU scan storage", var7);
      }
   }

   private void scanArea(int xStart, int zStart) {
      if (this.sampledArea) {
         try {
            this.world.close();
         } catch (IOException var12) {
            throw new UncheckedIOException(var12);
         }

         WorldData.onWorldUnload(this.world);
         this.world = this.createScanWorld();
         this.world.noSave = true;
         this.player = Ic2Player.get(this.world);
      }

      this.sampledArea = true;
      List<LevelChunk> scan = new ArrayList<>();

      for (int x = xStart; x < xStart + this.range; x++) {
         for (int z = zStart; z < zStart + this.range; z++) {
            LevelChunk chunk = this.world.getChunk(x, z);
            if (x > xStart && x < xStart + this.range - 2 && z > zStart && z < zStart + this.range - 2) {
               scan.add(chunk);
            }
         }
      }

      Ic2WorldDecorator decorator = new Ic2WorldDecorator(false);
      Random random = new Random(this.parentWorld.getSeed());
      long xSeed = random.nextLong() >> 3;
      long zSeed = random.nextLong() >> 3;

      for (int x = xStart; x < xStart + this.range - 1; x++) {
         for (int zx = zStart; zx < zStart + this.range - 1; zx++) {
            random.setSeed(xSeed * x + zSeed * zx ^ this.parentWorld.getSeed());
            decorator.generate(random, x, zx, this.world, this.world.getChunkSource().getGenerator(), this.world.getChunkSource());
         }
      }

      for (LevelChunk chunk : scan) {
         this.scanChunk(chunk);
      }

      this.world.getChunkSource().tick(() -> true, false);
   }

   private void scanChunk(LevelChunk chunk) {
      int xMin = chunk.getPos().x() * 16;
      int zMin = chunk.getPos().z() * 16;
      MutableBlockPos pos = new MutableBlockPos();

      for (int y = this.world.getMinY(); y < this.world.getMaxY(); y++) {
         for (int z = zMin; z < zMin + 16; z++) {
            for (int x = xMin; x < xMin + 16; x++) {
               pos.set(x, y, z);
               BlockState state = chunk.getBlockState(pos);
               if (!state.isAir()) {
                  for (ItemStack drop : this.getDrops(pos, state)) {
                     this.addDrop(drop);
                  }
               }
            }
         }
      }
   }

   private List<ItemStack> getDrops(BlockPos pos, BlockState state) {
      DropScan.DropDesc typical = this.typicalDrops.get(state);
      if (typical != null && typical.dropCount.get() >= 1000) {
         return typical.drops;
      } else {
         List<ItemStack> result = Block.getDrops(state, this.world, pos, this.world.getBlockEntity(pos), this.player, ItemStack.EMPTY);
         if (typical == null) {
            typical = new DropScan.DropDesc(result);
            this.typicalDrops.put(state, typical);
         }

         if (typical.dropCount.get() >= 0) {
            boolean equal = typical.drops.size() == result.size();

            for (int i = 0; equal && i < result.size(); i++) {
               equal = ItemStack.matches(typical.drops.get(i), result.get(i));
            }

            if (equal) {
               if (typical.dropCount.incrementAndGet() < 0) {
                  typical.dropCount.set(Integer.MIN_VALUE);
               }
            } else {
               typical.dropCount.set(Integer.MIN_VALUE);
            }
         }

         return result;
      }
   }

   private void analyze() {
      ItemComparableItemStack cobblestone = new ItemComparableItemStack(new ItemStack(Blocks.COBBLESTONE), false);
      ItemComparableItemStack netherrack = new ItemComparableItemStack(new ItemStack(Blocks.NETHERRACK), false);
      double normalizeBy;
      if (!this.drops.containsKey(cobblestone)) {
         if (!this.drops.containsKey(netherrack)) {
            IC2.log.warn(LogCategory.Uu, "UU scan failed, there was no cobblestone or netherrack dropped");
            return;
         }

         normalizeBy = this.drops.get(netherrack).getValue().longValue();
      } else {
         normalizeBy = this.drops.get(cobblestone).getValue().longValue();
         if (this.drops.containsKey(netherrack)) {
            normalizeBy = Math.max(normalizeBy, (double)this.drops.get(netherrack).getValue().longValue());
         }
      }

      Config config = MainConfig.get().getSub("balance/uu-values/world scan");
      if (config == null) {
         config = MainConfig.get()
            .getSub("balance/uu-values")
            .addSub(
               "world scan",
               "Initial uu values from scanning the world.\nRun /ic2 uu-world-scan <small|medium|large> to calibrate them for your world.\nDelete this whole section to revert to the default predefined values."
            );
      }

      List<Entry<ItemComparableItemStack, MutableLong>> sorted = new ArrayList<>(this.drops.entrySet());
      this.drops.clear();
      Collections.sort(sorted, new Comparator<Entry<ItemComparableItemStack, MutableLong>>() {
         {
            Objects.requireNonNull(DropScan.this);
         }

         public int compare(Entry<ItemComparableItemStack, MutableLong> a, Entry<ItemComparableItemStack, MutableLong> b) {
            return Long.compare(b.getValue().getValue(), a.getValue().getValue());
         }
      });
      IC2.log.info(LogCategory.Uu, "total");

      for (Entry<ItemComparableItemStack, MutableLong> entry : sorted) {
         ItemStack stack = entry.getKey().toStack();
         long count = entry.getValue().getValue();
         IC2.log.info(LogCategory.Uu, "%d %s", count, stack.getHoverName().getString());
         config.set(ConfigUtil.fromStack(stack), normalizeBy / count);
      }

      MainConfig.save();
   }

   private void addDrop(ItemStack stack) {
      ItemComparableItemStack key = new ItemComparableItemStack(stack, false);
      MutableLong amount = this.drops.get(key);
      if (amount == null) {
         amount = new MutableLong();
         this.drops.put(key.copy(), amount);
      }

      amount.add(StackUtil.getSize(stack));
   }

   private static final class DropDesc {
      List<ItemStack> drops;
      AtomicInteger dropCount = new AtomicInteger();

      DropDesc(List<ItemStack> drops) {
         this.drops = drops;
      }
   }
}
