package ic2.core.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;

public final class CommandArguments {
   public static CommandSyntaxException error(String message) {
      return new SimpleCommandExceptionType(Component.literal(message)).create();
   }

   public static boolean parseBoolean(String value) throws CommandSyntaxException {
      if (value.equals("true")) {
         return true;
      } else if (value.equals("false")) {
         return false;
      } else {
         throw error("Expected true or false: " + value);
      }
   }

   public static List<String> matching(String[] args, String[] candidates) {
      return matching(args, Arrays.asList(candidates));
   }

   public static List<String> matching(String[] args, List<String> candidates) {
      String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
      return candidates.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
   }
}
