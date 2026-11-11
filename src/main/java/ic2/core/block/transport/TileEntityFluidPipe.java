package ic2.core.block.transport;

import ic2.api.transport.IFluidPipe;
import ic2.core.IC2;
import ic2.core.block.comp.Ic2FluidTank;
import ic2.core.block.transport.cover.CoverProperty;
import ic2.core.block.transport.items.PipeSize;
import ic2.core.block.transport.items.PipeType;
import ic2.core.item.block.ItemPipe;
import ic2.core.util.LiquidUtil;
import ic2.core.util.LogCategory;
import ic2.core.util.Util;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT)
public class TileEntityFluidPipe extends TileEntityPipe implements IFluidPipe {
   protected PipeType type = PipeType.bronze;
   protected PipeSize size = PipeSize.small;
   protected TileEntityFluidPipe.PipeFluidTank tank;
   private boolean debug = false;

   public TileEntityFluidPipe() {
   }

   public TileEntityFluidPipe(PipeType type, PipeSize size) {
      this();
      this.type = type;
      this.size = size;
      this.tank = new TileEntityFluidPipe.PipeFluidTank(Util.allFacings, Util.allFacings, fluid -> true, (int)(type.transferRate * size.multiplier));
   }

   @Override
   public Set<CoverProperty> getCoverProperties() {
      return EnumSet.of(CoverProperty.FluidConsuming);
   }

   @Override
   public int getTransferRate() {
      return this.type.transferRate;
   }

   @Override
   public Ic2FluidTank getTank() {
      return this.tank;
   }

   @Override
   public int getCurrentInnerCapacity() {
      return this.tank.getFluidAmount();
   }

   @Override
   public int getMaxInnerCapacity() {
      return this.tank.getCapacity();
   }

   @Override
   public void flipConnection(Direction facing) {
      Level world = this.getLevel();
      BlockPos pos = this.getBlockPos();
      if (!world.isClientSide()) {
         this.connectivity = (byte)(this.connectivity ^ 1 << facing.ordinal());
         IC2.network.get(true).updateTileEntityField(this, "connectivity");
         world.updateNeighborsAt(pos, this.getBlockType());
         this.setChanged();
      }
   }

