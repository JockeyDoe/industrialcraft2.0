package ic2.core.util;

import ic2.core.IC2DamageSource;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class WorldQueries {
   private WorldQueries() {
   }

   public static List<BlockEntity> loadedBlockEntities(ServerLevel level) {
      ArrayList<BlockEntity> entities = new ArrayList<>();
      level.getChunkSource().chunkMap.forEachReadyToSendChunk(chunk -> entities.addAll(chunk.getBlockEntities().values()));
      return entities;
   }

   public static boolean hasTicker(Level level, BlockEntity entity) {
      return entity != null && ticker(level, entity) != null;
   }

   private static BlockEntityTicker ticker(Level level, BlockEntity entity) {
      return entity.getBlockState().getTicker(level, entity.getType());
   }

   public static void tickBlockEntity(Level level, BlockEntity entity) {
      BlockEntityTicker ticker = ticker(level, entity);
      if (ticker != null) {
         ticker.tick(level, entity.getBlockPos(), entity.getBlockState(), entity);
      }
   }

   public static BlockHitResult clip(Level world, Vec3 start, Vec3 end, boolean liquids) {
      BlockHitResult hit = world.clip(new ClipContext(start, end, Block.COLLIDER, liquids ? Fluid.ANY : Fluid.NONE, CollisionContext.empty()));
      return hit.getType() == Type.MISS ? null : hit;
   }

   public static HitResult intersect(AABB box, Vec3 start, Vec3 end) {
      return AABB.clip(List.of(box), start, end, BlockPos.ZERO);
   }

   public static boolean hurt(Entity entity, DamageSource source, float amount) {
      if (entity.level() instanceof ServerLevel server) {
         if (source instanceof IC2DamageSource ic2) {
            source = ic2.in(server);
         }

         return entity.hurtServer(server, source, amount);
      } else {
         return false;
      }
   }
}
