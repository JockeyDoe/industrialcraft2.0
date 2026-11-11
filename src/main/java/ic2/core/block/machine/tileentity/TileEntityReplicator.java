package ic2.core.block.machine.tileentity;

import ic2.api.network.INetworkClientTileEntityEventListener;
import ic2.api.recipe.IPatternStorage;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.ContainerBase;
import ic2.core.IC2;
import ic2.core.IHasGui;
import ic2.core.block.comp.Fluids;
import ic2.core.block.comp.Ic2FluidTank;
import ic2.core.block.invslot.InvSlotConsumableLiquid;
import ic2.core.block.invslot.InvSlotConsumableLiquidByList;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.block.machine.container.ContainerReplicator;
import ic2.core.block.machine.gui.GuiReplicator;
import ic2.core.network.GuiSynced;
import ic2.core.profile.NotClassic;
import ic2.core.ref.FluidName;
import ic2.core.util.ItemStacks;
import ic2.core.util.StackUtil;
import ic2.core.uu.UuIndex;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;

@NotClassic
public class TileEntityReplicator extends TileEntityElectricMachine implements IHasGui, IUpgradableBlock, INetworkClientTileEntityEventListener {
   private static final double uuPerTickBase = 1.0E-4;
   private static final double euPerTickBase = 512.0;
   private static final int defaultTier = 4;
   private static final int defaultEnergyStorage = 2000000;
   private double uuPerTick = 1.0E-4;
   private double euPerTick = 512.0;
   private double extraUuStored = 0.0;
   public double uuProcessed = 0.0;
   public ItemStack pattern;
   private TileEntityReplicator.Mode mode = TileEntityReplicator.Mode.STOPPED;
   public int index;
   public int maxIndex;
   public double patternUu;
   public double patternEu;
   public final InvSlotConsumableLiquid fluidSlot = new InvSlotConsumableLiquidByList(this, "fluid", 1, FluidName.uu_matter.getInstance());
   public final InvSlotOutput cellSlot = new InvSlotOutput(this, "cell", 1);
   public final InvSlotOutput outputSlot = new InvSlotOutput(this, "output", 1);
   public final InvSlotUpgrade upgradeSlot = new InvSlotUpgrade(this, "upgrade", 4);
   @GuiSynced
   public final Ic2FluidTank fluidTank;
   protected final Fluids fluids = this.addComponent(new Fluids(this));

   public TileEntityReplicator() {
      super(2000000, 4);
      this.fluidTank = this.fluids.addTank("fluidTank", 16000, Fluids.fluidPredicate(FluidName.uu_matter.getInstance()));
   }

   @Override
   protected void updateEntityServer() {
      super.updateEntityServer();
      boolean needsInvUpdate = false;
      if (this.fluidTank.getFluidAmount() < this.fluidTank.getCapacity()) {
         needsInvUpdate = this.gainFluid();
      }

      boolean newActive = false;
      if (this.mode != TileEntityReplicator.Mode.STOPPED
         && this.energy.getEnergy() >= this.euPerTick
         && this.pattern != null
         && this.outputSlot.canAdd(this.pattern)) {
         double uuRemaining = this.patternUu - this.uuProcessed;
         boolean finish;
         if (uuRemaining <= this.uuPerTick) {
            finish = true;
         } else {
            uuRemaining = this.uuPerTick;
            finish = false;
         }

         if (this.consumeUu(uuRemaining)) {
            newActive = true;
            this.energy.useEnergy(this.euPerTick);
            this.uuProcessed += uuRemaining;
            if (finish) {
               this.uuProcessed = 0.0;
               if (this.mode == TileEntityReplicator.Mode.SINGLE) {
                  this.mode = TileEntityReplicator.Mode.STOPPED;
               } else {
                  this.refreshInfo();
               }

               if (this.pattern != null) {
                  this.outputSlot.add(this.pattern);
                  needsInvUpdate = true;
               }
            }
         }
      }

      this.setActive(newActive);
      needsInvUpdate |= this.upgradeSlot.tickNoMark();
      if (needsInvUpdate) {
         this.setChanged();
      }
   }

   private boolean consumeUu(double amount) {
      if (amount <= this.extraUuStored) {
         this.extraUuStored -= amount;
         return true;
      } else {
         amount -= this.extraUuStored;
         int toDrain = (int)Math.ceil(amount * 1000.0);
         FluidStack drained = this.fluidTank.drainInternal(toDrain, false);
         if (drained != null && drained.getFluid() == FluidName.uu_matter.getInstance() && drained.getAmount() == toDrain) {
            this.fluidTank.drainInternal(toDrain, true);
            amount -= drained.getAmount() / 1000.0;
            if (amount < 0.0) {
               this.extraUuStored = -amount;
            } else {
               this.extraUuStored = 0.0;
            }

            return true;
         } else {
            return false;
         }
      }
   }

   public void refreshInfo() {
      IPatternStorage storage = this.getPatternStorage();
      ItemStack oldPattern = this.pattern;
      if (storage == null) {
         this.pattern = null;
      } else {
         List<ItemStack> patterns = storage.getPatterns();
         if (this.index < 0 || this.index >= patterns.size()) {
            this.index = 0;
         }

         this.maxIndex = patterns.size();
         if (patterns.isEmpty()) {
            this.pattern = null;
         } else {
            this.pattern = patterns.get(this.index);
            this.patternUu = UuIndex.instance.getInBuckets(this.pattern);
            if (!StackUtil.checkItemEqualityStrict(this.pattern, oldPattern)) {
               this.uuProcessed = 0.0;
               this.mode = TileEntityReplicator.Mode.STOPPED;
            }
         }
      }

      if (this.pattern == null) {
         this.uuProcessed = 0.0;
         this.mode = TileEntityReplicator.Mode.STOPPED;
      }
   }

