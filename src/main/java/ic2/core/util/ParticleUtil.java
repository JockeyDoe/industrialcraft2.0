package ic2.core.util;

import ic2.core.block.BlockTileEntity;
import ic2.core.block.ITeBlock;
import ic2.core.block.TeBlockRegistry;
import ic2.core.block.TileEntityBlock;
import ic2.core.block.state.Ic2BlockState;
import ic2.core.model.ISpecialParticleModel;
import ic2.core.model.Ic2Geometry;
import ic2.core.model.ModelUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class ParticleUtil {
   public static void showFlames(Level world, BlockPos pos, Direction facing) {
      if (world.getRandom().nextInt(8) == 0) {
         double width = 0.625;
         double height = 0.625;
         double depthOffset = 0.02;
         double x = pos.getX() + (facing.getStepX() * 1.04 + 1.0) / 2.0;
         double y = pos.getY() + world.getRandom().nextFloat() * 0.625;
         double z = pos.getZ() + (facing.getStepZ() * 1.04 + 1.0) / 2.0;
         if (facing.getAxis() == Axis.X) {
            z += world.getRandom().nextFloat() * 0.625 - 0.3125;
         } else {
            x += world.getRandom().nextFloat() * 0.625 - 0.3125;
         }

         world.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
         world.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.0, 0.0);
      }
   }

   public static void spawnBlockLandParticles(Level world, BlockPos pos, double x, double y, double z, int count, ITeBlock teBlock) {
      Minecraft mc = Minecraft.getInstance();
      RandomSource rnd = world.getRandom();
      if (mc.level == world
         && mc.player != null
         && mc.options.particles().get() != ParticleStatus.MINIMAL
         && (mc.options.particles().get() != ParticleStatus.DECREASED || rnd.nextInt(3) != 0)
         && !(mc.player.distanceToSqr(x, y, z) > 1024.0)) {
         double speed = 0.15;
         BlockState state = TeBlockRegistry.get(teBlock.getIdentifier()).getState(teBlock);

         for (int i = 0; i < count; i++) {
            double mx = rnd.nextGaussian() * 0.15;
            double my = rnd.nextGaussian() * 0.15;
            double mz = rnd.nextGaussian() * 0.15;
            TerrainParticle particle = newParticleBlockDust(world, x, y, z, mx, my, mz, state);
            ensureTexture(world, pos, particle, state);
            mc.particleEngine.add(particle);
         }
      }
   }

   public static void spawnBlockRunParticles(Level world, BlockPos pos, double x, double y, double z, double xSpeed, double zSpeed, ITeBlock block) {
      BlockState state = TeBlockRegistry.get(block.getIdentifier()).getState(block);
      TerrainParticle particle = newParticleDigging(world, x, y, z, xSpeed, 1.5, zSpeed, state);
      ensureTexture(world, pos, particle, state);
      Minecraft.getInstance().particleEngine.add(particle);
   }

   public static void spawnBlockHitParticles(TileEntityBlock te, Direction side) {
      spawnBlockHitParticles(te, side, false);
   }

   public static void spawnBlockHitParticles(TileEntityBlock te, Direction side, boolean checkTexture) {
      Level world = te.getLevel();
      BlockPos pos = te.getBlockPos();
      double offset = 0.1;
      AABB aabb = te.getVisualBoundingBox();
      double x = pos.getX() + world.getRandom().nextDouble() * (aabb.maxX - aabb.minX - offset * 2.0) + offset + aabb.minX;
      double y = pos.getY() + world.getRandom().nextDouble() * (aabb.maxY - aabb.minY - offset * 2.0) + offset + aabb.minY;
      double z = pos.getZ() + world.getRandom().nextDouble() * (aabb.maxZ - aabb.minZ - offset * 2.0) + offset + aabb.minZ;
      switch (side) {
         case DOWN:
            y = pos.getY() + aabb.minY - offset;
            break;
         case UP:
            y = pos.getY() + aabb.maxY + offset;
            break;
         case NORTH:
            z = pos.getZ() + aabb.minZ - offset;
            break;
         case SOUTH:
            z = pos.getZ() + aabb.maxZ + offset;
            break;
         case WEST:
            x = pos.getX() + aabb.minX - offset;
            break;
         case EAST:
            x = pos.getX() + aabb.maxX + offset;
            break;
         default:
            throw new IllegalStateException("invalid facing: " + side);
      }

      TerrainParticle particle = newParticleDigging(world, x, y, z, 0.0, 0.0, 0.0, te.getBlockState());
      particle.updateSprite(te.getBlockState(), pos);
      particle.setPower(0.2F);
      particle.scale(0.6F);
      if (checkTexture) {
         ensureTexture(world, pos, particle, te.getBlockState());
      }

      Minecraft.getInstance().particleEngine.add(particle);
   }

   public static void spawnBlockBreakParticles(TileEntityBlock te) {
      Level world = te.getLevel();
      BlockPos pos = te.getBlockPos();
      BlockState state = te.getBlockState();
      Minecraft mc = Minecraft.getInstance();

      for (int x = 0; x < 4; x++) {
         for (int y = 0; y < 4; y++) {
            for (int z = 0; z < 4; z++) {
               double xOffset = (x + 0.5) / 4.0;
               double yOffset = (y + 0.5) / 4.0;
               double zOffset = (z + 0.5) / 4.0;
               TerrainParticle particle = newParticleDigging(
                  world, pos.getX() + xOffset, pos.getY() + yOffset, pos.getZ() + zOffset, xOffset - 0.5, yOffset - 0.5, zOffset - 0.5, state
               );
               particle.updateSprite(te.getBlockState(), pos);
               ensureTexture(world, pos, particle, state);
               mc.particleEngine.add(particle);
            }
         }
      }
   }

   public static TerrainParticle newParticleBlockDust(
      Level world, double xCoord, double yCoord, double zCoord, double xSpeed, double ySpeed, double zSpeed, BlockState state
   ) {
      if (world instanceof ClientLevel clientLevel) {
         return new Ic2TerrainParticle(clientLevel, xCoord, yCoord, zCoord, xSpeed, ySpeed, zSpeed, state);
      } else {
         throw new IllegalArgumentException("Terrain particles require a client level");
      }
   }

   public static TerrainParticle newParticleDigging(
      Level world, double xCoord, double yCoord, double zCoord, double xSpeed, double ySpeed, double zSpeed, BlockState state
   ) {
      if (world instanceof ClientLevel clientLevel) {
         return new Ic2TerrainParticle(clientLevel, xCoord, yCoord, zCoord, xSpeed, ySpeed, zSpeed, state);
      } else {
         throw new IllegalArgumentException("Terrain particles require a client level");
      }
   }

   public static void ensureTexture(Level world, BlockPos pos, Particle particle, BlockState state) {
      if (pos != null) {
         Ic2Geometry model = ModelUtil.getBlockModel(state);
         if (model instanceof ISpecialParticleModel && ((ISpecialParticleModel)model).needsEnhancing(state)) {
            if (state.getBlock() instanceof BlockTileEntity block) {
               state = block.getExtendedState(state, world, pos);
            }

            assert state instanceof Ic2BlockState.Ic2BlockStateInstance;

            ((ISpecialParticleModel)model).enhanceParticle(particle, (Ic2BlockState.Ic2BlockStateInstance)state);
         }
      }
   }
}
