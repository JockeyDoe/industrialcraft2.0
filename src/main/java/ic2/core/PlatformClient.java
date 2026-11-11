package ic2.core;

import ic2.core.audio.PositionSpec;
import ic2.core.block.BlockTileEntity;
import ic2.core.block.RenderBlockWall;
import ic2.core.block.TeBlockRegistry;
import ic2.core.block.transport.items.PipeModel;
import ic2.core.block.wiring.CableModel;
import ic2.core.crop.CropModel;
import ic2.core.gui.GlTexture;
import ic2.core.item.ElectricItemTooltipHandler;
import ic2.core.item.FluidCellModel;
import ic2.core.item.tool.RenderObscurator;
import ic2.core.model.Ic2ModelLoader;
import ic2.core.model.ModelRegistry;
import ic2.core.network.RpcHandler;
import ic2.core.ref.BlockName;
import ic2.core.ref.FluidName;
import ic2.core.ref.IBlockModelProvider;
import ic2.core.ref.IFluidModelProvider;
import ic2.core.ref.IItemModelProvider;
import ic2.core.ref.ItemName;
import ic2.core.util.LevelIds;
import ic2.core.util.LogCategory;
import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.NeoForge;

@OnlyIn(Dist.CLIENT)
public class PlatformClient extends Platform {
   @Override
   public boolean isRendering() {
      Minecraft client = Minecraft.getInstance();
      return client != null && client.isSameThread();
   }

   @Override
   public void preInit() {
      for (BlockName name : BlockName.values) {
         if (!name.hasInstance()) {
            IC2.log.warn(LogCategory.Block, "The block " + name + " is not initialized.");
         } else {
            ((IBlockModelProvider)name.getInstance()).registerModels(name);
         }
      }

      for (BlockTileEntity block : TeBlockRegistry.getAllBlocks()) {
         if (!block.isIC2()) {
            block.registerModels(null);
         }
      }

      for (ItemName namex : ItemName.values) {
         if (!namex.hasInstance()) {
            IC2.log.warn(LogCategory.Item, "The item " + namex + " is not initialized.");
         } else {
            ((IItemModelProvider)namex.getInstance()).registerModels(namex);
         }
      }

      for (FluidName namexx : FluidName.values) {
         if (!namexx.hasInstance()) {
            IC2.log.warn(LogCategory.Block, "The fluid " + namexx + " is not initialized.");
         } else {
            Fluid provider = namexx.getInstance();
            if (provider instanceof IFluidModelProvider) {
               ((IFluidModelProvider)provider).registerModels(namexx);
            }
         }
      }

      Ic2ModelLoader loader = new Ic2ModelLoader();
      loader.register("models/block/cf/wall", new RenderBlockWall());
      loader.register("models/block/crop/crop", new CropModel());
      loader.register("models/block/wiring/cable", new CableModel());
      loader.register("models/block/transport/item_pipe", new PipeModel());
      loader.register("models/item/cell/fluid_cell", new FluidCellModel());
      loader.register("models/item/tool/electric/obscurator", new RenderObscurator());
      ModelRegistry.registerLoader(loader);
      GlTexture.init();
   }

   @Override
   public void displayError(String error, Object... args) {
      super.displayError(error, args);
   }

   @Override
   public Player getPlayerInstance() {
      Minecraft client = Minecraft.getInstance();
      return client == null ? null : client.player;
   }

   @Override
   public Level getWorld(int dimId) {
      if (this.isSimulating()) {
         return super.getWorld(dimId);
      } else {
         Level world = this.getPlayerWorld();
         if (world == null) {
            return null;
         } else {
            return LevelIds.id(world) == dimId ? world : null;
         }
      }
   }

   @Override
   public Level getPlayerWorld() {
      Minecraft client = Minecraft.getInstance();
      return client == null ? null : client.level;
   }

   @Override
   public void messagePlayer(Player player, String message, Object... args) {
      if (args.length > 0) {
         Minecraft.getInstance().player.sendSystemMessage(Component.translatable(message, this.getMessageComponents(args)));
      } else {
         Minecraft.getInstance().player.sendSystemMessage(Component.literal(message));
      }
   }

   @Override
   public boolean launchGuiClient(Player player, IHasGui inventory, boolean isAdmin) {
      Minecraft.getInstance().gui.setScreen(inventory.getGui(player, isAdmin));
      return true;
   }

   @Override
   public void profilerStartSection(String section) {
      if (this.isRendering()) {
         Profiler.get().push(section);
      } else {
         super.profilerStartSection(section);
      }
   }

   @Override
   public void profilerEndSection() {
      if (this.isRendering()) {
         Profiler.get().pop();
      } else {
         super.profilerEndSection();
      }
   }

   @Override
   public void profilerEndStartSection(String section) {
      if (this.isRendering()) {
         Profiler.get().popPush(section);
      } else {
         super.profilerEndStartSection(section);
      }
   }

   @Override
   public File getMinecraftDir() {
      return super.getMinecraftDir();
   }

   @Override
   public void playSoundSp(String sound, float f, float g) {
      IC2.audioManager.playOnce(this.getPlayerInstance(), PositionSpec.Hand, sound, true, IC2.audioManager.getDefaultVolume());
   }

   @Override
   public void onPostInit() {
      NeoForge.EVENT_BUS.register(new GuiOverlayer(Minecraft.getInstance()));
      new RpcHandler();
      new ElectricItemTooltipHandler();
   }

   @Override
   public void requestTick(boolean simulating, Runnable runnable) {
      if (simulating) {
         super.requestTick(simulating, runnable);
      } else {
         Minecraft.getInstance().execute(runnable);
      }
   }

   @Override
   public int getColorMultiplier(BlockState state, BlockGetter world, BlockPos pos, int tint) {
      BlockTintSource source = Minecraft.getInstance().getBlockColors().getTintSource(state, tint);
      return source == null ? -1 : (world instanceof BlockAndTintGetter getter ? source.colorInWorld(state, getter, pos) : source.color(state));
   }
}
