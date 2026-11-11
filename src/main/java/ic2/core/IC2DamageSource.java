package ic2.core;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;

public class IC2DamageSource extends DamageSource {
   public static final IC2DamageSource electricity = new IC2DamageSource("electricity");
   public static final IC2DamageSource nuke = new IC2DamageSource("nuke");
   public static final IC2DamageSource radiation = new IC2DamageSource("radiation");
   public final ResourceKey<DamageType> key;

   public IC2DamageSource(String name) {
      super(Holder.direct(new DamageType(name, name.equals("nuke") ? DamageScaling.ALWAYS : DamageScaling.NEVER, 0.1F)));
      this.key = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("ic2", name.equals("cropEating") ? "crop_eating" : name));
   }

   public DamageSource in(Level world) {
      return new DamageSource(world.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(this.key));
   }

   public boolean matches(DamageSource source) {
      return source == this || source.is(this.key);
   }

   public static DamageSource getNukeSource(Explosion explosion) {
      if (explosion == null) {
         throw new IllegalArgumentException("A nuclear explosion needs a level");
      } else {
         ResourceKey<DamageType> key = ResourceKey.create(
            Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("ic2", explosion.getIndirectSourceEntity() == null ? "nuke" : "nuke_player")
         );
         return new DamageSource(
            explosion.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(key),
            explosion.getDirectSourceEntity(),
            explosion.getIndirectSourceEntity()
         );
      }
   }
}