   public IPatternStorage getPatternStorage() {
      Level world = this.getLevel();

      for (Direction dir : Direction.values()) {
         BlockEntity target = world.getBlockEntity(this.getBlockPos().relative(dir));
         if (target instanceof IPatternStorage) {
            return (IPatternStorage)target;
         }
      }

      return null;
   }

   public void setOverclockRates() {
      this.upgradeSlot.onChanged();
      this.uuPerTick = 1.0E-4 / this.upgradeSlot.processTimeMultiplier;
      this.euPerTick = (512.0 + this.upgradeSlot.extraEnergyDemand) * this.upgradeSlot.energyDemandMultiplier;
      this.energy.setSinkTier(applyModifier(4, this.upgradeSlot.extraTier, 1.0));
      this.energy.setCapacity(applyModifier(2000000, this.upgradeSlot.extraEnergyStorage, this.upgradeSlot.energyStorageMultiplier));
   }

   private static int applyModifier(int base, int extra, double multiplier) {
      double ret = Math.round(((double)base + extra) * multiplier);
      return ret > 2.147483647E9 ? Integer.MAX_VALUE : (int)ret;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Screen getGui(Player player, boolean isAdmin) {
      return new GuiReplicator(new ContainerReplicator(player, this));
   }

   @Override
   public ContainerBase<TileEntityReplicator> getGuiContainer(Player player) {
      return new ContainerReplicator(player, this);
   }

   @Override
   protected void onLoaded() {
      super.onLoaded();
      if (IC2.platform.isSimulating()) {
         this.setOverclockRates();
         this.refreshInfo();
      }
   }

   @Override
   public void setChanged() {
      super.setChanged();
      if (IC2.platform.isSimulating()) {
         this.setOverclockRates();
      }
   }

   public boolean gainFluid() {
      return this.fluidSlot.processIntoTank(this.fluidTank, this.cellSlot);
   }

   @Override
   public void loadAdditional(CompoundTag nbt) {
      super.loadAdditional(nbt);
      this.extraUuStored = nbt.getDoubleOr("extraUuStored", 0.0);
      this.uuProcessed = nbt.getDoubleOr("uuProcessed", 0.0);
      this.index = nbt.getIntOr("index", 0);
      int modeIdx = nbt.getIntOr("mode", 0);
      this.mode = modeIdx < TileEntityReplicator.Mode.values().length ? TileEntityReplicator.Mode.values()[modeIdx] : TileEntityReplicator.Mode.STOPPED;
      CompoundTag contentTag = nbt.getCompoundOrEmpty("pattern");
      this.pattern = ItemStacks.read(contentTag);
   }

   @Override
   public CompoundTag saveAdditional(CompoundTag nbt) {
      super.saveAdditional(nbt);
      nbt.putDouble("extraUuStored", this.extraUuStored);
      nbt.putDouble("uuProcessed", this.uuProcessed);
      nbt.putInt("index", this.index);
      nbt.putInt("mode", this.mode.ordinal());
      if (this.pattern != null) {
         CompoundTag contentTag = new CompoundTag();
         ItemStacks.write(this.pattern, contentTag);
         nbt.put("pattern", contentTag);
      }

      return nbt;
   }

   @Override
   public void onNetworkEvent(Player player, int event) {
      switch (event) {
         case 0:
         case 1:
            if (this.mode == TileEntityReplicator.Mode.STOPPED) {
               IPatternStorage storage = this.getPatternStorage();
               if (storage != null) {
                  List<ItemStack> patterns = storage.getPatterns();
                  if (!patterns.isEmpty()) {
                     if (event == 0) {
                        if (this.index <= 0) {
                           this.index = patterns.size() - 1;
                        } else {
                           this.index--;
                        }
                     } else if (this.index >= patterns.size() - 1) {
                        this.index = 0;
                     } else {
                        this.index++;
                     }

                     this.refreshInfo();
                  }
               }
            }
         case 2:
         default:
            break;
         case 3:
            if (this.mode != TileEntityReplicator.Mode.STOPPED) {
               this.uuProcessed = 0.0;
               this.mode = TileEntityReplicator.Mode.STOPPED;
            }
            break;
         case 4:
            if (this.pattern != null) {
               this.mode = TileEntityReplicator.Mode.SINGLE;
               if (player != null) {
                  IC2.achievements.issueAchievement(player, "replicateObject");
               }
            }
            break;
         case 5:
            if (this.pattern != null) {
               this.mode = TileEntityReplicator.Mode.CONTINUOUS;
               if (player != null) {
                  IC2.achievements.issueAchievement(player, "replicateObject");
               }
            }
      }
   }

   @Override
   public void onGuiClosed(Player player) {
   }

   @Override
   public double getEnergy() {
      return this.energy.getEnergy();
   }

   @Override
   public boolean useEnergy(double amount) {
      return this.energy.useEnergy(amount);
   }

   public TileEntityReplicator.Mode getMode() {
      return this.mode;
   }

   @Override
   public Set<UpgradableProperty> getUpgradableProperties() {
      return EnumSet.of(
         UpgradableProperty.Processing,
         UpgradableProperty.RedstoneSensitive,
         UpgradableProperty.Transformer,
         UpgradableProperty.EnergyStorage,
         UpgradableProperty.ItemConsuming,
         UpgradableProperty.ItemProducing,
         UpgradableProperty.FluidConsuming
      );
   }

   public static enum Mode {
      STOPPED,
      SINGLE,
      CONTINUOUS;
   }
}
