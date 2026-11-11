package ic2.core.profile;

import com.google.common.collect.Iterables;
import ic2.api.event.ProfileEvent;
import ic2.core.IC2;
import ic2.core.init.MainConfig;
import ic2.core.init.Rezepte;
import ic2.core.util.Config;
import ic2.core.util.LogCategory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackResources;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.NameFileFilter;

public class ProfileManager {
   public static final String EXPERIMENTAL = "Experimental";
   public static final String CLASSIC = "Classic";
   public static final Map<String, Profile> PROFILES = addDefaultProfiles();
   @OnlyIn(Dist.CLIENT)
   private static List<PackResources> textureChanges;
   public static Profile selected = getOrError("Experimental");

   private static Map<String, Profile> addDefaultProfiles() {
      Map<String, Profile> ret = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
      ret.put("Experimental", new Profile("Experimental", Collections.singleton(TextureStyle.EXPERIMENTAL), Version.NEW));

      try {
         ret.put("Classic", ProfileParser.parse(ProfileTarget.fromJar("ic2/profiles/classic")));
         return ret;
      } catch (IOException var2) {
         throw new RuntimeException("Error opening profile XML", var2);
      }
   }

   public static void init() {
      Config.Value config = MainConfig.get().get("profiles/selected");
      if (config != null) {
         String active = config.value;
         File root = new File(IC2.platform.getMinecraftDir(), "ic2_profiles");
         if (root.exists()) {
            for (File file : root.listFiles()) {
               if (file.isDirectory()) {
                  for (File profile : file.listFiles(new NameFileFilter("profile.xml"))) {
                     try {
                        Profile p = ProfileParser.parse(new ProfileRoot(profile.getParentFile()));
                        if (!registerProfile(p)) {
                           IC2.log.warn(LogCategory.General, "Duplicate profile names: %s, skipping profile at %s", p.name, profile);
                        } else {
                           IC2.log.debug(LogCategory.General, "Registered profile %s at %s", p.name, profile);
                        }
                     } catch (IOException var13) {
                        throw new RuntimeException("Error opening " + profile, var13);
                     }
                  }
               } else if (IOCase.INSENSITIVE.checkEquals(FilenameUtils.getExtension(file.getName()), "zip")) {
                  try (ZipFile zip = new ZipFile(file)) {
                     ZipEntry entry = zip.getEntry("profile.xml");
                     if (entry != null) {
                        Profile p = ProfileParser.parse(new ProfileRoot(file));
                        if (!registerProfile(p)) {
                           IC2.log.warn(LogCategory.General, "Duplicate profile names: %s, skipping profile in %s", p.name, file);
                        } else {
                           IC2.log.debug(LogCategory.General, "Registered profile %s in %s", p.name, file);
                        }
                     }
                  } catch (IOException var15) {
                     IC2.log.warn(LogCategory.General, "Error opening zip at " + file, var15);
                  }
               }
            }
         }

         NeoForge.EVENT_BUS.post(new ProfileEvent.Load(Collections.unmodifiableSet(PROFILES.keySet()), active));
         Profile profile = get(active);
         if (profile != null) {
            if (selected != profile) {
               IC2.log.info(LogCategory.General, "Switching profiles from %s to %s", selected.name, active);
            }

            switchProfiles(profile);
         } else {
            IC2.log.warn(LogCategory.General, "Unknown/Invalid profile selected in the profile: %s, must be one of %s", active, PROFILES);
         }
      }
   }

   public static boolean registerProfile(Profile profile) {
      return PROFILES.putIfAbsent(profile.name, profile) == null;
   }

   public static ProfileManager.ChangeAction switchProfiles(Profile to) {
      Profile from = selected;
      if (from == to) {
         return ProfileManager.ChangeAction.Nothing;
      } else {
         NeoForge.EVENT_BUS.post(new ProfileEvent.Switch(from.name, to.name));
         applySwitch(to);
         if (from.style != to.style) {
            return ProfileManager.ChangeAction.GameReload;
         } else if (from.recipeConfigs.equals(to.recipeConfigs) && from.recipeRemovals.equals(to.recipeRemovals)) {
            return !from.textures.equals(to.textures) ? ProfileManager.ChangeAction.ResourceReload : ProfileManager.ChangeAction.Nothing;
         } else {
            return ProfileManager.ChangeAction.GameReload;
         }
      }
   }

   private static void applySwitch(Profile to) {
      selected = to;
      IC2.version = to.style;
   }

   public static Profile get(String name) {
      return PROFILES.get(name);
   }

   public static Profile getOrError(String name) {
      Profile ret = PROFILES.get(name);
      if (ret != null) {
         return ret;
      } else {
         throw new IllegalArgumentException("Cannot find profile " + name + "! Only have " + PROFILES);
      }
   }

   public static InputStream getRecipeConfig(String name) {
      List<RecipeChange> configs = selected.processRecipeConfigs(name);
      if (configs.isEmpty()) {
         return Rezepte.getDefaultConfigFile(name);
      } else {
         boolean isReplacing = configs.stream().anyMatch(change -> change.type == RecipeChange.ChangeType.REPLACEMENT);
         if (isReplacing && configs.size() == 1) {
            return ((RecipeChange)Iterables.getOnlyElement(configs)).getStream();
         } else {
            List<InputStream> streams = configs.stream().map(RecipeChange::getStream).filter(Objects::nonNull).collect(Collectors.toList());
            if (!isReplacing) {
               streams.add(0, Rezepte.getDefaultConfigFile(name));
            }

            byte[] split = new byte[]{10};

            for (int i = configs.size() - 1; i > 0; i--) {
               streams.add(i, new ByteArrayInputStream(split));
            }

            return new SequenceInputStream(Collections.enumeration(streams));
         }
      }
   }

   public static void getRecipeRemovals(String name) {
      List<Object> configs = selected.recipeRemovals.get(name);
      if (configs.isEmpty()) {
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static void doTextureChanges() {
      Minecraft.getInstance().getResourcePackRepository().reload();
   }

   public static enum ChangeAction {
      Nothing,
      ResourceReload,
      GameReload;
   }
}
