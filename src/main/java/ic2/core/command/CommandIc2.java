package ic2.core.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import ic2.api.crops.CropCard;
import ic2.api.crops.Crops;
import ic2.api.recipe.IRecipeInput;
import ic2.core.IC2;
import ic2.core.IWorldTickCallback;
import ic2.core.energy.grid.EnergyNetGlobal;
import ic2.core.energy.grid.EnergyNetLocal;
import ic2.core.energy.grid.EnergyNetSettings;
import ic2.core.energy.grid.GridInfo;
import ic2.core.item.ItemCropSeed;
import ic2.core.recipe.RecipeTags;
import ic2.core.ref.IMultiBlock;
import ic2.core.ref.IMultiItem;
import ic2.core.util.ConfigUtil;
import ic2.core.util.ItemData;
import ic2.core.util.ItemStacks;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import ic2.core.uu.DropScan;
import ic2.core.uu.UuGraph;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = "ic2")
public class CommandIc2 {
   @SubscribeEvent
   public static void register(RegisterCommandsEvent event) {
      CommandIc2 command = new CommandIc2();
      event.getDispatcher()
         .register(
            (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("ic2")
                     .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
                  .executes(context -> {
                     throw CommandArguments.error(command.func_71518_a((CommandSourceStack)context.getSource()));
                  }))
               .then(
                  Commands.argument("arguments", StringArgumentType.greedyString())
                     .suggests(
                        (context, builder) -> {
                           String[] words = builder.getRemaining().split(" ", -1);
                           int start = builder.getInput().lastIndexOf(32) + 1;
                           SuggestionsBuilder last = builder.createOffset(start);
                           command.func_184883_a(
                                 ((CommandSourceStack)context.getSource()).getServer(),
                                 (CommandSourceStack)context.getSource(),
                                 words,
                                 BlockPos.containing(((CommandSourceStack)context.getSource()).getPosition())
                              )
                              .forEach(last::suggest);
                           return last.buildFuture();
                        }
                     )
                     .executes(
                        context -> {
                           command.func_184881_a(
                              ((CommandSourceStack)context.getSource()).getServer(),
                              (CommandSourceStack)context.getSource(),
                              StringArgumentType.getString(context, "arguments").split(" +")
                           );
                           return 1;
                        }
                     )
               )
         );
   }

   public String func_71517_b() {
      return "ic2";
   }

   public String func_71518_a(CommandSourceStack icommandsender) {
      return "/ic2 uu-world-scan <tiny|small|medium|large> | debug (dumpUuValues | resolveIngredient <name> | dumpTextures <name> <size> | dumpLargeGrids | enet (logIssues | logUpdates) (true|false)) | currentItem | itemNameWithVariant | giveCrop <owner> <name> <growth (1-31)> <gain (1-31)> <resistance (1-31)>";
   }

   public List<String> func_184883_a(MinecraftServer server, CommandSourceStack sender, String[] args, BlockPos pos) {
      if (args.length == 1) {
         return CommandArguments.matching(args, new String[]{"uu-world-scan", "debug", "currentItem", "itemNameWithVariant", "giveCrop"});
      } else if (args.length == 2 && args[0].equals("uu-world-scan")) {
         return CommandArguments.matching(args, new String[]{"tiny", "small", "medium", "large"});
      } else if (args.length >= 2 && args[0].equals("debug")) {
         return this.getDebugTabCompletionOptions(server, sender, args, pos);
      } else {
         return args.length == 6 && args[0].equals("giveCrop") ? Collections.emptyList() : Collections.emptyList();
      }
   }

