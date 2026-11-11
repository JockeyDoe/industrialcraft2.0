package ic2.core.audio;

import java.lang.ref.WeakReference;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class AudioPosition {
   private final WeakReference<Level> worldRef;
   public final float x;
   public final float y;
   public final float z;

   public static AudioPosition getFrom(Object obj, PositionSpec positionSpec) {
      if (obj instanceof AudioPosition) {
         return (AudioPosition)obj;
      } else if (obj instanceof Entity e) {
         return new AudioPosition(e.level(), (float)e.getX(), (float)e.getY(), (float)e.getZ());
      } else {
         return obj instanceof BlockEntity te
            ? new AudioPosition(te.getLevel(), te.getBlockPos().getX() + 0.5F, te.getBlockPos().getY() + 0.5F, te.getBlockPos().getZ() + 0.5F)
            : null;
      }
   }

   public AudioPosition(Level world, float x, float y, float z) {
      this.worldRef = new WeakReference<>(world);
      this.x = x;
      this.y = y;
      this.z = z;
   }

   public AudioPosition(Level world, BlockPos pos) {
      this(world, pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F);
   }

   public Level getWorld() {
      return this.worldRef.get();
   }
}
