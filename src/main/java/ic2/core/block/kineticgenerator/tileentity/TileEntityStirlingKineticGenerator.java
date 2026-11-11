package ic2.core.block.kineticgenerator.tileentity;

import ic2.api.energy.tile.IHeatSource;
import ic2.api.energy.tile.IKineticSource;
import ic2.api.recipe.ILiquidHeatExchangerManager;
import ic2.api.recipe.Recipes;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.ContainerBase;
import ic2.core.IHasGui;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.comp.Fluids;
import ic2.core.block.comp.Ic2FluidTank;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumableLiquid;
import ic2.core.block.invslot.InvSlotConsumableLiquidByManager;
import ic2.core.block.invslot.InvSlotConsumableLiquidByTank;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.block.kineticgenerator.container.ContainerStirlingKineticGenerator;
import ic2.core.block.kineticgenerator.gui.GuiStirlingKineticGenerator;
import ic2.core.profile.NotClassic;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;

@NotClassic
public class TileEntityStirlingKineticGenerator extends TileEntityInventory implements IKineticSource, IUpgradableBlock, IHasGui {
   public Ic2FluidTank inputTank;
   public Ic2FluidTank outputTank;
   public InvSlotOutput hotoutputSlot;
   public InvSlotOutput cooloutputSlot;
   public InvSlotConsumableLiquidByTank hotfluidinputSlot;
   public InvSlotConsumableLiquidByManager coolfluidinputSlot;
   public InvSlotUpgrade upgradeSlot;
   private int heatbuffer = 0;
   private final int maxHeatbuffer;
   private int kUBuffer;
   private final int maxkUBuffer;
   private boolean newActive;
   private int liquidHeatStored;
   protected final Fluids fluids = this.addComponent(new Fluids(this));
   private static final int PARTS_KU = 3;
   private static final int PARTS_LIQUID = 1;
   private static final int PARTS_TOTAL = 4;

   public TileEntityStirlingKineticGenerator() {
      this.inputTank = this.fluids.addTankInsert("inputTank", 2000, Fluids.fluidPredicate(Recipes.liquidHeatupManager.getSingleDirectionLiquidManager()));
      this.outputTank = this.fluids.addTankExtract("outputTank", 2000);
      this.hotoutputSlot = new InvSlotOutput(this, "hotOutputSlot", 1);
      this.cooloutputSlot = new InvSlotOutput(this, "outputSlot", 1);
      this.coolfluidinputSlot = new InvSlotConsumableLiquidByManager(
         this,
         "coolfluidinputSlot",
         InvSlot.Access.I,
         1,
         InvSlot.InvSide.TOP,
         InvSlotConsumableLiquid.OpType.Drain,
         Recipes.liquidHeatupManager.getSingleDirectionLiquidManager()
      );
      this.hotfluidinputSlot = new InvSlotConsumableLiquidByTank(
         this, "hotfluidoutputSlot", InvSlot.Access.I, 1, InvSlot.InvSide.BOTTOM, InvSlotConsumableLiquid.OpType.Fill, this.outputTank
      );
      this.upgradeSlot = new InvSlotUpgrade(this, "upgrade", 3);
      this.maxHeatbuffer = 1000;
      this.maxkUBuffer = 2000;
   }

   @Override
   public void loadAdditional(CompoundTag nbt) {
      super.loadAdditional(nbt);
      this.inputTank.readFromNBT(nbt.getCompoundOrEmpty("inputTank"));
      this.outputTank.readFromNBT(nbt.getCompoundOrEmpty("outputTank"));
      this.heatbuffer = nbt.getIntOr("heatbuffer", 0);
      this.kUBuffer = nbt.getIntOr("kubuffer", 0);
      this.liquidHeatStored = nbt.getIntOr("liquidHeatStored", 0);
   }

   @Override
   public CompoundTag saveAdditional(CompoundTag nbt) {
      super.saveAdditional(nbt);
      CompoundTag inputTankTag = new CompoundTag();
      this.inputTank.writeToNBT(inputTankTag);
      nbt.put("inputTank", inputTankTag);
      CompoundTag outputTankTag = new CompoundTag();
      this.outputTank.writeToNBT(outputTankTag);
      nbt.put("outputTank", outputTankTag);
      nbt.putInt("heatbuffer", this.heatbuffer);
      nbt.putInt("kUBuffer", this.kUBuffer);
      nbt.putInt("liquidHeatStored", this.liquidHeatStored);
      return nbt;
   }

