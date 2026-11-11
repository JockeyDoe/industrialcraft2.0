package ic2.core.block;

import ic2.core.IC2;
import ic2.core.block.comp.Obscuration;
import ic2.core.block.state.Ic2BlockState;
import ic2.core.block.state.UnlistedProperty;
import ic2.core.ref.BlockName;
import ic2.core.util.Ic2Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;

public class TileEntityWall extends TileEntityBlock {
   public static final UnlistedProperty<TileEntityWall.WallRenderState> renderStateProperty = new UnlistedProperty<>(
      "renderstate", TileEntityWall.WallRenderState.class
   );
   protected final Obscuration obscuration;
   private Ic2Color color = BlockWall.defaultColor;
   private volatile TileEntityWall.WallRenderState renderState;

   public TileEntityWall() {
      this(BlockWall.defaultColor);
   }

   public TileEntityWall(Ic2Color color) {
      this.obscuration = this.addComponent(new Obscuration(this, new Runnable() {
         {
            Objects.requireNonNull(TileEntityWall.this);
         }

         @Override
         public void run() {
            IC2.network.get(true).updateTileEntityField(TileEntityWall.this, "obscuration");
         }
      }));
      this.color = color;
   }

   @Override
   public void loadAdditional(CompoundTag nbt) {
      super.loadAdditional(nbt);
      this.color = Ic2Color.values[nbt.getByteOr("color", (byte)0) & 255];
   }

   @Override
   public CompoundTag saveAdditional(CompoundTag nbt) {
      super.saveAdditional(nbt);
      nbt.putByte("color", (byte)this.color.ordinal());
      return nbt;
   }

   @Override
   protected void onLoaded() {
      super.onLoaded();
      if (this.getLevel().isClientSide()) {
         this.updateRenderState();
      }
   }

   @Override
   protected Ic2BlockState.Ic2BlockStateInstance getExtendedState(Ic2BlockState.Ic2BlockStateInstance state) {
      state = super.getExtendedState(state);
      TileEntityWall.WallRenderState value = this.renderState;
      if (value != null) {
         state = state.withProperties(renderStateProperty, value);
      }

      return state;
   }

   @Override
   public List<String> getNetworkedFields() {
      List<String> ret = new ArrayList<>();
      ret.add("color");
      ret.add("obscuration");
      ret.addAll(super.getNetworkedFields());
      return ret;
   }

   @Override
   public void onNetworkUpdate(String field) {
      super.onNetworkUpdate(field);
      if (this.updateRenderState()) {
         this.rerender();
      }
   }

   @Override
   protected boolean recolor(Direction side, DyeColor mcColor) {
      Ic2Color color = Ic2Color.get(mcColor);
      if (color == this.color) {
         return false;
      } else {
         this.color = color;
         if (!this.getLevel().isClientSide()) {
            IC2.network.get(true).updateTileEntityField(this, "obscuration");
            this.setChanged();
         } else if (this.updateRenderState()) {
            this.rerender();
         }

         return true;
      }
   }

   @Override
   protected ItemStack getPickBlock(Player player, HitResult target) {
      return BlockName.wall.getItemStack(this.color);
   }

   @Override
   protected boolean clientNeedsExtraModelInfo() {
      return this.obscuration.hasObscuration();
   }

   private boolean updateRenderState() {
      TileEntityWall.WallRenderState state = new TileEntityWall.WallRenderState(this.color, this.obscuration.getRenderState());
      if (state.equals(this.renderState)) {
         return false;
      } else {
         this.renderState = state;
         return true;
      }
   }

   public static class WallRenderState {
      public final Ic2Color color;
      public final Obscuration.ObscurationData[] obscurations;

      public WallRenderState(Ic2Color color, Obscuration.ObscurationData[] obscurations) {
         this.color = color;
         this.obscurations = obscurations;
      }

      @Override
      public boolean equals(Object obj) {
         if (obj == this) {
            return true;
         } else {
            return !(obj instanceof TileEntityWall.WallRenderState o)
               ? false
               : o.color == this.color && Arrays.equals((Object[])o.obscurations, (Object[])this.obscurations);
         }
      }

      @Override
      public int hashCode() {
         return this.color.hashCode() * 31 + Arrays.hashCode((Object[])this.obscurations);
      }

      @Override
      public String toString() {
         return "WallState<" + this.color + ", " + Arrays.toString((Object[])this.obscurations) + ">";
      }
   }
}
