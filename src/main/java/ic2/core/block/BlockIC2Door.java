package ic2.core.block;

import ic2.core.IC2;
import ic2.core.init.BlocksItems;
import ic2.core.item.block.ItemIC2Door;
import ic2.core.ref.BlockName;
import ic2.core.ref.IBlockModelProvider;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class BlockIC2Door extends DoorBlock implements IBlockModelProvider {
   public BlockIC2Door() {
      super(
         BlockSetType.IRON,
         Properties.of()
            .strength(50.0F, 150.0F)
            .sound(SoundType.METAL)
            .noOcclusion()
            .setId(ResourceKey.create(Registries.BLOCK, IC2.getIdentifier("reinforced_door")))
            .overrideDescription("ic2.reinforced_door")
      );
      Identifier name = IC2.getIdentifier(BlockName.reinforced_door.name());
      BlocksItems.registerBlock(this, name);
      BlocksItems.registerItem(new ItemIC2Door(this), name);
      BlockName.reinforced_door.setInstance(this);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void registerModels(BlockName name) {
      BlockBase.registerDefaultVanillaItemModel(this, null);
   }

   public boolean func_176198_a(Level world, BlockPos pos, Direction side) {
      return side != Direction.UP ? false : this.defaultBlockState().canSurvive(world, pos);
   }

   public Item func_180660_a(BlockState state, Random rand, int fortune) {
      return state.getValue(HALF) == DoubleBlockHalf.UPPER ? null : Item.byBlock(this);
   }

   public ItemStack func_185473_a(Level world, BlockPos pos, BlockState state) {
      return new ItemStack(Item.byBlock(this));
   }
}
