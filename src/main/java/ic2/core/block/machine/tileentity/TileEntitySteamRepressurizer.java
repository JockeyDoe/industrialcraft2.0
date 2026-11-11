package ic2.core.block.machine.tileentity;

import ic2.api.energy.tile.IHeatSource;
import ic2.core.ContainerBase;
import ic2.core.IHasGui;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.comp.Fluids;
import ic2.core.block.comp.Ic2FluidTank;
import ic2.core.gui.dynamic.DynamicContainer;
import ic2.core.gui.dynamic.DynamicGui;
import ic2.core.gui.dynamic.GuiParser;
import ic2.core.init.MainConfig;
import ic2.core.network.GuiSynced;
import ic2.core.profile.NotClassic;
import ic2.core.ref.FluidName;
import ic2.core.util.ConfigUtil;
import ic2.core.util.FluidKeys;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;

@NotClassic
public class TileEntitySteamRepressurizer extends TileEntityInventory implements IHasGui {
   protected int currentHeat;
   @GuiSynced
   protected final Ic2FluidTank output;
   @GuiSynced
   protected final Ic2FluidTank input;
   protected static final int CONSUMPTION = 10;
   public static final Fluid STEAM = FluidKeys.find("steam");
   protected final Fluids fluids = this.addComponent(new Fluids(this));

   public TileEntitySteamRepressurizer() {
      this.input = this.fluids.addTankInsert("input", 10000, Fluids.fluidPredicate(FluidName.steam.getInstance(), FluidName.superheated_steam.getInstance()));
      this.output = this.fluids.addTankExtract("output", 10000);
   }

   @Override
   public void loadAdditional(CompoundTag nbt) {
      super.loadAdditional(nbt);
      this.currentHeat = nbt.getIntOr("heat", 0);
   }

   @Override
   public CompoundTag saveAdditional(CompoundTag nbt) {
      super.saveAdditional(nbt);
      nbt.putInt("heat", this.currentHeat);
      return nbt;
   }

   public static boolean hasSteam() {
      return STEAM != null;
   }

   @Override
   protected void updateEntityServer() {
      super.updateEntityServer();
      if (hasSteam() && this.input.getFluidAmount() >= 10) {
         if (this.currentHeat < this.input.getFluidAmount() / 10) {
            this.getHeat();
         }

         int amount = this.getOutput();

         while (this.currentHeat > 0 && this.input.getFluidAmount() >= 10 && this.canOutput(amount)) {
            this.currentHeat--;
            this.input.drainInternal(10, true);
            this.output.fillInternal(new FluidStack(STEAM, amount), true);
         }
      }
   }

   protected void getHeat() {
      int aim = this.input.getFluidAmount() / 10;
      if (aim > 0) {
         Level world = this.getLevel();
         int targetHeat = aim;

         for (Direction dir : Direction.values()) {
            if (world.getBlockEntity(this.getBlockPos().relative(dir)) instanceof IHeatSource hs) {
               int request = hs.drawHeat(dir.getOpposite(), targetHeat, true);
               if (request > 0) {
                  targetHeat -= hs.drawHeat(dir.getOpposite(), request, false);
                  if (targetHeat <= 0) {
                     break;
                  }
               }
            }
         }

         this.currentHeat += aim - targetHeat;
      }
   }

   protected int getOutput() {
      assert this.input.getFluid() != null;

      Fluid fluid = this.input.getFluid().getFluid();
      if (fluid == FluidName.steam.getInstance()) {
         return ConfigUtil.getInt(MainConfig.get(), "balance/steamRepressurizer/steamPerSteam");
      } else if (fluid == FluidName.superheated_steam.getInstance()) {
         return ConfigUtil.getInt(MainConfig.get(), "balance/steamRepressurizer/steamPerSuperSteam");
      } else {
         throw new IllegalStateException("Unknown tank contents: " + fluid);
      }
   }

   protected boolean canOutput(int amount) {
      return this.output.fillInternal(new FluidStack(STEAM, amount), false) == amount;
   }

   @Override
   public ContainerBase<TileEntitySteamRepressurizer> getGuiContainer(Player player) {
      return DynamicContainer.create(this, player, GuiParser.parse(this.teBlock));
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Screen getGui(Player player, boolean isAdmin) {
      return DynamicGui.<TileEntitySteamRepressurizer>create(this, player, GuiParser.parse(this.teBlock));
   }

   @Override
   public void onGuiClosed(Player player) {
   }

   @Override
   public boolean getGuiState(String name) {
      return "valid".equals(name) ? hasSteam() : super.getGuiState(name);
   }
}
