package ic2.core.block;

import ic2.core.block.state.IIdProvider;
import ic2.core.item.block.ItemBlockTileEntity;
import ic2.core.ref.BlockMaterial;
import ic2.core.ref.TeBlock;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public interface ITeBlock extends IIdProvider {
   Identifier getIdentifier();

   boolean hasItem();

   @Nullable
   Class<? extends TileEntityBlock> getTeClass();

   boolean hasActive();

   Set<Direction> getSupportedFacings();

   float getHardness();

   float getExplosionResistance();

   TeBlock.HarvestTool getHarvestTool();

   TeBlock.DefaultDrop getDefaultDrop();

   Rarity getRarity();

   boolean allowWrenchRotating();

   default BlockMaterial getMaterial() {
      return TeBlockRegistry.getInfo(this.getIdentifier()).getDefaultMaterial();
   }

   default boolean isTransparent() {
      return false;
   }

   default void setPlaceHandler(TeBlock.ITePlaceHandler handler) {
      throw new UnsupportedOperationException();
   }

   @Nullable
   default TeBlock.ITePlaceHandler getPlaceHandler() {
      return null;
   }

   @Nullable
   @Deprecated
   TileEntityBlock getDummyTe();

   public interface ITeBlockCreativeRegisterer {
      void addSubBlocks(NonNullList<ItemStack> var1, BlockTileEntity var2, ItemBlockTileEntity var3, CreativeModeTab var4);
   }
}