   @Override
   protected void updateEntityServer() {
      super.updateEntityServer();
      if (this.tank.getFluid() != null && this.tank.getFluidAmount() > 0) {
         int availableFluidAmount = this.tank.getFluidAmount();
         int cPipes = 1;
         List<LiquidUtil.AdjacentFluidHandler> adjacentFluidHandlers = new ArrayList<>();

         for (Direction facing : Direction.values()) {
            if (LiquidUtil.drainTile(this, facing, Integer.MAX_VALUE, true) != null) {
               LiquidUtil.AdjacentFluidHandler target = LiquidUtil.getAdjacentHandler(this, facing);
               if (target != null) {
                  if (target.handler instanceof IFluidPipe) {
                     int targetAmount = ((IFluidPipe)target.handler).getTank().getFluidAmount();
                     if (this.tank.getFluidAmount() >= targetAmount) {
                        availableFluidAmount += targetAmount;
                        cPipes++;
                        adjacentFluidHandlers.add(target);
                     }
                  } else if (LiquidUtil.fillTile(target.handler, facing.getOpposite(), this.tank.getFluid(), true) > 0) {
                     adjacentFluidHandlers.add(target);
                  }
               }
            }
         }

         if (this.debug) {
            IC2.log.warn(LogCategory.Transport, "Number of valid adjacentFluidHandlers: %s", adjacentFluidHandlers.size());
         }

         int extraFluid = availableFluidAmount % cPipes;
         availableFluidAmount /= cPipes;
         List<LiquidUtil.AdjacentFluidHandler> adjacentPipes = new ArrayList<>();
         if (!adjacentFluidHandlers.isEmpty()) {
            Iterator<LiquidUtil.AdjacentFluidHandler> it = adjacentFluidHandlers.iterator();

            while (it.hasNext()) {
               LiquidUtil.AdjacentFluidHandler target = it.next();
               if (target.handler instanceof IFluidPipe) {
                  FluidStack ret = LiquidUtil.transfer(
                     this, target.dir, target.handler, availableFluidAmount - ((IFluidPipe)target.handler).getTank().getFluidAmount()
                  );
                  if (this.debug) {
                     IC2.log.warn(LogCategory.Transport, "Split with pipe: %s to facing %s", ret != null ? ret.getAmount() : 0, target.dir);
                  }

                  adjacentPipes.add(target);
                  it.remove();
               }
            }
         }

         if (adjacentFluidHandlers.isEmpty()) {
            while (extraFluid > 0) {
               int bullet = IC2.random.nextInt(adjacentPipes.size());
               LiquidUtil.AdjacentFluidHandler target = adjacentPipes.get(bullet);
               LiquidUtil.transfer(this, target.dir, target.handler, 1);
               if (this.debug) {
                  IC2.log.warn(LogCategory.Transport, "Split with pipe: 1 to facing %s", target.dir);
               }

               extraFluid--;
               adjacentPipes.remove(bullet);
            }
         } else {
            availableFluidAmount += extraFluid;
         }

         if (!adjacentFluidHandlers.isEmpty()) {
            int maxTransfer = Math.min(availableFluidAmount, (int)(this.type.transferRate * this.size.multiplier / 20.0F));
            int maxAmountPerOutput = (int)Math.floor((float)maxTransfer / adjacentFluidHandlers.size());
            if (maxAmountPerOutput <= 0) {
               while (maxTransfer > 0) {
                  int bullet = IC2.random.nextInt(adjacentFluidHandlers.size());
                  LiquidUtil.AdjacentFluidHandler target = adjacentFluidHandlers.get(bullet);
                  LiquidUtil.transfer(this, target.dir, target.handler, 1);
                  if (this.debug) {
                     IC2.log.warn(LogCategory.Transport, "Transferred: 1 to facing %s", target.dir);
                  }

                  maxTransfer--;
                  adjacentFluidHandlers.remove(bullet);
               }
            } else {
               for (LiquidUtil.AdjacentFluidHandler target : adjacentFluidHandlers) {
                  FluidStack ret = LiquidUtil.transfer(this, target.dir, target.handler, maxAmountPerOutput);
                  if (this.debug) {
                     IC2.log.warn(LogCategory.Transport, "Transferred: %s to facing %s", ret != null ? ret.getAmount() : 0, target.dir);
                  }
               }
            }
         }
      }
   }

   @Override
   public void loadAdditional(CompoundTag nbt) {
      super.loadAdditional(nbt);
      this.type = PipeType.values[nbt.getByteOr("type", (byte)0) & 255];
      this.size = PipeSize.values()[nbt.getByteOr("size", (byte)0) & 255];
      this.tank = new TileEntityFluidPipe.PipeFluidTank(Util.allFacings, Util.allFacings, fluid -> true, (int)(this.type.transferRate * this.size.multiplier));
      this.tank.readFromNBT(nbt);
   }

   @Override
   public CompoundTag saveAdditional(CompoundTag nbt) {
      super.saveAdditional(nbt);
      nbt.putByte("type", (byte)this.type.ordinal());
      nbt.putByte("size", (byte)this.size.ordinal());
      this.tank.writeToNBT(nbt);
      return nbt;
   }

   @Override
   protected ItemStack getPickBlock(Player player, HitResult target) {
      return ItemPipe.getPipe(this.type, this.size);
   }

   @Override
   public List<String> getNetworkedFields() {
      List<String> ret = super.getNetworkedFields();
      ret.add("type");
      ret.add("size");
      return ret;
   }

   @Override
   protected void updateRenderState() {
      this.renderState = new TileEntityPipe.PipeRenderState(this.type, this.size, this.connectivity, this.covers, this.getFacing().ordinal());
   }

