package ic2.core.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SingleQuadParticle.Layer;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

public class EntityIC2FX extends SingleQuadParticle {
   public EntityIC2FX(Level world, double x, double y, double z, int maxAge, double[] velocity, float[] colour) {
      super(
         (ClientLevel)world,
         x,
         y,
         z,
         velocity[0],
         velocity[1],
         velocity[2],
         Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.PARTICLES).getSprite(Identifier.withDefaultNamespace("generic_0"))
      );
      this.rCol = colour[0];
      this.gCol = colour[1];
      this.bCol = colour[2];
      this.gravity = 0.0F;
      this.alpha = 0.6F;
      this.lifetime = maxAge;
      this.setSize(0.02F, 0.02F);
      this.quadSize = this.quadSize * (this.random.nextFloat() * 0.6F + 0.5F);
   }

   protected Layer getLayer() {
      return Layer.TRANSLUCENT;
   }

   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      this.move(0.0, 0.02F, 0.0);
      if (this.age++ >= this.lifetime) {
         this.remove();
      }
   }

   public void func_189213_a() {
      this.tick();
   }
}
