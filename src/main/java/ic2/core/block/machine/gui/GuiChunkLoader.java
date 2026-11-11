package ic2.core.block.machine.gui;

import ic2.core.ChunkLoaderLogic;
import ic2.core.GuiIC2;
import ic2.core.IC2;
import ic2.core.block.machine.container.ContainerChunkLoader;
import ic2.core.gui.EnergyGauge;
import ic2.core.util.Ic2BlockPos;
import ic2.core.util.LogCategory;
import java.io.IOException;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiChunkLoader extends GuiIC2<ContainerChunkLoader> {
   private static final Identifier background = Identifier.fromNamespaceAndPath("ic2", "textures/gui/guichunkloader.png");

   public GuiChunkLoader(ContainerChunkLoader container) {
      super(container, 250);
      this.addElement(EnergyGauge.asBolt(this, 12, 125, container.base));
   }

   @Override
   protected Identifier getTexture() {
      return background;
   }

   @Override
   protected void drawForegroundLayer(int mouseX, int mouseY) {
      ChunkPos mainChunk = ChunkLoaderLogic.getChunkCoords(this.container.base.getBlockPos());
      Set<ChunkPos> loadedChunks = this.container.base.getLoadedChunks();
      int amountLoadedChunks = 0;

      for (int i = -4; i <= 4; i++) {
         for (int j = -4; j <= 4; j++) {
            ChunkPos currentChunk = new ChunkPos(mainChunk.x() + i, mainChunk.z() + j);
            int xpos = -this.leftPos + 89 + 16 * i;
            int ypos = -this.topPos + 80 + 16 * j;
            this.drawChunkAt(xpos, ypos, currentChunk);
            if (loadedChunks.contains(currentChunk)) {
               this.drawColoredRect(xpos, ypos, 16, 16, 805371648);
               amountLoadedChunks++;
            } else {
               this.drawColoredRect(xpos, ypos, 16, 16, 822018048);
            }
         }
      }

      this.getGraphics().text(this.getFont(), amountLoadedChunks + " / " + ChunkLoaderLogic.getInstance().getMaxChunksPerTicket(), 8, 16, -12566464, false);
      super.drawForegroundLayer(mouseX, mouseY);
   }

   private void drawChunkAt(int x, int y, ChunkPos chunkPos) {
      Level world = this.container.base.getLevel();
      LevelChunk chunk = world.getChunk(chunkPos.x(), chunkPos.z());
      Ic2BlockPos worldPos = new Ic2BlockPos();

      for (int cx = 0; cx < 16; cx++) {
         worldPos.setX(chunkPos.x() << 4 | cx);

         for (int cz = 0; cz < 16; cz++) {
            worldPos.setZ(chunkPos.z() << 4 | cz);
            worldPos.setY(chunk.getHeight(Types.WORLD_SURFACE, cx, cz));
            BlockState state = chunk.getBlockState(worldPos);
            if (state.isAir()) {
               worldPos.moveDown();
               state = chunk.getBlockState(worldPos);
            }

            this.drawColoredRect(x + cx, y + cz, 1, 1, this.getColor(state, world, worldPos));
         }
      }
   }

   private int getColor(BlockState state, Level world, BlockPos pos) {
      MapColor color = state.getMapColor(world, pos);
      if (color == null) {
         IC2.log.error(LogCategory.General, "BlockState " + state + " does not have a MapColor set. Please report to the mod author of that mod.");
         return 0;
      } else {
         return color.col | 0xFF000000;
      }
   }

   @Override
   protected void func_73864_a(int mouseX, int mouseY, int mouseButton) throws IOException {
      if (mouseButton == 0) {
         ChunkPos mainChunk = ChunkLoaderLogic.getChunkCoords(this.container.base.getBlockPos());

         for (int i = -4; i <= 4; i++) {
            for (int j = -4; j <= 4; j++) {
               if (mouseX - this.leftPos > 89 + 16 * i
                  && mouseX - this.leftPos <= 89 + 16 * i + 16
                  && mouseY - this.topPos > 80 + 16 * j
                  && mouseY - this.topPos <= 80 + 16 * j + 16) {
                  this.changeChunk(new ChunkPos(mainChunk.x() + i, mainChunk.z() + j));
                  return;
               }
            }
         }
      }

      super.func_73864_a(mouseX, mouseY, mouseButton);
   }

   private void changeChunk(ChunkPos chunk) {
      ChunkPos mainChunk = ChunkLoaderLogic.getChunkCoords(this.container.base.getBlockPos());
      IC2.network.get(false).initiateClientTileEntityEvent(this.container.base, chunk.x() - mainChunk.x() + 8 & 15 | (chunk.z() - mainChunk.z() + 8 & 15) << 4);
   }
}