   @Override
   protected void updateConnectivity() {
      byte newConnectivity = this.connectivity;

      for (Direction direction : Direction.values()) {
         BlockEntity tile = this.getLevel().getBlockEntity(this.getBlockPos().relative(direction));
         if (tile != null) {
            if (tile instanceof IFluidPipe) {
               if (((IFluidPipe)tile).isConnected(direction.getOpposite())) {
                  newConnectivity = (byte)(newConnectivity | 1 << direction.ordinal());
               }
            } else if (LiquidUtil.isFluidTile(tile, direction.getOpposite())) {
               newConnectivity = (byte)(newConnectivity | this.connectivity & 1 << direction.ordinal());
            }
         }
      }

      if (this.connectivity != newConnectivity) {
         this.connectivity = newConnectivity;
         IC2.network.get(true).updateTileEntityField(this, "connectivity");
      }
   }

   @Override
   public void onPlaced(ItemStack stack, LivingEntity placer, Direction facing) {
      super.onPlaced(stack, placer, facing);
      if (!this.getLevel().isClientSide()) {
         BlockEntity tile = this.getLevel().getBlockEntity(this.getBlockPos().relative(facing.getOpposite()));
         if (tile != null && (tile instanceof IFluidPipe || LiquidUtil.isFluidTile(tile, facing))) {
            this.flipConnection(facing.getOpposite());
            if (tile instanceof IFluidPipe other && !other.isConnected(facing)) {
               other.flipConnection(facing);
            }
         }
      }
   }

   @Override
   protected void onBlockBreak() {
      super.onBlockBreak();
      if (this.tank.getFluidAmount() > 1000 && LiquidUtil.fillBlock(this.tank.getFluid(), this.getLevel(), this.getBlockPos(), true)) {
         LiquidUtil.fillBlock(this.tank.getFluid(), this.getLevel(), this.getBlockPos(), false);
      }
   }

   @Override
   public boolean hasCapability(BlockCapability<?, Direction> capability, Direction facing) {
      return capability == net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK
         ? facing != null && this.isConnected(facing)
         : super.hasCapability(capability, facing);
   }

   @Override
   public <T> T getCapability(BlockCapability<T, Direction> capability, Direction facing) {
      if (capability != net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK) {
         return super.getCapability(capability, facing);
      } else {
         return (T)(facing != null && this.isConnected(facing) ? new TileEntityFluidPipe.PipeFluidHandler(facing) : null);
      }
   }

   @Override
   protected List<AABB> getAabbs(boolean forCollision) {
      if (!forCollision) {
         return super.getAabbs(false);
      } else {
         float th = this.size.thickness;
         float sp = (1.0F - th) / 2.0F;
         List<AABB> ret = new ArrayList<>(7);
         ret.add(new AABB(sp, sp, sp, sp + th, sp + th, sp + th));

         for (Direction facing : Direction.values()) {
            boolean hasConnection = (this.connectivity & 1 << facing.ordinal()) != 0;
            if (hasConnection) {
               float zS = sp;
               float yS = sp;
               float xS = sp;
               float yE;
               float zE;
               float xE = yE = zE = sp + th;
               switch (facing) {
                  case DOWN:
                     yS = 0.0F;
                     yE = sp;
                     break;
                  case UP:
                     yS = sp + th;
                     yE = 1.0F;
                     break;
                  case NORTH:
                     zS = 0.0F;
                     zE = sp;
                     break;
                  case SOUTH:
                     zS = sp + th;
                     zE = 1.0F;
                     break;
                  case WEST:
                     xS = 0.0F;
                     xE = sp;
                     break;
                  case EAST:
                     xS = sp + th;
                     xE = 1.0F;
                     break;
                  default:
                     throw new RuntimeException();
               }

               ret.add(new AABB(xS, yS, zS, xE, yE, zE));
               float cs = 1.0F;
               float ch = 0.1F;
               boolean hasCover = (this.covers & 1 << facing.ordinal()) != 0;
               zS = 0.0F;
               yS = 0.0F;
               xS = 0.0F;
               zE = 1.0F;
               yE = 1.0F;
               xE = 1.0F;
               if (hasCover) {
                  switch (facing) {
                     case DOWN:
                        yS = 0.0F;
                        yE = ch;
                        break;
                     case UP:
                        yS = cs - ch;
                        yE = 1.0F;
                        break;
                     case NORTH:
                        zS = 0.0F;
                        zE = ch;
                        break;
                     case SOUTH:
                        zS = cs - ch;
                        zE = 1.0F;
                        break;
                     case WEST:
                        xS = 0.0F;
                        xE = ch;
                        break;
                     case EAST:
                        xS = cs - ch;
                        xE = 1.0F;
                        break;
                     default:
                        throw new RuntimeException();
                  }

                  ret.add(new AABB(xS, yS, zS, xE, yE, zE));
               }
            }
         }

         return ret;
      }
   }

