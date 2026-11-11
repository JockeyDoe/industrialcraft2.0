package ic2.core;

import ic2.core.util.LevelIds;
import ic2.core.util.Util;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class Platform {
   public boolean isSimulating() {
      return !this.isRendering();
   }

   public boolean isRendering() {
      return false;
   }

   public void displayError(String error, Object... args) {
      if (args.length > 0) {
         error = String.format(error, args);
      }

      error = "IndustrialCraft 2 Error\n\n == = IndustrialCraft 2 Error = == \n\n" + error + "\n\n == == == == == == == == == == ==\n";
      error = error.replace("\n", System.getProperty("line.separator"));
      throw new RuntimeException(error);
   }

   public void displayError(Exception e, String error, Object... args) {
      if (args.length > 0) {
         error = String.format(error, args);
      }

      this.displayError("An unexpected Exception occured.\n\n" + this.getStackTrace(e) + "\n" + error);
   }

   public String getStackTrace(Exception e) {
      StringWriter writer = new StringWriter();
      PrintWriter printWriter = new PrintWriter(writer);
      e.printStackTrace(printWriter);
      return writer.toString();
   }

   public Player getPlayerInstance() {
      return null;
   }

   public Level getWorld(int dimId) {
      return LevelIds.get(dimId);
   }

   public Level getPlayerWorld() {
      return null;
   }

   public void preInit() {
   }

   public void messagePlayer(Player player, String message, Object... args) {
      if (player instanceof ServerPlayer) {
         Component msg;
         if (args.length > 0) {
            msg = Component.translatable(message, this.getMessageComponents(args));
         } else {
            msg = Component.translatable(message, new Object[0]);
         }

         player.sendSystemMessage(msg);
      }
   }

   public boolean launchGui(Player player, IHasGui inventory) {
      return !Util.isFakePlayer(player, true) && player instanceof ServerPlayer server && NativeMenus.launch(server, inventory, null);
   }

   public boolean launchSubGui(Player player, IHasGui inventory, int id) {
      return !Util.isFakePlayer(player, true) && player instanceof ServerPlayer server && NativeMenus.launch(server, inventory, id);
   }

   public boolean launchGuiClient(Player player, IHasGui inventory, boolean isAdmin) {
      return false;
   }

   public void profilerStartSection(String section) {
   }

   public void profilerEndSection() {
   }

   public void profilerEndStartSection(String section) {
   }

   public File getMinecraftDir() {
      return FMLPaths.GAMEDIR.get().toFile();
   }

   public void playSoundSp(String sound, float f, float g) {
   }

   public void resetPlayerInAirTime(Player player) {
      if (player instanceof ServerPlayer) {
         ((ServerPlayer)player).connection.resetFlyingTicks();
      }
   }

   public int getBlockTexture(Block block, BlockGetter world, int x, int y, int z, int side) {
      return 0;
   }

   public void removePotion(LivingEntity entity, Holder<MobEffect> potion) {
      entity.removeEffect(potion);
   }

   public void onPostInit() {
   }

   protected Component[] getMessageComponents(Object... args) {
      Component[] encodedArgs = new Component[args.length];

      for (int i = 0; i < args.length; i++) {
         if (args[i] instanceof String && ((String)args[i]).startsWith("ic2.")) {
            encodedArgs[i] = Component.translatable((String)args[i], new Object[0]);
         } else {
            encodedArgs[i] = Component.literal(args[i].toString());
         }
      }

      return encodedArgs;
   }

   public void requestTick(boolean simulating, Runnable runnable) {
      if (!simulating) {
         throw new IllegalStateException();
      } else {
         ServerLifecycleHooks.getCurrentServer().execute(runnable);
      }
   }

   public int getColorMultiplier(BlockState state, BlockGetter world, BlockPos pos, int tint) {
      throw new UnsupportedOperationException("client only");
   }
}
