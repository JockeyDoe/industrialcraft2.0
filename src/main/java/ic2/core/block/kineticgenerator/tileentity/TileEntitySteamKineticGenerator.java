package ic2.core.block.kineticgenerator.tileentity;

import ic2.api.energy.tile.IKineticSource;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.ContainerBase;
import ic2.core.ExplosionIC2;
import ic2.core.IC2;
import ic2.core.IHasGui;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.comp.Fluids;
import ic2.core.block.comp.Ic2FluidTank;
import ic2.core.block.invslot.InvSlotConsumable;
import ic2.core.block.invslot.InvSlotConsumableItemStack;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.block.kineticgenerator.container.ContainerSteamKineticGenerator;
import ic2.core.block.kineticgenerator.gui.GuiSteamKineticGenerator;
import ic2.core.block.machine.tileentity.TileEntityCondenser;
import ic2.core.init.MainConfig;
import ic2.core.item.type.CraftingItemType;
import ic2.core.profile.NotClassic;
import ic2.core.ref.FluidName;
import ic2.core.ref.ItemName;
import ic2.core.util.ConfigUtil;
import ic2.core.util.LiquidUtil;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;

@NotClassic
public class TileEntitySteamKineticGenerator extends TileEntityInventory implements IKineticSource, IHasGui, IUpgradableBlock {
   protected final Ic2FluidTank steamTank;
   protected final Ic2FluidTank distilledWaterTank;
   public final InvSlotUpgrade upgradeSlot = new InvSlotUpgrade(this, "upgrade", 1);
   public final InvSlotConsumable turbineSlot = new InvSlotConsumableItemStack(
      this, "Turbineslot", 1, ItemName.crafting.getItemStack(CraftingItemType.steam_turbine)
   );
   private static final float outputModifier = ConfigUtil.getFloat(MainConfig.get(), "balance/energy/kineticgenerator/steam");
   private int kUoutput;
   private boolean ventingSteam;
   private boolean throttled;
   private boolean isTurbineFilledWithWater = false;
   private int condensationProgress = 0;
   private int updateTicker = IC2.random.nextInt(this.getTickRate());
   protected final Fluids fluids;

   public TileEntitySteamKineticGenerator() {
      this.turbineSlot.setStackSizeLimit(1);
      this.fluids = this.addComponent(new Fluids(this));
      this.steamTank = this.fluids
         .addTankInsert("steamTank", 21000, Fluids.fluidPredicate(FluidName.steam.getInstance(), FluidName.superheated_steam.getInstance()));
      this.distilledWaterTank = this.fluids
         .addTank("distilledWaterTank", 1000, Fluids.fluidPredicate(FluidName.distilled_water.getInstance(), net.minecraft.world.level.material.Fluids.WATER));
   }

   @Override
   protected void updateEntityServer() {
      super.updateEntityServer();
      boolean needsInvUpdate = false;
      if (this.distilledWaterTank.getCapacity() - this.distilledWaterTank.getFluidAmount() >= 1 && this.isTurbineFilledWithWater) {
         this.isTurbineFilledWithWater = false;
      }

      if (this.steamTank.getFluidAmount() > 0 && !this.isTurbineFilledWithWater && !this.turbineSlot.isEmpty()) {
         if (!this.getActive()) {
            this.setActive(true);
            needsInvUpdate = true;
         }

         boolean hotSteam = this.steamTank.getFluid().getFluid() == FluidName.superheated_steam.getInstance();
         boolean turbineDoneWork = this.turbineDoWork(hotSteam);
         if (this.updateTicker++ >= this.getTickRate()) {
            if (turbineDoneWork) {
               this.turbineSlot.damage(hotSteam ? 1 : 2, false);
            }

            this.updateTicker = 0;
         }
      } else if (this.getActive()) {
         this.setActive(false);
         needsInvUpdate = true;
         this.kUoutput = 0;
      }

      needsInvUpdate |= this.upgradeSlot.tickNoMark();
      if (needsInvUpdate) {
         super.setChanged();
      }
   }

   private int handleSteam(boolean hotSteam) {
      int amount = this.steamTank.getFluidAmount();

      assert amount > 0;

      this.steamTank.drainInternal(amount, true);
      int KUWorkbuffer = amount * 2 * (hotSteam ? 2 : 1);
      if (hotSteam) {
         this.outputSteam(amount, true);
      } else {
         int condensation = amount / 10;
         this.condensationProgress += condensation;
         this.outputSteam(amount - condensation, false);
      }

      return KUWorkbuffer;
   }

