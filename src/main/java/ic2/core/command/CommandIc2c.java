package ic2.core.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class CommandIc2c {
   public String func_71517_b() {
      return "ic2c";
   }

   public String func_71518_a(CommandSourceStack icommandsender) {
      return "/ic2c (rightClick <x> <y> <z> [XN|XP|YN|YP|ZN|ZP]) | currentItem";
   }

   public List<String> func_184883_a(MinecraftServer server, CommandSourceStack sender, String[] args, BlockPos pos) {
      return args.length == 1 ? CommandArguments.matching(args, new String[]{"rightClick", "currentItem"}) : Collections.emptyList();
   }

   public void func_184881_a(MinecraftServer server, CommandSourceStack sender, String[] args) throws CommandSyntaxException {
      if (args.length == 0) {
         throw CommandArguments.error(this.func_71518_a(sender));
      } else {
         if (args.length >= 4 && args.length <= 5 && args[0].equals("rightClick")) {
            this.cmdRightClick(sender, args);
         } else if (args.length == 1 && args[0].equals("currentItem")) {
            CommandIc2.cmdCurrentItem(sender);
         } else {
            CommandIc2.msg(sender, "Unknown Command.");
         }
      }
   }

   private void cmdRightClick(CommandSourceStack sender, String[] args) throws CommandSyntaxException {
      BlockPos pos = new BlockPos(Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
      CommandIc2c.ClickSide side;
      if (args.length == 5) {
         try {
            side = CommandIc2c.ClickSide.valueOf(args[4]);
         } catch (IllegalArgumentException var7) {
            throw CommandArguments.error("Invalid side: " + args[4]);
         }
      } else {
         side = CommandIc2c.ClickSide.YP;
      }

      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), side.facing, pos, false));
      CommandIc2.msg(sender, "Right click executed.");
   }

   private static enum ClickSide {
      XN(Direction.WEST),
      XP(Direction.EAST),
      YN(Direction.DOWN),
      YP(Direction.UP),
      ZN(Direction.NORTH),
      ZP(Direction.SOUTH);

      final Direction facing;

      private ClickSide(Direction facing) {
         this.facing = facing;
      }
   }
}
