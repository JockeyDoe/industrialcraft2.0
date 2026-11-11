package ic2.core.block.generator.tileentity;

import ic2.core.ContainerBase;
import ic2.core.IC2;
import ic2.core.IHasGui;
import ic2.core.audio.AudioSource;
import ic2.core.audio.PositionSpec;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.comp.Energy;
import ic2.core.block.invslot.InvSlotCharge;
import ic2.core.gui.dynamic.DynamicContainer;
import ic2.core.gui.dynamic.DynamicGui;
import ic2.core.gui.dynamic.GuiParser;
import ic2.core.network.GuiSynced;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public abstract class TileEntityBaseGenerator extends TileEntityInventory implements IHasGui {
   public final InvSlotCharge chargeSlot;
   protected final Energy energy;
   @GuiSynced
   public int fuel = 0;
   protected double production;
   private int ticksSinceLastActiveUpdate;
   private int activityMeter = 0;
   public AudioSource audioSource;

   public TileEntityBaseGenerator(double production, int tier, int maxStorage) {
      this.production = production;
      this.ticksSinceLastActiveUpdate = IC2.random.nextInt(256);
      this.chargeSlot = new InvSlotCharge(this, 1);
      this.energy = this.addComponent(Energy.asBasicSource(this, maxStorage, tier).addManagedSlot(this.chargeSlot));
   }

   @Override
   public void loadAdditional(CompoundTag nbttagcompound) {
      super.loadAdditional(nbttagcompound);
      this.fuel = nbttagcompound.getIntOr("fuel", 0);
   }

   @Override
   public CompoundTag saveAdditional(CompoundTag nbt) {
      super.saveAdditional(nbt);
      nbt.putInt("fuel", this.fuel);
      return nbt;
   }

   @Override
   protected void onUnloaded() {
      if (IC2.platform.isRendering() && this.audioSource != null) {
         IC2.audioManager.removeSources(this);
         this.audioSource = null;
      }

      super.onUnloaded();
   }

   @Override
   protected void updateEntityServer() {
      super.updateEntityServer();
      boolean needsInvUpdate = false;
      if (this.needsFuel()) {
         needsInvUpdate = this.gainFuel();
      }

      boolean newActive = this.gainEnergy();
      if (needsInvUpdate) {
         this.setChanged();
      }

      if (!this.delayActiveUpdate()) {
         this.setActive(newActive);
      } else {
         if (this.ticksSinceLastActiveUpdate % 256 == 0) {
            this.setActive(this.activityMeter > 0);
            this.activityMeter = 0;
         }

         if (newActive) {
            this.activityMeter++;
         } else {
            this.activityMeter--;
         }

         this.ticksSinceLastActiveUpdate++;
      }
   }

   public boolean gainEnergy() {
      if (this.isConverting()) {
         this.energy.addEnergy(this.production);
         this.fuel--;
         return true;
      } else {
         return false;
      }
   }

   public boolean isConverting() {
      return !this.needsFuel() && this.energy.getFreeEnergy() >= this.production;
   }

   public boolean needsFuel() {
      return this.fuel <= 0 && this.energy.getFreeEnergy() >= this.production;
   }

   public abstract boolean gainFuel();

   public String getOperationSoundFile() {
      return null;
   }

   protected boolean delayActiveUpdate() {
      return false;
   }

   @Override
   public void onGuiClosed(Player player) {
   }

   @Override
   public ContainerBase<? extends TileEntityBaseGenerator> getGuiContainer(Player player) {
      return DynamicContainer.create(this, player, GuiParser.parse(this.teBlock));
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Screen getGui(Player player, boolean isAdmin) {
      return DynamicGui.<TileEntityBaseGenerator>create(this, player, GuiParser.parse(this.teBlock));
   }

   @Override
   public void onNetworkUpdate(String field) {
      if (field.equals("active")) {
         if (this.audioSource == null && this.getOperationSoundFile() != null) {
            this.audioSource = IC2.audioManager
               .createSource(this, PositionSpec.Center, this.getOperationSoundFile(), true, false, IC2.audioManager.getDefaultVolume());
         }

         if (this.getActive()) {
            if (this.audioSource != null) {
               this.audioSource.play();
            }
         } else if (this.audioSource != null) {
            this.audioSource.stop();
         }
      }

      super.onNetworkUpdate(field);
   }
}
