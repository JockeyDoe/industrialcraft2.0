package ic2.core.block.comp;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import ic2.api.recipe.ILiquidAcceptManager;
import ic2.core.block.TileEntityBlock;
import ic2.core.block.invslot.InvSlot;
import ic2.core.util.NbtChecks;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class Fluids extends TileEntityComponent {
   protected final List<Fluids.InternalFluidTank> managedTanks = new ArrayList<>();
   protected final List<Supplier<? extends Collection<Fluids.InternalFluidTank>>> unmanagedTanks = new ArrayList<>();

   public Fluids(TileEntityBlock parent) {
      super(parent);
   }

   public Fluids.InternalFluidTank addTankInsert(String name, int capacity) {
      return this.addTankInsert(name, capacity, Predicates.alwaysTrue());
   }

   public Fluids.InternalFluidTank addTankInsert(String name, int capacity, Predicate<Fluid> acceptedFluids) {
      return this.addTankInsert(name, capacity, InvSlot.InvSide.ANY, acceptedFluids);
   }

   public Fluids.InternalFluidTank addTankInsert(String name, int capacity, InvSlot.InvSide side) {
      return this.addTankInsert(name, capacity, side, Predicates.alwaysTrue());
   }

   public Fluids.InternalFluidTank addTankInsert(String name, int capacity, InvSlot.InvSide side, Predicate<Fluid> acceptedFluids) {
      return this.addTank(name, capacity, InvSlot.Access.I, side, acceptedFluids);
   }

   public Fluids.InternalFluidTank addTankExtract(String name, int capacity) {
      return this.addTankExtract(name, capacity, InvSlot.InvSide.ANY);
   }

   public Fluids.InternalFluidTank addTankExtract(String name, int capacity, InvSlot.InvSide side) {
      return this.addTank(name, capacity, InvSlot.Access.O, side);
   }

   public Fluids.InternalFluidTank addTank(String name, int capacity) {
      return this.addTank(name, capacity, InvSlot.Access.IO);
   }

   public Fluids.InternalFluidTank addTank(String name, int capacity, InvSlot.Access access) {
      return this.addTank(name, capacity, access, InvSlot.InvSide.ANY);
   }

   public Fluids.InternalFluidTank addTank(String name, int capacity, Predicate<Fluid> acceptedFluids) {
      return this.addTank(name, capacity, InvSlot.Access.IO, InvSlot.InvSide.ANY, acceptedFluids);
   }

   public Fluids.InternalFluidTank addTank(String name, int capacity, InvSlot.Access access, InvSlot.InvSide side) {
      return this.addTank(name, capacity, access, side, Predicates.alwaysTrue());
   }

   public Fluids.InternalFluidTank addTank(String name, int capacity, InvSlot.Access access, InvSlot.InvSide side, Predicate<Fluid> acceptedFluids) {
      return this.addTank(
         name,
         capacity,
         access.isInput() ? side.getAcceptedSides() : Collections.emptySet(),
         access.isOutput() ? side.getAcceptedSides() : Collections.emptySet(),
         acceptedFluids
      );
   }

   public Fluids.InternalFluidTank addTank(
      String name, int capacity, Collection<Direction> inputSides, Collection<Direction> outputSides, Predicate<Fluid> acceptedFluids
   ) {
      return this.addTank(new Fluids.InternalFluidTank(name, inputSides, outputSides, acceptedFluids, capacity));
   }

   public Fluids.InternalFluidTank addTank(Fluids.InternalFluidTank tank) {
      this.managedTanks.add(tank);
      return tank;
   }

   public void addUnmanagedTanks(Fluids.InternalFluidTank tank) {
      this.unmanagedTanks.add(Suppliers.ofInstance(Collections.singleton(tank)));
   }

   public void addUnmanagedTanks(Collection<Fluids.InternalFluidTank> tanks) {
      this.addUnmanagedTankHook(Suppliers.ofInstance(tanks));
   }

   public void addUnmanagedTankHook(Supplier<? extends Collection<Fluids.InternalFluidTank>> suppl) {
      this.unmanagedTanks.add(suppl);
   }

   public void changeConnectivity(Fluids.InternalFluidTank tank, InvSlot.Access access, InvSlot.InvSide side) {
      this.changeConnectivity(
         tank, access.isInput() ? side.getAcceptedSides() : Collections.emptySet(), access.isOutput() ? side.getAcceptedSides() : Collections.emptySet()
      );
   }

   public void changeConnectivity(Fluids.InternalFluidTank tank, Collection<Direction> inputSides, Collection<Direction> outputSides) {
      assert this.managedTanks.contains(tank);

      tank.inputSides = inputSides;
      tank.outputSides = outputSides;
   }

   public Ic2FluidTank getFluidTank(String name) {
      for (Fluids.InternalFluidTank tank : this.getAllTanks()) {
         if (tank.identifier.equals(name)) {
            return tank;
         }
      }

      throw new IllegalArgumentException("Unable to find tank: " + name);
   }

   @Override
   public void readFromNbt(CompoundTag nbt) {
      for (Fluids.InternalFluidTank tank : this.managedTanks) {
         if (NbtChecks.hasType(nbt, tank.identifier, 10)) {
            tank.readFromNBT(nbt.getCompoundOrEmpty(tank.identifier));
         }
      }
   }

   @Override
   public CompoundTag writeToNbt() {
      CompoundTag nbt = new CompoundTag();

      for (Fluids.InternalFluidTank tank : this.managedTanks) {
         CompoundTag subTag = new CompoundTag();
         subTag = tank.writeToNBT(subTag);
         nbt.put(tank.identifier, subTag);
      }

      return nbt;
   }

   @Override
   public Collection<? extends BlockCapability<?, Direction>> getProvidedCapabilities(Direction side) {
      return Collections.singleton(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK);
   }

   @Override
   public <T> T getCapability(BlockCapability<T, Direction> cap, Direction side) {
      return (T)(cap == net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK ? new Fluids.FluidHandler(side) : super.getCapability(cap, side));
   }

   public static Predicate<Fluid> fluidPredicate(Fluid... fluids) {
      final Collection<Fluid> acceptedFluids;
      if (fluids.length > 10) {
         acceptedFluids = new HashSet<>(Arrays.asList(fluids));
      } else {
         acceptedFluids = Arrays.asList(fluids);
      }

      return new Predicate<Fluid>() {
         public boolean apply(Fluid fluid) {
            return acceptedFluids.contains(fluid);
         }
      };
   }

   public static Predicate<Fluid> fluidPredicate(final ILiquidAcceptManager manager) {
      return new Predicate<Fluid>() {
         public boolean apply(Fluid fluid) {
            return manager.acceptsFluid(fluid);
         }
      };
   }

   public Iterable<Fluids.InternalFluidTank> getAllTanks() {
      if (this.unmanagedTanks.isEmpty()) {
         return this.managedTanks;
      } else {
         List<Fluids.InternalFluidTank> tanks = new ArrayList<>();
         tanks.addAll(this.managedTanks);

         for (Supplier<? extends Collection<Fluids.InternalFluidTank>> suppl : this.unmanagedTanks) {
            tanks.addAll((Collection<? extends Fluids.InternalFluidTank>)suppl.get());
         }

         return tanks;
      }
   }

   private class FluidHandler implements ResourceHandler<FluidResource> {
      private final Direction side;

      FluidHandler(Direction side) {
         Objects.requireNonNull(Fluids.this);
         super();
         this.side = side;
      }

      private List<Fluids.InternalFluidTank> tanks() {
         List<Fluids.InternalFluidTank> result = new ArrayList<>();

         for (Fluids.InternalFluidTank tank : Fluids.this.getAllTanks()) {
            if (this.side == null || tank.canFill(this.side) || tank.canDrain(this.side)) {
               result.add(tank);
            }
         }

         return result;
      }

      public int size() {
         return this.tanks().size();
      }

      public FluidResource getResource(int i) {
         return (FluidResource)this.tanks().get(i).getResource(0);
      }

      public long getAmountAsLong(int i) {
         return this.tanks().get(i).getAmountAsLong(0);
      }

      public long getCapacityAsLong(int i, FluidResource r) {
         return this.tanks().get(i).getCapacityAsLong(0, r);
      }

      public boolean isValid(int i, FluidResource r) {
         Fluids.InternalFluidTank tank = this.tanks().get(i);
         return (this.side == null || tank.canFill(this.side)) && tank.isValid(0, r);
      }

      public int insert(int i, FluidResource r, int amount, TransactionContext tx) {
         Fluids.InternalFluidTank tank = this.tanks().get(i);
         return this.side != null && !tank.canFill(this.side) ? 0 : tank.insert(0, r, amount, tx);
      }

      public int extract(int i, FluidResource r, int amount, TransactionContext tx) {
         Fluids.InternalFluidTank tank = this.tanks().get(i);
         return this.side != null && !tank.canDrain(this.side) ? 0 : tank.extract(0, r, amount, tx);
      }
   }

   public static class InternalFluidTank extends Ic2FluidTank {
      protected final String identifier;
      private final Predicate<Fluid> acceptedFluids;
      private Collection<Direction> inputSides;
      private Collection<Direction> outputSides;

      protected InternalFluidTank(
         String identifier, Collection<Direction> inputSides, Collection<Direction> outputSides, Predicate<Fluid> acceptedFluids, int capacity
      ) {
         super(capacity);
         this.identifier = identifier;
         this.acceptedFluids = acceptedFluids;
         this.inputSides = inputSides;
         this.outputSides = outputSides;
      }

      @Override
      public boolean canFillFluidType(FluidStack fluid) {
         return fluid != null && this.acceptsFluid(fluid.getFluid());
      }

      @Override
      public boolean canDrainFluidType(FluidStack fluid) {
         return fluid != null && this.acceptsFluid(fluid.getFluid());
      }

      public boolean acceptsFluid(Fluid fluid) {
         return this.acceptedFluids.apply(fluid);
      }

      public boolean canFill(Direction side) {
         return this.inputSides.contains(side);
      }

      public boolean canDrain(Direction side) {
         return this.outputSides.contains(side);
      }
   }
}