   private boolean turbineDoWork(boolean hotSteam) {
      int rawOutput = this.handleSteam(hotSteam);
      int waterAmount = this.distilledWaterTank.getFluidAmount();
      float throttle;
      if (waterAmount == 0) {
         this.throttled = false;
         throttle = 1.0F;
      } else {
         this.throttled = true;
         throttle = 1.0F - (float)waterAmount / this.distilledWaterTank.getCapacity();
      }

      this.kUoutput = (int)(rawOutput * throttle * outputModifier);
      if (this.condensationProgress >= 100) {
         if (this.distilledWaterTank.fillInternal(new FluidStack(FluidName.distilled_water.getInstance(), 1), false) == 1) {
            this.condensationProgress -= 100;
            this.distilledWaterTank.fillInternal(new FluidStack(FluidName.distilled_water.getInstance(), 1), true);
         } else {
            this.isTurbineFilledWithWater = true;
         }
      }

      return this.kUoutput > 0;
   }

   private void outputSteam(int amount, boolean hotSteam) {
      Level world = this.getLevel();

      for (Direction dir : Direction.values()) {
         BlockEntity te = world.getBlockEntity(this.getBlockPos().relative(dir));
         if (te instanceof TileEntityCondenser || hotSteam && te instanceof TileEntitySteamKineticGenerator) {
            int transAmount = LiquidUtil.fillTile(te, dir.getOpposite(), new FluidStack(FluidName.steam.getInstance(), amount), false);
            if (transAmount > 0) {
               amount -= transAmount;
               if (amount <= 0) {
                  break;
               }
            }
         }
      }

      if (amount > 0) {
         this.ventingSteam = true;
         if (world.getRandom().nextInt(10) == 0) {
            new ExplosionIC2(world, null, this.getBlockPos(), 1, 1.0F, ExplosionIC2.Type.Heat).doExplosion();
         }
      } else {
         this.ventingSteam = false;
      }
   }

   public int getKUoutput() {
      return this.kUoutput;
   }

   @Override
   public void loadAdditional(CompoundTag nbt) {
      super.loadAdditional(nbt);
      this.condensationProgress = nbt.getIntOr("condensationprogress", 0);
   }

   @Override
   public CompoundTag saveAdditional(CompoundTag nbt) {
      super.saveAdditional(nbt);
      nbt.putInt("condensationprogress", this.condensationProgress);
      return nbt;
   }

   @Override
   public ContainerBase<TileEntitySteamKineticGenerator> getGuiContainer(Player player) {
      return new ContainerSteamKineticGenerator(player, this);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Screen getGui(Player player, boolean isAdmin) {
      return new GuiSteamKineticGenerator(new ContainerSteamKineticGenerator(player, this));
   }

   @Override
   public int maxrequestkineticenergyTick(Direction directionFrom) {
      return this.getConnectionBandwidth(directionFrom);
   }

   @Override
   public int getConnectionBandwidth(Direction side) {
      return side == this.getFacing() ? this.kUoutput : 0;
   }

   @Override
   public int requestkineticenergy(Direction directionFrom, int requestkineticenergy) {
      return this.drawKineticEnergy(directionFrom, requestkineticenergy, false);
   }

   @Override
   public int drawKineticEnergy(Direction side, int request, boolean simulate) {
      return side == this.getFacing() ? this.kUoutput : 0;
   }

   @Override
   public void onGuiClosed(Player player) {
   }

   public int gaugeLiquidScaled(int i, int tank) {
      return tank == 0 && this.distilledWaterTank.getFluidAmount() > 0
         ? this.distilledWaterTank.getFluidAmount() * i / this.distilledWaterTank.getCapacity()
         : 0;
   }

   @Override
   public double getEnergy() {
      return 0.0;
   }

   @Override
   public boolean useEnergy(double amount) {
      return false;
   }

   public int getDistilledWaterTankFill() {
      return this.distilledWaterTank.getFluidAmount();
   }

   public Ic2FluidTank getDistilledWaterTank() {
      return this.distilledWaterTank;
   }

   public boolean hasTurbine() {
      return !this.turbineSlot.isEmpty();
   }

   public boolean isVentingSteam() {
      return this.ventingSteam;
   }

   public boolean isThrottled() {
      return this.throttled;
   }

   public boolean isTurbineBlockedByWater() {
      return this.isTurbineFilledWithWater;
   }

   public int getTickRate() {
      return 20;
   }

   @Override
   public Set<UpgradableProperty> getUpgradableProperties() {
      return EnumSet.of(UpgradableProperty.ItemConsuming, UpgradableProperty.FluidConsuming, UpgradableProperty.FluidProducing);
   }
}