   private List<String> getDebugTabCompletionOptions(MinecraftServer server, CommandSourceStack sender, String[] args, BlockPos pos) {
      if (args.length == 2) {
         return CommandArguments.matching(args, new String[]{"dumpUuValues", "resolveIngredient", "dumpTextures", "dumpLargeGrids", "enet"});
      } else if (args.length == 3 && args[1].equals("resolveIngredient")) {
         List<String> possibilities = new ArrayList<>(1024);

         for (Identifier loc : BuiltInRegistries.ITEM.keySet()) {
            possibilities.add(loc.toString());
         }

         for (String name : RecipeTags.getOreNames()) {
            possibilities.add("OreDict:" + name);
         }

         for (String name : BuiltInRegistries.FLUID.keySet().stream().map(Object::toString).toList()) {
            possibilities.add("Fluid:" + name);
         }

         return CommandArguments.matching(args, possibilities);
      } else {
         if (args.length >= 3 && "dumpTextures".equals(args[1])) {
            if (args.length == 3) {
               List<String> possibilities = new ArrayList<>(1024);

               for (Identifier loc : BuiltInRegistries.ITEM.keySet()) {
                  possibilities.add(loc.toString());
               }

               return CommandArguments.matching(args, possibilities);
            }

            if (args.length == 4) {
               List<String> possibilities = new ArrayList<>();

               for (short num = 512; num > 8; num = (short)(num >> 1)) {
                  possibilities.add(Integer.toString(num));
               }

               return CommandArguments.matching(args, possibilities);
            }
         } else if (args.length >= 3 && "enet".equals(args[1])) {
            if (args.length == 3) {
               List<String> possibilities = new ArrayList<>(1024);

               for (Identifier loc : BuiltInRegistries.ITEM.keySet()) {
                  possibilities.add(loc.toString());
               }

               return CommandArguments.matching(args, new String[]{"logIssues", "logUpdates"});
            }

            if (args.length == 4) {
               return CommandArguments.matching(args, new String[]{"true", "false"});
            }
         }

         return Collections.emptyList();
      }
   }

   public void func_184881_a(MinecraftServer server, CommandSourceStack sender, String[] args) throws CommandSyntaxException {
      if (args.length == 0) {
         throw CommandArguments.error(this.func_71518_a(sender));
      } else {
         if (args.length == 2 && args[0].equals("uu-world-scan")) {
            this.cmdUuWorldScan(sender, args[1]);
         } else if (args[0].equals("debug")) {
            if (args.length == 2 && args[1].equals("dumpUuValues")) {
               this.cmdDumpUuValues(sender);
            } else if (args.length == 3 && args[1].equals("resolveIngredient")) {
               this.cmdDebugResolveIngredient(sender, args[2]);
            } else if (args.length == 4 && args[1].equals("dumpTextures")) {
               this.cmdDebugDumpTextures(sender, args[2], args[3]);
            } else if (args.length == 2 && args[1].equals("dumpLargeGrids")) {
               this.dumpLargeGrids(sender);
            } else {
               if (args.length != 4 || !args[1].equals("enet")) {
                  throw CommandArguments.error(this.func_71518_a(sender));
               }

               this.cmdDebugEnet(sender, args[2], CommandArguments.parseBoolean(args[3]));
            }
         } else if (args.length == 1 && args[0].equals("currentItem")) {
            cmdCurrentItem(sender);
         } else if (args.length == 1 && args[0].equals("itemNameWithVariant") && sender.getEntity() instanceof Player) {
            Player player = (Player)sender.getEntity();
            ItemStack stack = player.getInventory().getSelectedItem();
            if (StackUtil.isEmpty(stack)) {
               msg(sender, "empty: " + StackUtil.toStringSafe(stack));
            } else if (!stack.getItem().getClass().getCanonicalName().startsWith("ic2.core")) {
               msg(sender, "Not an IC2 Item.");
            } else {
               String name = Util.getName(stack.getItem()).getPath();
               String variant = null;
               if (stack.getItem() instanceof IMultiItem) {
                  variant = ((IMultiItem)stack.getItem()).getVariant(stack);
               } else if (stack.getItem() instanceof BlockItem && ((BlockItem)stack.getItem()).getBlock() instanceof IMultiBlock) {
                  variant = ((IMultiBlock)((BlockItem)stack.getItem()).getBlock()).getVariant(stack);
               }

               msg(sender, "Name: " + name + (variant == null ? "" : " Variant: " + variant));
            }
         } else if (args.length == 6 && args[0].equals("giveCrop") && sender.getEntity() instanceof Player) {
            this.cmdGiveCrop(sender, args);
         } else {
            msg(sender, "Unknown Command.");
         }
      }
   }

