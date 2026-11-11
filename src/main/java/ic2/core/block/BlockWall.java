package ic2.core.block;

import ic2.api.event.RetextureEvent;
import ic2.core.Ic2Player;
import ic2.core.item.block.ItemBlockTileEntity;
import ic2.core.ref.BlockMaterial;
import ic2.core.ref.BlockName;
import ic2.core.util.Ic2Color;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;

public class BlockWall extends BlockMultiID<Ic2Color> {
   public static final Ic2Color defaultColor = Ic2Color.light_gray;

   public static BlockWall create() {
      return BlockMultiID.create(BlockWall.class, Ic2Color.class);
   }

   private BlockWall() {
      super(BlockName.wall, BlockMaterial.ROCK);
      this.func_149711_c(3.0F);
      this.func_149752_b(30.0F);
      this.func_149672_a(SoundType.STONE);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(this.typeProperty, defaultColor));
      NeoForge.EVENT_BUS.register(this);
   }

   public boolean recolorBlock(Level world, BlockPos pos, Direction side, DyeColor color) {
      BlockState state = world.getBlockState(pos);
      Ic2Color type = this.getType(state);
      if (type == null) {
         return false;
      } else {
         Ic2Color newColor = Ic2Color.get(color);
         if (type != newColor) {
            world.setBlock(pos, (BlockState)state.setValue(this.typeProperty, newColor), 3);
            return true;
         } else {
            return false;
         }
      }
   }

   @SubscribeEvent
   public void onRetexture(RetextureEvent event) {
      if (event.state.getBlock() == this) {
         if (!(event.getLevel() instanceof Level world)) {
            return;
         }

         Ic2Color var6 = (Ic2Color)event.state.getValue(this.typeProperty);
         if (ItemBlockTileEntity.placeTeBlock(null, Ic2Player.get(world), world, event.pos, Direction.DOWN, new TileEntityWall(var6))) {
            BlockState newState = BlockName.te.getInstance().defaultBlockState();
            RetextureEvent event2 = new RetextureEvent(
               world, event.pos, newState, event.side, event.player, event.refState, event.refVariant, event.refSide, event.refColorMultipliers
            );
            NeoForge.EVENT_BUS.post(event2);
            if (event2.applied) {
               event.applied = true;
               event.setCanceled(true);
            } else {
               world.setBlock(event.pos, event.state, 3);
            }
         }
      }
   }
}