   @Override
   protected void updateEntityServer() {
      super.updateEntityServer();
      this.coolfluidinputSlot.processIntoTank(this.inputTank, this.cooloutputSlot);
      this.hotfluidinputSlot.processFromTank(this.outputTank, this.hotoutputSlot);
      if (this.heatbuffer < this.maxHeatbuffer) {
         this.heatbuffer = this.heatbuffer + this.drawHu(this.maxHeatbuffer - this.heatbuffer);
      }

      this.newActive = false;
      if (this.inputTank.getFluidAmount() > 0
         && this.outputTank.getFluidAmount() < this.outputTank.getCapacity()
         && Recipes.liquidHeatupManager.getSingleDirectionLiquidManager().acceptsFluid(this.inputTank.getFluid().getFluid())
         && this.kUBuffer < this.maxkUBuffer) {
         ILiquidHeatExchangerManager.HeatExchangeProperty property = Recipes.liquidHeatupManager.getHeatExchangeProperty(this.inputTank.getFluid().getFluid());
         if (this.outputTank.getFluid() == null || FluidStack.isSameFluidSameComponents(new FluidStack(property.outputFluid, 0), this.outputTank.getFluid())) {
            int heatbufferToUse = this.heatbuffer / 4;
            heatbufferToUse = Math.min(
               heatbufferToUse,
               (
                     Math.min(this.outputTank.getCapacity() - this.outputTank.getFluidAmount(), this.inputTank.getFluidAmount()) * property.huPerMB
                        - this.liquidHeatStored
                  )
                  / 1
            );
            heatbufferToUse = Math.min(heatbufferToUse, (this.maxkUBuffer - this.kUBuffer) / 3);
            if (heatbufferToUse > 0) {
               this.kUBuffer += heatbufferToUse * 3 * 4;
               this.liquidHeatStored += heatbufferToUse * 1;
               this.heatbuffer -= heatbufferToUse * 4;
               this.newActive = true;
            }

            if (this.liquidHeatStored >= property.huPerMB) {
               int mbToConvert = this.liquidHeatStored / property.huPerMB;
               mbToConvert = this.inputTank.drainInternal(mbToConvert, false).getAmount();
               mbToConvert = this.outputTank.fillInternal(new FluidStack(property.outputFluid, mbToConvert), false);
               this.liquidHeatStored = this.liquidHeatStored - mbToConvert * property.huPerMB;
               this.inputTank.drainInternal(mbToConvert, true);
               this.outputTank.fillInternal(new FluidStack(property.outputFluid, mbToConvert), true);
            }
         }
      }

      if (this.getActive() != this.newActive) {
         this.setActive(this.newActive);
      }

      this.upgradeSlot.tick();
   }

   private int drawHu(int amount) {
      if (amount <= 0) {
         return 0;
      } else {
         Level world = this.getLevel();
         int tmpAmount = amount;

         for (Direction dir : Direction.values()) {
            if (dir != this.getFacing() && world.getBlockEntity(this.getBlockPos().relative(dir)) instanceof IHeatSource hs) {
               int request = hs.drawHeat(dir.getOpposite(), tmpAmount, true);
               if (request > 0) {
                  tmpAmount -= hs.drawHeat(dir.getOpposite(), request, false);
                  if (tmpAmount <= 0) {
                     break;
                  }
               }
            }
         }

         return amount - tmpAmount;
      }
   }

   @Override
   public int maxrequestkineticenergyTick(Direction directionFrom) {
      return Math.min(this.kUBuffer, this.getConnectionBandwidth(directionFrom));
   }

   @Override
   public int getConnectionBandwidth(Direction side) {
      return side != this.getFacing() ? 0 : this.maxkUBuffer;
   }

   @Override
   public int requestkineticenergy(Direction directionFrom, int requestkineticenergy) {
      return this.drawKineticEnergy(directionFrom, requestkineticenergy, false);
   }

   @Override
   public int drawKineticEnergy(Direction side, int request, boolean simulate) {
      if (side != this.getFacing()) {
         return 0;
      } else {
         if (request > this.kUBuffer) {
            request = this.kUBuffer;
         }

         if (!simulate) {
            this.kUBuffer -= request;
         }

         return request;
      }
   }

   @Override
   public Set<UpgradableProperty> getUpgradableProperties() {
      return EnumSet.of(
         UpgradableProperty.ItemConsuming, UpgradableProperty.ItemProducing, UpgradableProperty.FluidConsuming, UpgradableProperty.FluidProducing
      );
   }

   @Override
   public double getEnergy() {
      return 40.0;
   }

   @Override
   public boolean useEnergy(double amount) {
      return true;
   }

   public Ic2FluidTank getInputTank() {
      return this.inputTank;
   }

   public Ic2FluidTank getOutputTank() {
      return this.outputTank;
   }

   @Override
   public ContainerBase<?> getGuiContainer(Player player) {
      return new ContainerStirlingKineticGenerator(player, this);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Screen getGui(Player player, boolean isAdmin) {
      return new GuiStirlingKineticGenerator(new ContainerStirlingKineticGenerator(player, this));
   }

   @Override
   public void onGuiClosed(Player player) {
   }
}