   private void cmdUuWorldScan(CommandSourceStack sender, String arg) throws CommandSyntaxException {
      int areaCount;
      if (arg.equals("tiny")) {
         areaCount = 128;
      } else if (arg.equals("small")) {
         areaCount = 1024;
      } else if (arg.equals("medium")) {
         areaCount = 2048;
      } else {
         if (!arg.equals("large")) {
            throw CommandArguments.error(this.func_71518_a(sender));
         }

         areaCount = 4096;
      }

      float time = areaCount * 0.0032F;
      msg(sender, String.format("Starting world scan, this will take about %.1f minutes with a powerful cpu.", time));
      msg(sender, "The scan runs in a separate native world; progress is reported in the server log.");
      ServerLevel world = null;
      if (sender.getEntity() instanceof ServerPlayer) {
         world = ((ServerPlayer)sender.getEntity()).level();
      } else {
         world = sender.getServer().overworld();
      }

      if (world == null) {
         msg(sender, "Can't determine the world to scan.");
      } else {
         int area = 50000;
         int range = 5;
         ServerLevel scanWorld = world;
         int count = areaCount;
         Thread worker = new Thread(() -> {
            DropScan scan = null;

            try {
               scan = new DropScan(scanWorld, range);
               scan.start(area, count);
            } finally {
               if (scan != null) {
                  scan.cleanup();
               }
            }
         }, "IC2 UU world scan");
         worker.setDaemon(true);
         worker.start();
      }
   }

   private void cmdDumpUuValues(CommandSourceStack sender) {
      List<Entry<ItemStack, Double>> list = new ArrayList<>();
      Iterator<Entry<ItemStack, Double>> it = UuGraph.iterator();

      while (it.hasNext()) {
         list.add(it.next());
      }

      Collections.sort(list, new Comparator<Entry<ItemStack, Double>>() {
         {
            Objects.requireNonNull(CommandIc2.this);
         }

         public int compare(Entry<ItemStack, Double> a, Entry<ItemStack, Double> b) {
            return a.getKey().getHoverName().getString().compareTo(b.getKey().getHoverName().getString());
         }
      });
      msg(sender, "UU Values:");

      for (Entry<ItemStack, Double> entry : list) {
         msg(sender, String.format("  %s: %s", entry.getKey().getHoverName().getString(), entry.getValue()));
      }

      msg(sender, "(check console for full list)");
   }

   private void cmdDebugResolveIngredient(CommandSourceStack sender, String arg) {
      try {
         IRecipeInput input = ConfigUtil.asRecipeInput(arg);
         if (input == null) {
            msg(sender, "No match");
         } else {
            List<ItemStack> inputs = input.getInputs();
            msg(sender, inputs.size() + " matches:");

            for (ItemStack stack : inputs) {
               if (stack == null) {
                  msg(sender, " null");
               } else {
                  msg(
                     sender,
                     String.format(
                        " %s (%s, od: %s, name: %s / %s)",
                        StackUtil.toStringSafe(stack),
                        Util.getName(stack.getItem()),
                        this.getOreDictNames(stack),
                        stack.getItem().getDescriptionId(),
                        stack.getDisplayName()
                     )
                  );
               }
            }
         }
      } catch (Exception var7) {
         msg(sender, "Error: " + var7);
      }
   }

   private String getOreDictNames(ItemStack stack) {
      String ret = "";

      for (int oreId : RecipeTags.getOreIDs(stack)) {
         if (!ret.isEmpty()) {
            ret = ret + ", ";
         }

         ret = ret + RecipeTags.getOreName(oreId);
      }

      return ret.isEmpty() ? "<none>" : ret;
   }

   private void cmdDebugDumpTextures(CommandSourceStack sender, String name, String size) {
      if (!FMLEnvironment.getDist().isClient()) {
         msg(sender, "Can't dump textures on the dedicated server.");
      } else {
         msg(sender, "Dumping requested textures to sprites texture...");
         Integer meta = null;
         int pos = name.indexOf(64);
         if (pos != -1) {
            meta = Integer.valueOf(name.substring(pos + 1));
            name = name.substring(0, pos);
         }

         String regex = "^" + Pattern.quote(name).replace("*", "\\E.*\\Q") + "$";
         Pattern pattern = Pattern.compile(regex);
         IC2.tickHandler.requestSingleWorldTick(IC2.platform.getPlayerWorld(), new CommandIc2.TextureDumper(pattern, Integer.valueOf(size), meta));
      }
   }