   @Override
   public AABB getVisualBoundingBox() {
      return this.getPhysicsBoundingBox();
   }

   @Override
   protected AABB getOutlineBoundingBox() {
      return super.getVisualBoundingBox();
   }

   @OnlyIn(Dist.CLIENT)
   @SubscribeEvent(priority = EventPriority.LOWEST)
   public static void drawBetter(ExtractBlockOutlineRenderStateEvent event) {
      if (event.getLevel().getBlockEntity(event.getBlockPos()) instanceof TileEntityFluidPipe pipe) {
         event.getLevelRenderState().blockOutlineRenderState = new BlockOutlineRenderState(
            event.getBlockPos(), event.isInTranslucentPass(), event.isHighContrast(), Shapes.create(pipe.getVisualBoundingBox().inflate(0.002))
         );
         event.setCanceled(true);
      }
   }

   private class PipeFluidHandler implements ResourceHandler<FluidResource> {
      private final Direction side;

      PipeFluidHandler(Direction side) {
         Objects.requireNonNull(TileEntityFluidPipe.this);
         super();
         this.side = side;
      }

      public int size() {
         return 1;
      }

      public FluidResource getResource(int i) {
         return (FluidResource)TileEntityFluidPipe.this.tank.getResource(i);
      }

      public long getAmountAsLong(int i) {
         return TileEntityFluidPipe.this.tank.getAmountAsLong(i);
      }

      public long getCapacityAsLong(int i, FluidResource r) {
         return TileEntityFluidPipe.this.tank.getCapacityAsLong(i, r);
      }

      public boolean isValid(int i, FluidResource r) {
         return TileEntityFluidPipe.this.tank.isValid(i, r)
            && (
               !TileEntityFluidPipe.this.coversComponent.hasCover(this.side)
                  || TileEntityFluidPipe.this.coversComponent.getCoverItem(this.side).allowsInput(r.toStack(1))
            );
      }

      public int insert(int i, FluidResource r, int amount, TransactionContext tx) {
         return this.isValid(i, r) ? TileEntityFluidPipe.this.tank.insert(i, r, amount, tx) : 0;
      }

      public int extract(int i, FluidResource r, int amount, TransactionContext tx) {
         return TileEntityFluidPipe.this.coversComponent.hasCover(this.side)
               && !TileEntityFluidPipe.this.coversComponent.getCoverItem(this.side).allowsOutput(r.toStack(amount))
            ? 0
            : TileEntityFluidPipe.this.tank.extract(i, r, amount, tx);
      }
   }

   private class PipeFluidTank extends Ic2FluidTank {
      private final Predicate<Fluid> acceptedFluids;
      private Collection<Direction> inputSides;
      private Collection<Direction> outputSides;

      protected PipeFluidTank(Collection<Direction> inputSides, Collection<Direction> outputSides, Predicate<Fluid> acceptedFluids, int capacity) {
         Objects.requireNonNull(TileEntityFluidPipe.this);
         super(capacity);
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
         return this.acceptedFluids.test(fluid);
      }

      public boolean canFill(Direction side) {
         return this.inputSides.contains(side);
      }

      public boolean canDrain(Direction side) {
         return this.outputSides.contains(side);
      }
   }
}
