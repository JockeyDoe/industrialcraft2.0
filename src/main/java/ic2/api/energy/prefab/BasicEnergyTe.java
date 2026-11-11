package ic2.api.energy.prefab;

import ic2.core.entity.EntityTagData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BasicEnergyTe<T extends BasicEnergyTile> extends BlockEntity {
   protected T energyBuffer;

   protected BasicEnergyTe(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   public T getEnergyBuffer() {
      return this.energyBuffer;
   }

   public void onLoad() {
      super.onLoad();
      this.energyBuffer.onLoad();
   }

   public void setRemoved() {
      super.setRemoved();
      this.energyBuffer.invalidate();
   }

   public void onChunkUnload() {
      this.energyBuffer.onChunkUnload();
   }

   public void loadAdditional(CompoundTag nbt) {
      this.energyBuffer.readFromNBT(nbt);
   }

   public CompoundTag saveAdditional(CompoundTag nbt) {
      return this.energyBuffer.writeToNBT(nbt);
   }

   protected void loadAdditional(ValueInput input) {
      super.loadAdditional(input);
      this.loadAdditional(EntityTagData.read(input));
   }

   protected void saveAdditional(ValueOutput output) {
      super.saveAdditional(output);
      EntityTagData.write(output, this.saveAdditional(new CompoundTag()));
   }

   public static class Sink extends BasicEnergyTe<BasicSink> {
      public Sink(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity, int tier) {
         super(type, pos, state);
         this.energyBuffer = new BasicSink(this, (double)capacity, tier);
      }
   }

   public static class Source extends BasicEnergyTe<BasicSource> {
      public Source(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity, int tier) {
         super(type, pos, state);
         this.energyBuffer = new BasicSource(this, (double)capacity, tier);
      }
   }
}
