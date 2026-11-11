package ic2.core.block.machine.tileentity;

import com.google.common.collect.ImmutableSet;
import ic2.api.network.INetworkClientTileEntityEventListener;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.ChunkLoaderLogic;
import ic2.core.ContainerBase;
import ic2.core.IC2;
import ic2.core.IHasGui;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.comp.Energy;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotDischarge;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.block.machine.container.ContainerChunkLoader;
import ic2.core.block.machine.gui.GuiChunkLoader;
import ic2.core.init.MainConfig;
import ic2.core.profile.NotClassic;
import ic2.core.util.ConfigUtil;
import ic2.core.util.LogCategory;
import ic2.core.util.NbtChecks;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@NotClassic
public class TileEntityChunkloader extends TileEntityInventory implements INetworkClientTileEntityEventListener, IHasGui, IUpgradableBlock {
   private ChunkLoaderLogic.Ticket ticket;
   private final Set<ChunkPos> loadedChunks = new HashSet<>();
   public final InvSlotUpgrade upgradeSlot;
   public final InvSlotDischarge dischargeSlot;
   public final Energy energy;
   private static final int defaultTier = 1;
   private static final int defaultEnergyStorage = 2500;
   private final double euPerChunk = ConfigUtil.getFloat(MainConfig.get(), "balance/euPerChunk");

   public TileEntityChunkloader() {
      this.upgradeSlot = new InvSlotUpgrade(this, "upgrade", 4);
      this.dischargeSlot = new InvSlotDischarge(this, InvSlot.Access.IO, 1, true, InvSlot.InvSide.ANY);
      this.energy = this.addComponent(Energy.asBasicSink(this, 2500.0, 1).addManagedSlot(this.dischargeSlot));
   }

   @Override
   public void updateEntityServer() {
      super.updateEntityServer();
      boolean active = this.energy.useEnergy(this.getLoadedChunks().size() * this.euPerChunk);
      if (active != this.getActive()) {
         this.setActive(active);
      }

      this.upgradeSlot.tick();
   }

   @Override
   public void loadAdditional(CompoundTag nbt) {
      super.loadAdditional(nbt);
      ListTag list = NbtChecks.getList(nbt, "loadedChunks", 4);
      this.loadedChunks.clear();

      for (int i = 0; i < list.size(); i++) {
         LongTag currentNBT = (LongTag)list.get(i);
         long value = currentNBT.longValue();
         this.loadedChunks.add(ChunkLoaderLogic.deserialize(value));
      }
   }

   @Override
   public CompoundTag saveAdditional(CompoundTag nbt) {
      super.saveAdditional(nbt);
      ListTag list = new ListTag();
      nbt.put("loadedChunks", list);

      for (ChunkPos chunk : this.loadedChunks) {
         list.add(LongTag.valueOf(ChunkLoaderLogic.serialize(chunk)));
      }

      return nbt;
   }

   @Override
   public void setActive(boolean active) {
      Level world = this.getLevel();
      if (!world.isClientSide() && this.getActive() != active) {
         if (active) {
            if (this.ticket != null) {
               throw new IllegalStateException("Cannot activate ChunkLoader: " + this.getBlockPos() + " " + this.ticket);
            }

            this.ticket = ChunkLoaderLogic.getInstance().createTicket(world, this.getBlockPos());

            for (ChunkPos coords : this.loadedChunks) {
               ChunkLoaderLogic.getInstance().addChunkToTicket(this.ticket, coords);
            }
         } else {
            if (this.ticket == null) {
               throw new IllegalStateException("Cannot deactivate ChunkLoader: " + this.getBlockPos() + " " + this.ticket);
            }

            ChunkLoaderLogic.getInstance().removeTicket(this.ticket);
            this.ticket = null;
         }
      }

      super.setActive(active);
   }

   @Override
   public void onLoaded() {
      super.onLoaded();
      Level world = this.getLevel();
      if (!world.isClientSide()) {
         this.ticket = ChunkLoaderLogic.getInstance().getTicket(world, this.getBlockPos(), false);
         if (this.ticket != null) {
            this.loadedChunks.clear();
            this.loadedChunks.addAll(this.ticket.getChunkList());
         }

         super.setActive(this.ticket != null);
         this.setOverclockRates();
      }
   }

   @Override
   protected void onUnloaded() {
      super.onUnloaded();
      this.ticket = null;
   }

