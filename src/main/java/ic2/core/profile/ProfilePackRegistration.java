package ic2.core.profile;

import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackMetadataResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.Pack.Metadata;
import net.minecraft.server.packs.repository.Pack.Position;
import net.minecraft.server.packs.repository.Pack.ResourcesSupplier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;

@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT)
public final class ProfilePackRegistration {
   @SubscribeEvent
   public static void add(AddPackFindersEvent event) {
      if (event.getPackType() == PackType.CLIENT_RESOURCES) {
         event.addRepositorySource(
            output -> {
               int index = 0;

               for (final TextureStyle style : ProfileManager.selected.textures) {
                  if (style != TextureStyle.EXPERIMENTAL) {
                     PackLocationInfo location = new PackLocationInfo(
                        "ic2-profile-" + index++, Component.literal("IC2 Profile Pack for " + style.mod), PackSource.BUILT_IN, Optional.empty()
                     );
                     var supplier = new ResourcesSupplier() {
                        public PackMetadataResources openMetadata(PackLocationInfo info) {
                           return new ProfilePack(style, info);
                        }

                        public Stream<PackResources> openResources(PackLocationInfo info, Metadata metadata) {
                           return Stream.of(new ProfilePack(style, info));
                        }
                     };
                     Pack pack = Pack.readMetaAndCreate(location, supplier, PackType.CLIENT_RESOURCES, new PackSelectionConfig(true, Position.TOP, true));
                     if (pack != null) {
                        output.accept(pack);
                     }
                  }
               }
            }
         );
      }
   }
}
