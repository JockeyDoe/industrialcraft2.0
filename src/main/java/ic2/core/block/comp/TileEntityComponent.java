package ic2.core.block.comp;

import ic2.core.IC2;
import ic2.core.block.TileEntityBlock;
import ic2.core.network.GrowingBuffer;
import java.io.DataInput;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.BlockCapability;

public abstract class TileEntityComponent {
   protected final TileEntityBlock parent;

   public TileEntityComponent(TileEntityBlock parent) {
      this.parent = parent;
   }

   public TileEntityBlock getParent() {
      return this.parent;
   }

   public void readFromNbt(CompoundTag nbt) {
   }

   public CompoundTag writeToNbt() {
      return null;
   }

   public void onLoaded() {
   }

   public void onUnloaded() {
   }

   public void onNeighborChange(Block srcBlock, BlockPos srcPos) {
   }

   public void onContainerUpdate(ServerPlayer player) {
   }

   public void onNetworkUpdate(DataInput is) throws IOException {
   }

   public boolean enableWorldTick() {
      return false;
   }

   public void onWorldTick() {
   }

   protected void setNetworkUpdate(ServerPlayer player, GrowingBuffer data) {
      IC2.network.get(true).sendComponentUpdate(this.parent, Components.getId((Class<? extends TileEntityComponent>)this.getClass()), player, data);
   }

   public Collection<? extends BlockCapability<?, Direction>> getProvidedCapabilities(Direction side) {
      return Collections.emptySet();
   }

   public <T> T getCapability(BlockCapability<T, Direction> cap, Direction side) {
      return null;
   }
}