   @Override
   public void onPlaced(ItemStack stack, LivingEntity placer, Direction facing) {
      super.onPlaced(stack, placer, facing);
      this.loadedChunks.add(ChunkLoaderLogic.getChunkCoords(this.getBlockPos()));
   }

   @Override
   protected boolean onActivated(Player player, InteractionHand hand, Direction side, float hitX, float hitY, float hitZ) {
      return this.getLevel().isClientSide() ? true : IC2.platform.launchGui(player, this);
   }

   @Override
   public ContainerBase<TileEntityChunkloader> getGuiContainer(Player player) {
      return new ContainerChunkLoader(player, this);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Screen getGui(Player player, boolean isAdmin) {
      return new GuiChunkLoader(new ContainerChunkLoader(player, this));
   }

   @Override
   public void onGuiClosed(Player player) {
   }

   public void addChunkToLoaded(ChunkPos chunk) {
      if (this.getLevel().isClientSide()) {
         new RuntimeException("Something tried to change the ChunkLoaderState on the client.").printStackTrace();
      } else if (!this.isChunkInRange(chunk)) {
         IC2.log.warn(LogCategory.Block, "Trying to add a Chunk to loaded, however the chunk is too far away. Aborting.");
      } else if (this.getLoadedChunks().size() < ChunkLoaderLogic.getInstance().getMaxChunksPerTicket()) {
         if (this.ticket != null) {
            ChunkLoaderLogic.getInstance().addChunkToTicket(this.ticket, chunk);
         }

         this.loadedChunks.add(chunk);
         this.setChanged();
      }
   }

   public void removeChunkFromLoaded(ChunkPos chunk) {
      if (this.getLevel().isClientSide()) {
         new RuntimeException("Something tried to change the ChunkLoaderState on the client.").printStackTrace();
      } else if (!ChunkLoaderLogic.getChunkCoords(this.getBlockPos()).equals(chunk)) {
         if (this.ticket != null) {
            ChunkLoaderLogic.getInstance().removeChunkFromTicket(this.ticket, chunk);
         }

         this.loadedChunks.remove(chunk);
         this.setChanged();
      }
   }

   public ImmutableSet<ChunkPos> getLoadedChunks() {
      return ImmutableSet.copyOf(this.loadedChunks);
   }

   public boolean isChunkInRange(ChunkPos chunk) {
      ChunkPos mainChunk = ChunkLoaderLogic.getChunkCoords(this.getBlockPos());
      return Math.abs(chunk.x() - mainChunk.x()) <= 4 && Math.abs(chunk.z() - mainChunk.z()) <= 4;
   }

   @Override
   public void onNetworkEvent(Player player, int event) {
      int x = (event & 15) - 8;
      int z = (event >> 4 & 15) - 8;
      ChunkPos mainChunk = ChunkLoaderLogic.getChunkCoords(this.getBlockPos());
      ChunkPos chunk = new ChunkPos(mainChunk.x() + x, mainChunk.z() + z);
      if (this.isChunkInRange(chunk)) {
         if (this.getLoadedChunks().contains(chunk)) {
            this.removeChunkFromLoaded(chunk);
         } else {
            this.addChunkToLoaded(chunk);
         }
      }
   }

   @Override
   protected void onBlockBreak() {
      super.onBlockBreak();
      if (this.ticket != null) {
         ChunkLoaderLogic.getInstance().removeTicket(this.ticket);
         this.ticket = null;
      }
   }

   @Override
   public double getEnergy() {
      return this.energy.getEnergy();
   }

   @Override
   public boolean useEnergy(double amount) {
      return this.energy.useEnergy(amount);
   }

   @Override
   public Set<UpgradableProperty> getUpgradableProperties() {
      return EnumSet.of(UpgradableProperty.EnergyStorage, UpgradableProperty.ItemConsuming, UpgradableProperty.ItemProducing, UpgradableProperty.Transformer);
   }

   public void setOverclockRates() {
      this.upgradeSlot.onChanged();
      int tier = this.upgradeSlot.getTier(1);
      this.energy.setSinkTier(tier);
      this.dischargeSlot.setTier(tier);
      this.energy.setCapacity(this.upgradeSlot.getEnergyStorage(2500, 0, 0));
   }

   @Override
   public void setChanged() {
      super.setChanged();
      if (IC2.platform.isSimulating()) {
         this.setOverclockRates();
      }
   }
}