   private void dumpLargeGrids(CommandSourceStack sender) {
      List<GridInfo> allGrids = new ArrayList<>();

      for (Level world : sender.getServer().getAllLevels()) {
         EnergyNetLocal energyNet = EnergyNetGlobal.getLocal(world);
         allGrids.addAll(energyNet.getGridInfos());
      }

      Collections.sort(allGrids, new Comparator<GridInfo>() {
         {
            Objects.requireNonNull(CommandIc2.this);
         }

         public int compare(GridInfo a, GridInfo b) {
            return b.complexNodeCount - a.complexNodeCount;
         }
      });
      msg(sender, "found " + allGrids.size() + " grids overall");

      for (int i = 0; i < 8 && i < allGrids.size(); i++) {
         GridInfo grid = allGrids.get(i);
         if (grid.nodeCount == 0) {
            msg(sender, "grid " + grid.id + " is empty");
         } else {
            msg(
               sender,
               String.format(
                  "%d complex / %d total nodes in grid %d (%d/%d/%d - %d/%d/%d)",
                  grid.complexNodeCount,
                  grid.nodeCount,
                  grid.id,
                  grid.minX,
                  grid.minY,
                  grid.minZ,
                  grid.maxX,
                  grid.maxY,
                  grid.maxZ
               )
            );
         }
      }
   }

   private void cmdDebugEnet(CommandSourceStack sender, String option, boolean value) throws CommandSyntaxException {
      if ("logIssues".equals(option)) {
         msg(sender, "setting logGridUpdateIssues to " + value);
         EnergyNetSettings.logGridUpdateIssues = value;
      } else {
         if (!"logUpdates".equals(option)) {
            throw CommandArguments.error(this.func_71518_a(sender));
         }

         msg(sender, "setting logGridUpdatesVerbose to " + value);
         EnergyNetSettings.logGridUpdatesVerbose = value;
      }
   }

   public static void msg(CommandSourceStack sender, String text) {
      sender.sendSystemMessage(Component.literal(text));
   }

   static void cmdCurrentItem(CommandSourceStack sender) {
      if (!(sender.getEntity() instanceof Player)) {
         msg(sender, "Not applicable for non-player");
      } else {
         Player player = (Player)sender.getEntity();
         ItemStack stack = player.getInventory().getSelectedItem();
         if (StackUtil.isEmpty(stack)) {
            msg(sender, "empty: " + StackUtil.toStringSafe(stack));
         } else {
            msg(
               sender,
               String.format(
                  "ID: %s, Raw Meta: %d, Meta: %d, Damage: %d, NBT: %s",
                  Util.getName(stack.getItem()),
                  StackUtil.getRawMeta(stack),
                  ItemStacks.rawMeta(stack),
                  ItemStacks.rawMeta(stack),
                  ItemData.read(stack)
               )
            );
            msg(sender, "Current Item excluding amount: " + ConfigUtil.fromStack(stack));
            msg(sender, "Current Item including amount: " + ConfigUtil.fromStackWithAmount(stack));
         }
      }
   }

   private void cmdGiveCrop(CommandSourceStack sender, String[] args) throws CommandSyntaxException {
      Player player = (Player)sender.getEntity();
      if (!StackUtil.isEmpty(player.getInventory().getSelectedItem())) {
         msg(sender, "The currently selected slot needs to be empty.");
      } else {
         CropCard crop = Crops.instance.getCropCard(args[1], args[2]);
         if (crop == null) {
            msg(sender, "The crop you specified does not exist.");
         } else {
            int growth;
            int gain;
            int resistance;
            try {
               growth = Integer.parseInt(args[3]);
               gain = Integer.parseInt(args[4]);
               resistance = Integer.parseInt(args[5]);
            } catch (NumberFormatException var9) {
               throw CommandArguments.error(this.func_71518_a(sender));
            }

            if (growth < 1 || growth > 31 || gain < 1 || gain > 31 || resistance < 1 || resistance > 31) {
               throw CommandArguments.error(this.func_71518_a(sender));
            }

            player.getInventory().add(ItemCropSeed.generateItemStackFromValues(crop, growth, gain, resistance, 4));
         }
      }
   }

   public static class TextureDumper implements IWorldTickCallback {
      private final Pattern pattern;
      private final int size;
      private final Integer meta;

      TextureDumper(Pattern pattern, int size, Integer meta) {
         this.pattern = pattern;
         this.size = size;
         this.meta = meta;
      }

      @Override
      public void onTick(Level world) {
         TextureDumpScreen.open(this.pattern, this.size, this.meta);
      }
   }
}
