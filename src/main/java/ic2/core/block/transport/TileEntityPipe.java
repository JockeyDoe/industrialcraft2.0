package ic2.core.block.transport;

import ic2.api.transport.IPipe;
import ic2.core.IC2;
import ic2.core.block.TileEntityBlock;
import ic2.core.block.state.Ic2BlockState;
import ic2.core.block.state.UnlistedProperty;
import ic2.core.block.transport.cover.CoverProperty;
import ic2.core.block.transport.cover.Covers;
import ic2.core.block.transport.cover.ICoverHolder;
import ic2.core.block.transport.cover.ICoverItem;
import ic2.core.block.transport.items.PipeSize;
import ic2.core.block.transport.items.PipeType;
import ic2.core.util.StackUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;

public abstract class TileEntityPipe extends TileEntityBlock implements IPipe, ICoverHolder {
   public static final UnlistedProperty<TileEntityPipe.PipeRenderState> renderStateProperty = new UnlistedProperty<>(
      "renderstate", TileEntityPipe.PipeRenderState.class
   );
   protected volatile TileEntityPipe.PipeRenderState renderState;
   protected byte connectivity = 0;
   protected byte covers = 0;
   protected final Covers coversComponent = this.addComponent(new Covers(this));

   @Override
   public BlockEntity getTile() {
      return this;
   }

   @Override
   public boolean isConnected(Direction facing) {
      return (this.connectivity & 1 << facing.ordinal()) != 0;
   }

   @Override
   public abstract void flipConnection(Direction var1);

   @Override
   public Set<CoverProperty> getCoverProperties() {
      return Collections.emptySet();
   }

   @Override
   public boolean canPlaceCover(Level world, BlockPos pos, Direction side, ItemStack stack) {
      return !(stack.getItem() instanceof ICoverItem item)
         ? false
         : item.isSuitableFor(stack, this.getCoverProperties()) && (this.covers & 1 << side.ordinal()) == 0;
   }

   @Override
   public void placeCover(Level world, BlockPos pos, Direction side, ItemStack stack) {
      this.coversComponent.addCover(side, stack);
      this.covers = (byte)(this.covers ^ 1 << side.ordinal());
      IC2.network.get(true).updateTileEntityField(this, "covers");
   }

   @Override
   public boolean canRemoveCover(Level world, BlockPos pos, Direction side) {
      return (this.covers & 1 << side.ordinal()) != 0;
   }

   @Override
   public void removeCover(Level world, BlockPos pos, Direction side) {
      ItemStack ret = this.coversComponent.removeCover(side);
      this.covers = (byte)(this.covers ^ 1 << side.ordinal());
      IC2.network.get(true).updateTileEntityField(this, "covers");
      StackUtil.dropAsEntity(this.getLevel(), this.getBlockPos(), StackUtil.copyWithSize(ret, 1));
   }

   @Override
   protected void onLoaded() {
      super.onLoaded();
      if (this.getLevel().isClientSide()) {
         this.updateRenderState();
      }
   }

   @Override
   public void loadAdditional(CompoundTag nbt) {
      super.loadAdditional(nbt);
      this.connectivity = nbt.getByteOr("connectivity", (byte)0);
      this.covers = nbt.getByteOr("covers", (byte)0);
   }

   @Override
   public CompoundTag saveAdditional(CompoundTag nbt) {
      super.saveAdditional(nbt);
      nbt.putByte("connectivity", this.connectivity);
      nbt.putByte("covers", this.covers);
      return nbt;
   }

   @Override
   protected void onUnloaded() {
      super.onUnloaded();
   }

   @Override
   public void onNeighborChange(Block neighbor, BlockPos neighborPos) {
      super.onNeighborChange(neighbor, neighborPos);
   }

   @Override
   public void onPlaced(ItemStack stack, LivingEntity placer, Direction facing) {
      if (this.getLevel().isClientSide()) {
         this.updateRenderState();
      }

      super.onPlaced(stack, placer, facing);
   }

   protected abstract void updateConnectivity();

   @Override
   public Ic2BlockState.Ic2BlockStateInstance getExtendedState(Ic2BlockState.Ic2BlockStateInstance state) {
      state = super.getExtendedState(state);
      TileEntityPipe.PipeRenderState pipeRenderState = this.renderState;
      if (pipeRenderState != null) {
         state = state.withProperties(renderStateProperty, pipeRenderState);
      }

      return state;
   }

   @Override
   protected SoundType getBlockSound(Entity entity) {
      return SoundType.METAL;
   }

   @Override
   protected boolean isNormalCube() {
      return false;
   }

   @Override
   protected boolean isSideSolid(Direction side) {
      return false;
   }

   @Override
   protected boolean doesSideBlockRendering(Direction side) {
      return false;
   }

   @Override
   protected int getLightOpacity() {
      return 0;
   }

   @Override
   protected boolean clientNeedsExtraModelInfo() {
      return true;
   }

   @Override
   public void onNetworkUpdate(String field) {
      this.updateRenderState();
      this.rerender();
      super.onNetworkUpdate(field);
   }

   @Override
   public List<String> getNetworkedFields() {
      List<String> ret = super.getNetworkedFields();
      ret.add("connectivity");
      ret.add("covers");
      return ret;
   }

   @Override
   protected List<ItemStack> getAuxDrops(int fortune) {
      List<ItemStack> ret = new ArrayList<>();

      for (Direction facing : Direction.values()) {
         if (this.coversComponent.hasCover(facing)) {
            ret.add(this.coversComponent.removeCover(facing));
         }
      }

      return ret;
   }

   protected abstract void updateRenderState();

   public static class PipeRenderState {
      public final PipeType type;
      public final PipeSize size;
      public final int connectivity;
      public final int covers;
      public final int facing;

      public PipeRenderState(PipeType type, PipeSize size, int connectivity, int covers, int facing) {
         this.type = type;
         this.size = size;
         this.connectivity = connectivity;
         this.covers = covers;
         this.facing = facing;
      }

      @Override
      public int hashCode() {
         int ret = this.type.hashCode();
         ret = ret * 31 + this.size.hashCode();
         ret = ret * 31 + this.connectivity;
         ret = ret * 31 + this.covers;
         return ret * 31 + this.facing;
      }

      @Override
      public boolean equals(Object obj) {
         if (obj == this) {
            return true;
         } else {
            return !(obj instanceof TileEntityPipe.PipeRenderState o)
               ? false
               : o.type == this.type && o.size == this.size && o.connectivity == this.connectivity && o.covers == this.covers && o.facing == this.facing;
         }
      }

      @Override
      public String toString() {
         return "PipeState<" + this.type + ", " + this.size + ", " + this.connectivity + ", " + this.covers + ", " + this.facing + ">";
      }
   }
}
