package ic2.core.block.machine.tileentity;

import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.MachineRecipe;
import ic2.api.recipe.MachineRecipeResult;
import ic2.api.recipe.Recipes;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.block.invslot.InvSlotProcessableGeneric;
import ic2.core.recipe.BasicMachineRecipeManager;
import ic2.core.util.LiquidUtil;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

public class TileEntityCompressor extends TileEntityStandardMachine<IRecipeInput, Collection<ItemStack>, ItemStack> {
   protected boolean usingPumpRecipe;
   protected final Set<TileEntityPump> pumps = new HashSet<>(12, 0.5F);

   public TileEntityCompressor() {
      super(2, 300, 1);
      this.inputSlot = new InvSlotProcessableGeneric(this, "input", 1, Recipes.compressor);
   }

   public static void init() {
      Recipes.compressor = new BasicMachineRecipeManager();
   }

   @Override
   protected void onLoaded() {
      super.onLoaded();
      this.findPumps();
   }

   @Override
   protected void onNeighborChange(Block neighbor, BlockPos neighborPos) {
      super.onNeighborChange(neighbor, neighborPos);
      this.findPumps();
   }

   protected void findPumps() {
      Level world = this.getLevel();
      this.pumps.clear();

      for (Direction side : Direction.values()) {
         BlockEntity te = world.getBlockEntity(this.getBlockPos().relative(side));
         if (te instanceof TileEntityPump) {
            this.pumps.add((TileEntityPump)te);
         }
      }
   }

   @Override
   public MachineRecipeResult<IRecipeInput, Collection<ItemStack>, ItemStack> getOutput() {
      this.usingPumpRecipe = false;
      MachineRecipeResult<IRecipeInput, Collection<ItemStack>, ItemStack> output = super.getOutput();
      if (output != null) {
         return output;
      } else {
         if (!this.pumps.isEmpty() && this.inputSlot.isEmpty() && this.outputSlot.canAdd(new ItemStack(Items.SNOWBALL))) {
            FluidStack fluid = new FluidStack(Fluids.WATER, 1000);

            for (TileEntityPump pump : this.pumps) {
               FluidStack amount = LiquidUtil.drainTile(pump, Direction.UP, Fluids.WATER, fluid.getAmount(), true);
               if (amount != null) {
                  assert amount.getFluid() == Fluids.WATER;

                  fluid.setAmount(fluid.getAmount() - amount.getAmount());
               }

               if (fluid.getAmount() <= 0) {
                  this.usingPumpRecipe = true;
                  output = new MachineRecipe<>(null, Collections.singletonList(new ItemStack(Items.SNOWBALL))).getResult(null);
                  break;
               }
            }
         }

         return output;
      }
   }

   @Override
   public void operateOnce(MachineRecipeResult<IRecipeInput, Collection<ItemStack>, ItemStack> output, Collection<ItemStack> processResult) {
      if (this.usingPumpRecipe) {
         FluidStack fluid = new FluidStack(Fluids.WATER, 1000);

         for (TileEntityPump pump : this.pumps) {
            FluidStack amount = LiquidUtil.drainTile(pump, Direction.UP, Fluids.WATER, fluid.getAmount(), false);
            if (amount != null && amount.getFluid() == Fluids.WATER) {
               fluid.setAmount(fluid.getAmount() - amount.getAmount());
            }

            if (fluid.getAmount() <= 0) {
               break;
            }
         }

         this.outputSlot.add(processResult);
      } else {
         super.operateOnce(output, processResult);
      }
   }

   @Override
   public String getStartSoundFile() {
      return "Machines/CompressorOp.ogg";
   }

   @Override
   public String getInterruptSoundFile() {
      return "Machines/InterruptOne.ogg";
   }

   @Override
   public Set<UpgradableProperty> getUpgradableProperties() {
      return EnumSet.of(
         UpgradableProperty.Processing,
         UpgradableProperty.Transformer,
         UpgradableProperty.EnergyStorage,
         UpgradableProperty.ItemConsuming,
         UpgradableProperty.ItemProducing
      );
   }
}
