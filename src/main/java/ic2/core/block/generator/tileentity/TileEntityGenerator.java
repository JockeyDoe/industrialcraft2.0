package ic2.core.block.generator.tileentity;

import ic2.core.block.invslot.InvSlotConsumableFuel;
import ic2.core.block.machine.tileentity.TileEntityIronFurnace;
import ic2.core.gui.dynamic.IGuiValueProvider;
import ic2.core.init.MainConfig;
import ic2.core.network.GuiSynced;
import ic2.core.util.ConfigUtil;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class TileEntityGenerator extends TileEntityBaseGenerator implements IGuiValueProvider {
   public final InvSlotConsumableFuel fuelSlot;
   @GuiSynced
   public int totalFuel = 0;

   public TileEntityGenerator() {
      super(Math.round(10.0F * ConfigUtil.getFloat(MainConfig.get(), "balance/energy/generator/generator")), 1, 4000);
      this.fuelSlot = new InvSlotConsumableFuel(this, "fuel", 1, false);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   protected void updateEntityClient() {
      super.updateEntityClient();
      if (this.getActive()) {
         TileEntityIronFurnace.showFlames(this.getLevel(), this.getBlockPos(), this.getFacing());
      }
   }

   public double getFuelRatio() {
      return this.fuel <= 0 ? 0.0 : (double)this.fuel / this.totalFuel;
   }

   @Override
   public boolean gainFuel() {
      int fuelValue = this.fuelSlot.consumeFuel() / 4;
      if (fuelValue == 0) {
         return false;
      } else {
         this.fuel += fuelValue;
         this.totalFuel = fuelValue;
         return true;
      }
   }

   @Override
   public boolean isConverting() {
      return this.fuel > 0;
   }

   @Override
   public String getOperationSoundFile() {
      return "Generators/GeneratorLoop.ogg";
   }

   @Override
   public double getGuiValue(String name) {
      if ("fuel".equals(name)) {
         return this.getFuelRatio();
      } else {
         throw new IllegalArgumentException();
      }
   }

   @Override
   public void loadAdditional(CompoundTag nbt) {
      super.loadAdditional(nbt);
      this.totalFuel = nbt.getIntOr("totalFuel", 0);
   }

   @Override
   public CompoundTag saveAdditional(CompoundTag nbt) {
      super.saveAdditional(nbt);
      nbt.putInt("totalFuel", this.totalFuel);
      return nbt;
   }
}
