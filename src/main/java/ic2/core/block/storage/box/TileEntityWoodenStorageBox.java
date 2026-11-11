package ic2.core.block.storage.box;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.SoundType;

public class TileEntityWoodenStorageBox extends TileEntityStorageBox {
   public TileEntityWoodenStorageBox() {
      super(27);
   }

   @Override
   protected SoundType getBlockSound(Entity entity) {
      return SoundType.WOOD;
   }
}
