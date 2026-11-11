package ic2.core.block.machine.tileentity;

import ic2.api.recipe.IElectrolyzerRecipeManager;
import ic2.api.recipe.Recipes;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.ContainerBase;
import ic2.core.IC2;
import ic2.core.IHasGui;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.comp.Fluids;
import ic2.core.block.comp.Ic2FluidTank;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.block.machine.container.ContainerElectrolyzer;
import ic2.core.block.machine.gui.GuiElectrolyzer;
import ic2.core.gui.CustomGauge;
import ic2.core.recipe.ElectrolyzerRecipeManager;
import ic2.core.ref.FluidName;
import ic2.core.ref.TeBlock;
import ic2.core.util.FluidKeys;
import ic2.core.util.LiquidUtil;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;

@TeBlock.Delegated(current = TileEntityElectrolyzer.class, old = TileEntityClassicElectrolyzer.class)
public class TileEntityElectrolyzer extends TileEntityElectricMachine implements IUpgradableBlock, IHasGui, CustomGauge.IGaugeRatioProvider {
   protected int progress = 0;
   protected IElectrolyzerRecipeManager.ElectrolyzerRecipe recipe = null;
   protected Ic2FluidTank input;
   public final InvSlotUpgrade upgradeSlot;
   protected final Fluids fluids = this.addComponent(new Fluids(this));

   public static Class<? extends TileEntityInventory> delegate() {
      return IC2.version.isClassic() ? TileEntityClassicElectrolyzer.class : TileEntityElectrolyzer.class;
   }

   public TileEntityElectrolyzer() {
      super(32000, 2);
      this.input = this.fluids.addTankInsert("input", 8000, Fluids.fluidPredicate(Recipes.electrolyzer));
      this.upgradeSlot = new InvSlotUpgrade(this, "upgradeSlot", 4);
   }

   public static void init() {
      Recipes.electrolyzer = new ElectrolyzerRecipeManager();
      Recipes.electrolyzer
         .addRecipe(
            FluidKeys.name(net.minecraft.world.level.material.Fluids.WATER),
            40,
            32,
            new IElectrolyzerRecipeManager.ElectrolyzerOutput(FluidName.hydrogen.getName(), 26, Direction.DOWN),
            new IElectrolyzerRecipeManager.ElectrolyzerOutput(FluidName.oxygen.getName(), 13, Direction.UP)
         );
   }

   @Override
   public void loadAdditional(CompoundTag nbt) {
      super.loadAdditional(nbt);
      this.progress = nbt.getIntOr("progress", 0);
   }

   @Override
   public CompoundTag saveAdditional(CompoundTag nbt) {
      super.saveAdditional(nbt);
      nbt.putInt("progress", this.progress);
      return nbt;
   }

   @Override
   public void updateEntityServer() {
      super.updateEntityServer();
      boolean needsInvUpdate = false;
      if (this.canOperate()) {
         assert this.recipe != null;

         this.setActive(true);
         this.energy.useEnergy(this.recipe.EUaTick);
         this.progress++;
         if (this.progress >= this.recipe.ticksNeeded) {
            this.operate();
            this.progress = 0;
            needsInvUpdate = true;
         }
      } else {
         this.setActive(false);
         this.progress = 0;
      }

      needsInvUpdate |= this.upgradeSlot.tickNoMark();
      if (needsInvUpdate) {
         super.setChanged();
      }
   }

   protected boolean canOperate() {
      if (this.input.getFluid() == null) {
         return false;
      } else {
         this.recipe = Recipes.electrolyzer.getElectrolysisInformation(this.input.getFluid().getFluid());
         if (this.recipe != null && !(this.energy.getEnergy() < this.recipe.EUaTick) && this.input.getFluidAmount() >= this.recipe.inputAmount) {
            for (IElectrolyzerRecipeManager.ElectrolyzerOutput output : this.recipe.outputs) {
               if (!this.canFillTank(output.tankDirection, output.getOutput())) {
                  return false;
               }
            }

            return true;
         } else {
            return false;
         }
      }
   }

   protected void operate() {
      assert this.recipe != null;

      this.input.drainInternal(this.recipe.inputAmount, true);

      for (IElectrolyzerRecipeManager.ElectrolyzerOutput output : this.recipe.outputs) {
         this.fillTank(output.tankDirection, output.getOutput());
      }
   }

   protected boolean canFillTank(Direction facing, FluidStack fluid) {
      BlockEntity te = this.getLevel().getBlockEntity(this.getBlockPos().relative(facing));
      return te instanceof TileEntityTank ? LiquidUtil.fillTile(te, facing, fluid, true) == fluid.getAmount() : false;
   }

   protected void fillTank(Direction facing, FluidStack fluid) {
      BlockEntity te = this.getLevel().getBlockEntity(this.getBlockPos().relative(facing));
      if (te instanceof TileEntityTank) {
         LiquidUtil.fillTile(te, facing, fluid, false);
      }
   }

   @Override
   public Set<UpgradableProperty> getUpgradableProperties() {
      return EnumSet.of(UpgradableProperty.FluidConsuming);
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
   public ContainerBase<TileEntityElectrolyzer> getGuiContainer(Player player) {
      return new ContainerElectrolyzer(player, this);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Screen getGui(Player player, boolean isAdmin) {
      return new GuiElectrolyzer(new ContainerElectrolyzer(player, this));
   }

   @Override
   public void onGuiClosed(Player player) {
   }

   public Ic2FluidTank getInput() {
      return this.input;
   }

   public boolean hasRecipe() {
      return this.getCurrentRecipe() != null;
   }

   public IElectrolyzerRecipeManager.ElectrolyzerRecipe getCurrentRecipe() {
      return this.recipe;
   }

   @Override
   public double getRatio() {
      return this.recipe == null ? 0.0 : (double)this.progress / this.recipe.ticksNeeded;
   }
}
