package ic2.core.init;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import net.minecraft.locale.Language;

public final class Localization {
   private static final Map<String, String> english = loadEnglish();

   private Localization() {
   }

   public static void preInit(File source) {
   }

   private static Map<String, String> loadEnglish() {
      Properties values = new Properties();

      try (InputStream stream = Localization.class.getResourceAsStream("/assets/ic2/lang_ic2/en_us.properties")) {
         if (stream == null) {
            throw new IllegalStateException("Missing IC2 English translations");
         }

         values.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
      } catch (IOException var6) {
         throw new UncheckedIOException(var6);
      }

      Map<String, String> result = new HashMap<>();
      values.forEach((key, value) -> result.put(languageKey(key.toString()), value.toString()));
      return Map.copyOf(result);
   }

   public static String languageKey(String key) {
      return !key.startsWith("achievement.") && !key.startsWith("itemGroup.") && !key.startsWith("death.") ? "ic2." + key : key;
   }

   public static String canonicalKey(String key) {
      return key.startsWith("item.ic2.") ? key.substring(5) : (key.startsWith("block.ic2.") ? key.substring(6) : key);
   }

   public static String translate(String key) {
      Language language = Language.getInstance();
      if (language.has(key)) {
         return language.getOrDefault(key);
      } else {
         String canonical = canonicalKey(key);
         return language.has(canonical) ? language.getOrDefault(canonical) : english.getOrDefault(canonical, key);
      }
   }

   public static String translate(String key, Object... arguments) {
      try {
         return String.format(Locale.ROOT, translate(key), arguments);
      } catch (IllegalFormatException var3) {
         return "Format error: " + translate(key);
      }
   }
}
