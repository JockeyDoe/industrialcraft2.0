package ic2.core.profile;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class TextureStyle {
   public static final TextureStyle EXPERIMENTAL = new TextureStyle("ic2", ProfileTarget.fromJar("assets/ic2")) {
      @OnlyIn(Dist.CLIENT)
      @Override
      public PackResources applyChanges() {
         return null;
      }
   };
   public static final TextureStyle CLASSIC = new TextureStyle("ic2", ProfileTarget.fromJar("ic2/profiles/classic/ic2"));
   protected final ProfileTarget target;
   public final String mod;

   public TextureStyle(String mod, ProfileTarget target) {
      this.mod = mod;
      this.target = target;
   }

   @OnlyIn(Dist.CLIENT)
   public PackResources applyChanges() {
      PackLocationInfo location = new PackLocationInfo(
         "ic2-profile-" + this.mod, Component.literal("IC2 Profile Pack for " + this.mod), PackSource.BUILT_IN, Optional.empty()
      );
      return new ProfilePack(this, location);
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else {
         return !(obj instanceof TextureStyle other) ? false : this.mod.equals(other.mod) && this.target.equals(other.target);
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.mod, this.target.root, this.target.offset);
   }

   @Override
   public String toString() {
      return "TextureStyle<" + this.mod + ": " + this.target + ">";
   }
}
