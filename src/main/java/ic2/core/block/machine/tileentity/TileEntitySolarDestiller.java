package ic2.core.block.machine.tileentity;

import ic2.api.crops.BiomeCategory;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.ContainerBase;
import ic2.core.IC2;
import ic2.core.IHasGui;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.comp.Fluids;
import ic2.core.block.comp.Ic2FluidTank;
import ic2.core.block.generator.tileentity.TileEntitySolarGenerator;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumableLiquid;
import ic2.core.block.invslot.InvSlotConsumableLiquidByList;
import ic2.core.block.invslot.InvSlotConsumableLiquidByTank;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.block.machine.container.ContainerSolarDestiller;
import ic2.core.block.machine.gui.GuiSolarDestiller;
import ic2.core.profile.NotClassic;
import ic2.core.ref.FluidName;
import ic2.core.util.BiomeUtil;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;

@NotClassic
public class TileEntitySolarDestiller extends TileEntityInventory implements IHasGui, IUpgradableBlock {
   public final Ic2FluidTank inputTank;
   public final Ic2FluidTank outputTank;
   private int tickrate;
   private int updateTicker;
   private float skyLight;
   public final InvSlotOutput wateroutputSlot;
   public final InvSlotOutput destiwateroutputSlott;
   public final InvSlotConsumableLiquidByList waterinputSlot;
   public final InvSlotConsumableLiquidByTank destiwaterinputSlot;
   public final InvSlotUpgrade upgradeSlot;
   protected final Fluids fluids = this.addComponent(new Fluids(this));

   public TileEntitySolarDestiller() {
      this.inputTank = this.fluids.addTankInsert("inputTank", 10000, Fluids.fluidPredicate(net.minecraft.world.level.material.Fluids.WATER));
      this.outputTank = this.fluids.addTankExtract("outputTank", 10000);
      this.waterinputSlot = new InvSlotConsumableLiquidByList(
         this, "waterInput", InvSlot.Access.I, 1, InvSlot.InvSide.TOP, InvSlotConsumableLiquid.OpType.Drain, net.minecraft.world.level.material.Fluids.WATER
      );
      this.destiwaterinputSlot = new InvSlotConsumableLiquidByTank(
         this, "destilledWaterInput", InvSlot.Access.I, 1, InvSlot.InvSide.BOTTOM, InvSlotConsumableLiquid.OpType.Fill, this.outputTank
      );
      this.wateroutputSlot = new InvSlotOutput(this, "waterOutput", 1);
      this.destiwateroutputSlott = new InvSlotOutput(this, "destilledWaterOutput", 1);
      this.upgradeSlot = new InvSlotUpgrade(this, "upgrade", 3);
   }

   @Override
   protected void onLoaded() {
      super.onLoaded();
      this.tickrate = this.getTickRate();
      this.updateTicker = IC2.random.nextInt(this.tickrate);
   }

   @Override
   protected void updateEntityServer() {
      super.updateEntityServer();
      this.waterinputSlot.processIntoTank(this.inputTank, this.wateroutputSlot);
      if (++this.updateTicker >= this.tickrate) {
         this.updateSunVisibility();
         if (this.canWork()) {
            this.inputTank.drainInternal(1, true);
            this.outputTank.fillInternal(new FluidStack(FluidName.distilled_water.getInstance(), 1), true);
         }

         this.updateTicker = 0;
      }

      this.destiwaterinputSlot.processFromTank(this.outputTank, this.destiwateroutputSlott);
      this.upgradeSlot.tick();
   }

   public void updateSunVisibility() {
      this.skyLight = TileEntitySolarGenerator.getSkyLight(this.getLevel(), this.getBlockPos().above());
   }

   @Override
   public ContainerBase<TileEntitySolarDestiller> getGuiContainer(Player player) {
      return new ContainerSolarDestiller(player, this);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Screen getGui(Player player, boolean isAdmin) {
      return new GuiSolarDestiller(new ContainerSolarDestiller(player, this));
   }

   @Override
   public void onGuiClosed(Player player) {
   }

   public int getTickRate() {
      Holder<Biome> biome = BiomeUtil.getBiome(this.getLevel(), this.getBlockPos());
      if (BiomeCategory.hasType(biome, BiomeCategory.HOT)) {
         return 36;
      } else {
         return BiomeCategory.hasType(biome, BiomeCategory.COLD) ? 144 : 72;
      }
   }

   public int gaugeLiquidScaled(int i, int tank) {
      switch (tank) {
         case 0:
            if (this.inputTank.getFluidAmount() <= 0) {
               return 0;
            }

            return this.inputTank.getFluidAmount() * i / this.inputTank.getCapacity();
         case 1:
            if (this.outputTank.getFluidAmount() <= 0) {
               return 0;
            }

            return this.outputTank.getFluidAmount() * i / this.outputTank.getCapacity();
         default:
            return 0;
      }
   }

   public boolean canWork() {
      return this.inputTank.getFluidAmount() > 0 && this.outputTank.getFluidAmount() < this.outputTank.getCapacity() && this.skyLight > 0.5;
   }

   @Override
   public Set<UpgradableProperty> getUpgradableProperties() {
      return EnumSet.of(UpgradableProperty.ItemConsuming, UpgradableProperty.ItemProducing, UpgradableProperty.FluidProducing);
   }

   @Override
   public double getEnergy() {
      return 40.0;
   }

   @Override
   public boolean useEnergy(double amount) {
      return true;
   }
}
