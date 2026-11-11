package ic2.core.block.reactor.tileentity;

import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IEnergySource;
import ic2.api.energy.tile.IEnergyTile;
import ic2.api.energy.tile.IMetaDelegate;
import ic2.api.reactor.IBaseReactorComponent;
import ic2.api.reactor.IReactor;
import ic2.api.reactor.IReactorChamber;
import ic2.api.reactor.IReactorComponent;
import ic2.api.recipe.ILiquidHeatExchangerManager;
import ic2.api.recipe.Recipes;
import ic2.core.ContainerBase;
import ic2.core.ExplosionIC2;
import ic2.core.IC2;
import ic2.core.IC2DamageSource;
import ic2.core.IHasGui;
import ic2.core.audio.AudioSource;
import ic2.core.audio.PositionSpec;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.comp.Fluids;
import ic2.core.block.comp.Ic2FluidTank;
import ic2.core.block.comp.Redstone;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumableLiquid;
import ic2.core.block.invslot.InvSlotConsumableLiquidByManager;
import ic2.core.block.invslot.InvSlotConsumableLiquidByTank;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.block.invslot.InvSlotReactor;
import ic2.core.block.reactor.container.ContainerNuclearReactor;
import ic2.core.block.reactor.gui.GuiNuclearReactor;
import ic2.core.block.type.ResourceBlock;
import ic2.core.gui.dynamic.IGuiValueProvider;
import ic2.core.init.MainConfig;
import ic2.core.item.reactor.ItemReactorHeatStorage;
import ic2.core.ref.BlockMaterial;
import ic2.core.ref.BlockName;
import ic2.core.ref.TeBlock;
import ic2.core.util.ConfigUtil;
import ic2.core.util.LogCategory;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import ic2.core.util.WorldQueries;
import ic2.core.util.WorldSearchUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.fluids.FluidStack;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class TileEntityNuclearReactorElectric extends TileEntityInventory implements IHasGui, IReactor, IEnergySource, IMetaDelegate, IGuiValueProvider {
   public AudioSource audioSourceMain;
   public AudioSource audioSourceGeiger;
   private float lastOutput = 0.0F;
   public final Fluids.InternalFluidTank inputTank;
   public final Fluids.InternalFluidTank outputTank;
   private final List<IEnergyTile> subTiles = new ArrayList<>();
   public final InvSlotReactor reactorSlot;
   public final InvSlotOutput coolantoutputSlot;
   public final InvSlotOutput hotcoolantoutputSlot;
   public final InvSlotConsumableLiquidByManager coolantinputSlot;
   public final InvSlotConsumableLiquidByTank hotcoolinputSlot;
   public final Redstone redstone;
   protected final Fluids fluids;
   public float output = 0.0F;
   public int updateTicker;
   public int heat = 0;
   public int maxHeat = 10000;
   public float hem = 1.0F;
   private int EmitHeatbuffer = 0;
   public int EmitHeat = 0;
   private boolean fluidCooled = false;
   public boolean addedToEnergyNet = false;
   private static final float huOutputModifier = 40.0F * ConfigUtil.getFloat(MainConfig.get(), "balance/energy/FluidReactor/outputModifier");

   public TileEntityNuclearReactorElectric() {
      this.updateTicker = IC2.random.nextInt(this.getTickRate());
      this.fluids = this.addComponent(new Fluids(this));
      this.inputTank = this.fluids.addTank("inputTank", 10000, InvSlot.Access.NONE, InvSlot.InvSide.ANY, Fluids.fluidPredicate(Recipes.liquidHeatupManager));
      this.outputTank = this.fluids.addTank("outputTank", 10000, InvSlot.Access.NONE);
      this.reactorSlot = new InvSlotReactor(this, "reactor", 54);
      this.coolantinputSlot = new InvSlotConsumableLiquidByManager(
         this, "coolantinputSlot", InvSlot.Access.I, 1, InvSlot.InvSide.ANY, InvSlotConsumableLiquid.OpType.Drain, Recipes.liquidHeatupManager
      );
      this.hotcoolinputSlot = new InvSlotConsumableLiquidByTank(
         this, "hotcoolinputSlot", InvSlot.Access.I, 1, InvSlot.InvSide.ANY, InvSlotConsumableLiquid.OpType.Fill, this.outputTank
      );
      this.coolantoutputSlot = new InvSlotOutput(this, "coolantoutputSlot", 1);
      this.hotcoolantoutputSlot = new InvSlotOutput(this, "hotcoolantoutputSlot", 1);
      this.redstone = this.addComponent(new Redstone(this));
   }

   @Override
   protected void onLoaded() {
      super.onLoaded();
      if (!this.getLevel().isClientSide() && !this.isFluidCooled()) {
         this.refreshChambers();
         NeoForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
         this.addedToEnergyNet = true;
      }

      this.createChamberRedstoneLinks();
      if (this.isFluidCooled()) {
         this.createCasingRedstoneLinks();
         this.openTanks();
      }
   }

   @Override
   protected void onUnloaded() {
      if (IC2.platform.isRendering()) {
         IC2.audioManager.removeSources(this);
         this.audioSourceMain = null;
         this.audioSourceGeiger = null;
      }

      if (IC2.platform.isSimulating() && this.addedToEnergyNet) {
         NeoForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
         this.addedToEnergyNet = false;
      }

      super.onUnloaded();
   }

   public int gaugeHeatScaled(int i) {
      return i * this.heat / (this.maxHeat / 100 * 85);
   }

   @Override
   public void loadAdditional(CompoundTag nbt) {
      super.loadAdditional(nbt);
      this.heat = nbt.getIntOr("heat", 0);
      this.output = nbt.getShortOr("output", (short)0);
   }

   @Override
   public CompoundTag saveAdditional(CompoundTag nbt) {
      nbt = super.saveAdditional(nbt);
      nbt.putInt("heat", this.heat);
      nbt.putShort("output", (short)this.getReactorEnergyOutput());
      return nbt;
   }

   @Override
   protected void onNeighborChange(Block neighbor, BlockPos neighborPos) {
      super.onNeighborChange(neighbor, neighborPos);
      if (this.addedToEnergyNet) {
         this.refreshChambers();
      }
   }

   @Override
   public void drawEnergy(double amount) {
   }

   @Override
   public boolean emitsEnergyTo(IEnergyAcceptor receiver, Direction direction) {
      return true;
   }

   @Override
   public double getOfferedEnergy() {
      return this.getReactorEnergyOutput() * 5.0F * ConfigUtil.getFloat(MainConfig.get(), "balance/energy/generator/nuclear");
   }

   @Override
   public int getSourceTier() {
      return 5;
   }

   @Override
   public double getReactorEUEnergyOutput() {
      return this.getOfferedEnergy();
   }

   @Override
   public List<IEnergyTile> getSubTiles() {
      return Collections.unmodifiableList(new ArrayList<>(this.subTiles));
   }

   private void processfluidsSlots() {
      this.coolantinputSlot.processIntoTank(this.inputTank, this.coolantoutputSlot);
      this.hotcoolinputSlot.processFromTank(this.outputTank, this.hotcoolantoutputSlot);
   }

   public void refreshChambers() {
      Level world = this.getLevel();
      List<IEnergyTile> newSubTiles = new ArrayList<>();
      newSubTiles.add(this);

      for (Direction dir : Direction.values()) {
         BlockEntity te = world.getBlockEntity(this.getBlockPos().relative(dir));
         if (te instanceof TileEntityReactorChamberElectric && !te.isRemoved()) {
            newSubTiles.add((TileEntityReactorChamberElectric)te);
         }
      }

      if (!newSubTiles.equals(this.subTiles)) {
         if (this.addedToEnergyNet) {
            NeoForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
         }

         this.subTiles.clear();
         this.subTiles.addAll(newSubTiles);
         if (this.addedToEnergyNet) {
            NeoForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
         }
      }
   }

   @Override
   protected void updateEntityServer() {
      super.updateEntityServer();
      if (this.updateTicker++ % this.getTickRate() == 0) {
         if (!this.getLevel().isAreaLoaded(this.getBlockPos(), 8)) {
            this.output = 0.0F;
         } else {
            boolean toFluidCooled = this.isFluidReactor();
            if (this.fluidCooled != toFluidCooled) {
               if (toFluidCooled) {
                  this.enableFluidMode();
               } else {
                  this.disableFluidMode();
               }

               this.fluidCooled = toFluidCooled;
            }

            this.dropAllUnfittingStuff();
            this.output = 0.0F;
            this.maxHeat = 10000;
            this.hem = 1.0F;
            this.processChambers();
            if (this.fluidCooled) {
               this.processfluidsSlots();
               FluidStack inputFluid = this.inputTank.getFluid();

               assert inputFluid == null || Recipes.liquidHeatupManager.acceptsFluid(this.inputTank.getFluid().getFluid());

               int huOtput = (int)(huOutputModifier * this.EmitHeatbuffer);
               int outputroom = this.outputTank.getCapacity() - this.outputTank.getFluidAmount();
               this.EmitHeatbuffer = 0;
               if (outputroom > 0 && inputFluid != null) {
                  ILiquidHeatExchangerManager.HeatExchangeProperty prop = Recipes.liquidHeatupManager.getHeatExchangeProperty(inputFluid.getFluid());
                  int fluidOutput = huOtput / prop.huPerMB;
                  FluidStack add = new FluidStack(prop.outputFluid, fluidOutput);
                  if (this.outputTank.canFillFluidType(add)) {
                     FluidStack draincoolant;
                     if (fluidOutput < outputroom) {
                        this.EmitHeatbuffer = (int)(huOtput % prop.huPerMB / huOutputModifier);
                        this.EmitHeat = (int)(huOtput / huOutputModifier);
                        draincoolant = this.inputTank.drainInternal(fluidOutput, false);
                     } else {
                        this.EmitHeat = outputroom * prop.huPerMB;
                        draincoolant = this.inputTank.drainInternal(outputroom, false);
                     }

                     if (draincoolant != null) {
                        this.EmitHeat = draincoolant.getAmount() * prop.huPerMB;
                        huOtput -= this.inputTank.drainInternal(draincoolant.getAmount(), true).getAmount() * prop.huPerMB;
                        this.outputTank.fillInternal(new FluidStack(prop.outputFluid, draincoolant.getAmount()), true);
                     } else {
                        this.EmitHeat = 0;
                     }
                  }
               } else {
                  this.EmitHeat = 0;
               }

               this.addHeat((int)(huOtput / huOutputModifier));
            }

            if (this.calculateHeatEffects()) {
               return;
            }

            this.setActive(this.heat >= 1000 || this.output > 0.0F);
            this.setChanged();
         }

         IC2.network.get(true).updateTileEntityField(this, "output");
      }
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   protected void updateEntityClient() {
      super.updateEntityClient();
      showHeatEffects(this.getLevel(), this.getBlockPos(), this.heat);
   }

   public static void showHeatEffects(Level world, BlockPos pos, int heat) {
      RandomSource rnd = world.getRandom();
      if (rnd.nextInt(8) == 0) {
         int puffs = heat / 1000;
         if (puffs > 0) {
            puffs = rnd.nextInt(puffs);

            for (int n = 0; n < puffs; n++) {
               world.addParticle(ParticleTypes.SMOKE, pos.getX() + rnd.nextFloat(), pos.getY() + 0.95F, pos.getZ() + rnd.nextFloat(), 0.0, 0.0, 0.0);
            }

            puffs -= rnd.nextInt(4) + 3;

            for (int n = 0; n < puffs; n++) {
               world.addParticle(ParticleTypes.FLAME, pos.getX() + rnd.nextFloat(), pos.getY() + 1, pos.getZ() + rnd.nextFloat(), 0.0, 0.0, 0.0);
            }
         }
      }
   }

   public void dropAllUnfittingStuff() {
      for (int i = 0; i < this.reactorSlot.size(); i++) {
         ItemStack stack = this.reactorSlot.get(i);
         if (stack != null && !this.isUsefulItem(stack, false)) {
            this.reactorSlot.put(i, null);
            this.eject(stack);
         }
      }

      for (int ix = this.reactorSlot.size(); ix < this.reactorSlot.rawSize(); ix++) {
         ItemStack stack = this.reactorSlot.get(ix);
         this.reactorSlot.put(ix, null);
         this.eject(stack);
      }
   }

   public boolean isUsefulItem(ItemStack stack, boolean forInsertion) {
      Item item = stack.getItem();
      if (item == null) {
         return false;
      } else {
         return forInsertion
               && this.fluidCooled
               && item.getClass() == ItemReactorHeatStorage.class
               && ((ItemReactorHeatStorage)item).getCustomDamage(stack) > 0
            ? false
            : item instanceof IBaseReactorComponent && (!forInsertion || ((IBaseReactorComponent)item).canBePlacedIn(stack, this));
      }
   }

   public void eject(ItemStack drop) {
      if (IC2.platform.isSimulating() && drop != null) {
         StackUtil.dropAsEntity(this.getLevel(), this.getBlockPos(), drop);
      }
   }

   public boolean calculateHeatEffects() {
      if (this.heat >= 4000 && IC2.platform.isSimulating() && !(ConfigUtil.getFloat(MainConfig.get(), "protection/reactorExplosionPowerLimit") <= 0.0F)) {
         float power = (float)this.heat / this.maxHeat;
         if (power >= 1.0F) {
            this.explode();
            return true;
         } else {
            Level world = this.getLevel();
            if (power >= 0.85F && world.getRandom().nextFloat() <= 0.2F * this.hem) {
               BlockPos coord = this.getRandCoord(2);
               BlockState state = world.getBlockState(coord);
               Block block = state.getBlock();
               if (state.isAir()) {
                  world.setBlock(coord, Blocks.FIRE.defaultBlockState(), 3);
               } else if (state.getDestroySpeed(world, coord) >= 0.0F && world.getBlockEntity(coord) == null) {
                  BlockMaterial mat = BlockMaterial.of(state);
                  if (mat != BlockMaterial.ROCK
                     && mat != BlockMaterial.IRON
                     && mat != BlockMaterial.LAVA
                     && mat != BlockMaterial.GROUND
                     && mat != BlockMaterial.CLAY) {
                     world.setBlock(coord, Blocks.FIRE.defaultBlockState(), 3);
                  } else {
                     world.setBlock(coord, Blocks.LAVA.defaultBlockState(), 3);
                  }
               }
            }

            if (power >= 0.7F) {
               for (LivingEntity entity : world.getEntitiesOfClass(
                  LivingEntity.class,
                  new AABB(
                     this.getBlockPos().getX() - 3,
                     this.getBlockPos().getY() - 3,
                     this.getBlockPos().getZ() - 3,
                     this.getBlockPos().getX() + 4,
                     this.getBlockPos().getY() + 4,
                     this.getBlockPos().getZ() + 4
                  )
               )) {
                  WorldQueries.hurt(entity, IC2DamageSource.radiation, (int)(world.getRandom().nextInt(4) * this.hem));
               }
            }

            if (power >= 0.5F && world.getRandom().nextFloat() <= this.hem) {
               BlockPos coord = this.getRandCoord(2);
               BlockState state = world.getBlockState(coord);
               if (BlockMaterial.of(state) == BlockMaterial.WATER) {
                  world.removeBlock(coord, false);
               }
            }

            if (power >= 0.4F && world.getRandom().nextFloat() <= this.hem) {
               BlockPos coord = this.getRandCoord(2);
               if (world.getBlockEntity(coord) == null) {
                  BlockState state = world.getBlockState(coord);
                  BlockMaterial mat = BlockMaterial.of(state);
                  if (mat == BlockMaterial.WOOD || mat == BlockMaterial.LEAVES || mat == BlockMaterial.CLOTH) {
                     world.setBlock(coord, Blocks.FIRE.defaultBlockState(), 3);
                  }
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   public BlockPos getRandCoord(int radius) {
      if (radius <= 0) {
         return null;
      } else {
         Level world = this.getLevel();

         BlockPos ret;
         do {
            ret = this.getBlockPos()
               .offset(
                  world.getRandom().nextInt(2 * radius + 1) - radius,
                  world.getRandom().nextInt(2 * radius + 1) - radius,
                  world.getRandom().nextInt(2 * radius + 1) - radius
               );
         } while (ret.equals(this.getBlockPos()));

         return ret;
      }
   }

   public void processChambers() {
      int size = this.getReactorSize();

      for (int pass = 0; pass < 2; pass++) {
         for (int y = 0; y < 6; y++) {
            for (int x = 0; x < size; x++) {
               ItemStack stack = this.reactorSlot.get(x, y);
               if (stack != null && stack.getItem() instanceof IReactorComponent) {
                  IReactorComponent comp = (IReactorComponent)stack.getItem();
                  comp.processChamber(stack, this, x, y, pass == 0);
               }
            }
         }
      }
   }

   @Override
   public boolean produceEnergy() {
      return this.redstone.hasRedstoneInput() && ConfigUtil.getFloat(MainConfig.get(), "balance/energy/generator/nuclear") > 0.0F;
   }

   public int getReactorSize() {
      Level world = this.getLevel();
      if (world == null) {
         return 9;
      } else {
         int cols = 3;

         for (Direction dir : Direction.values()) {
            BlockEntity target = world.getBlockEntity(this.getBlockPos().relative(dir));
            if (target instanceof TileEntityReactorChamberElectric) {
               cols++;
            }
         }

         return cols;
      }
   }

   private boolean isFullSize() {
      return this.getReactorSize() == 9;
   }

   @Override
   public int getTickRate() {
      return 20;
   }

   @Override
   protected boolean onActivated(Player player, InteractionHand hand, Direction side, float hitX, float hitY, float hitZ) {
      return StackUtil.checkItemEquality(StackUtil.get(player, hand), BlockName.te.getItemStack(TeBlock.reactor_chamber))
         ? false
         : super.onActivated(player, hand, side, hitX, hitY, hitZ);
   }

   @Override
   public ContainerBase<TileEntityNuclearReactorElectric> getGuiContainer(Player player) {
      return new ContainerNuclearReactor(player, this);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Screen getGui(Player player, boolean isAdmin) {
      return new GuiNuclearReactor(new ContainerNuclearReactor(player, this));
   }

   @Override
   public void onGuiClosed(Player player) {
   }

   @Override
   public void onNetworkUpdate(String field) {
      if (field.equals("output")) {
         if (this.output > 0.0F) {
            if (this.lastOutput <= 0.0F) {
               if (this.audioSourceMain == null) {
                  this.audioSourceMain = IC2.audioManager
                     .createSource(
                        this, PositionSpec.Center, "Generators/NuclearReactor/NuclearReactorLoop.ogg", true, false, IC2.audioManager.getDefaultVolume()
                     );
               }

               if (this.audioSourceMain != null) {
                  this.audioSourceMain.play();
               }
            }

            if (this.output < 40.0F) {
               if (this.lastOutput <= 0.0F || this.lastOutput >= 40.0F) {
                  if (this.audioSourceGeiger != null) {
                     this.audioSourceGeiger.remove();
                  }

                  this.audioSourceGeiger = IC2.audioManager
                     .createSource(this, PositionSpec.Center, "Generators/NuclearReactor/GeigerLowEU.ogg", true, false, IC2.audioManager.getDefaultVolume());
                  if (this.audioSourceGeiger != null) {
                     this.audioSourceGeiger.play();
                  }
               }
            } else if (this.output < 80.0F) {
               if (this.lastOutput < 40.0F || this.lastOutput >= 80.0F) {
                  if (this.audioSourceGeiger != null) {
                     this.audioSourceGeiger.remove();
                  }

                  this.audioSourceGeiger = IC2.audioManager
                     .createSource(this, PositionSpec.Center, "Generators/NuclearReactor/GeigerMedEU.ogg", true, false, IC2.audioManager.getDefaultVolume());
                  if (this.audioSourceGeiger != null) {
                     this.audioSourceGeiger.play();
                  }
               }
            } else if (this.output >= 80.0F && this.lastOutput < 80.0F) {
               if (this.audioSourceGeiger != null) {
                  this.audioSourceGeiger.remove();
               }

               this.audioSourceGeiger = IC2.audioManager
                  .createSource(this, PositionSpec.Center, "Generators/NuclearReactor/GeigerHighEU.ogg", true, false, IC2.audioManager.getDefaultVolume());
               if (this.audioSourceGeiger != null) {
                  this.audioSourceGeiger.play();
               }
            }
         } else if (this.lastOutput > 0.0F) {
            if (this.audioSourceMain != null) {
               this.audioSourceMain.stop();
            }

            if (this.audioSourceGeiger != null) {
               this.audioSourceGeiger.stop();
            }
         }

         this.lastOutput = this.output;
      }

      super.onNetworkUpdate(field);
   }

   @Override
   public BlockEntity getCoreTe() {
      return this;
   }

   @Override
   public BlockPos getPosition() {
      return this.getBlockPos();
   }

   @Override
   public Level getWorldObj() {
      return this.getLevel();
   }

   @Override
   public int getHeat() {
      return this.heat;
   }

   @Override
   public void setHeat(int heat) {
      this.heat = heat;
   }

   @Override
   public int addHeat(int amount) {
      this.heat += amount;
      return this.heat;
   }

   @Override
   public ItemStack getItemAt(int x, int y) {
      return x >= 0 && x < this.getReactorSize() && y >= 0 && y < 6 ? this.reactorSlot.get(x, y) : null;
   }

   @Override
   public void setItemAt(int x, int y, ItemStack item) {
      if (x >= 0 && x < this.getReactorSize() && y >= 0 && y < 6) {
         this.reactorSlot.put(x, y, item);
      }
   }

   @Override
   public void explode() {
      float boomPower = 10.0F;
      float boomMod = 1.0F;

      for (int i = 0; i < this.reactorSlot.size(); i++) {
         ItemStack stack = this.reactorSlot.get(i);
         if (stack != null && stack.getItem() instanceof IReactorComponent) {
            float f = ((IReactorComponent)stack.getItem()).influenceExplosion(stack, this);
            if (f > 0.0F && f < 1.0F) {
               boomMod *= f;
            } else {
               boomPower += f;
            }
         }

         this.reactorSlot.put(i, null);
      }

      boomPower *= this.hem * boomMod;
      IC2.log
         .log(
            LogCategory.PlayerActivity,
            org.apache.logging.log4j.Level.INFO,
            "Nuclear Reactor at %s melted (raw explosion power %f)",
            Util.formatPosition(this),
            boomPower
         );
      boomPower = Math.min(boomPower, ConfigUtil.getFloat(MainConfig.get(), "protection/reactorExplosionPowerLimit"));
      Level world = this.getLevel();

      for (Direction dir : Direction.values()) {
         BlockEntity target = world.getBlockEntity(this.getBlockPos().relative(dir));
         if (target instanceof TileEntityReactorChamberElectric) {
            world.removeBlock(target.getBlockPos(), false);
         }
      }

      world.removeBlock(this.getBlockPos(), false);
      ExplosionIC2 explosion = new ExplosionIC2(world, null, this.getBlockPos(), boomPower, 0.01F, ExplosionIC2.Type.Nuclear);
      explosion.doExplosion();
   }

   @Override
   public void addEmitHeat(int heat) {
      this.EmitHeatbuffer += heat;
   }

   @Override
   public int getMaxHeat() {
      return this.maxHeat;
   }

   @Override
   public void setMaxHeat(int newMaxHeat) {
      this.maxHeat = newMaxHeat;
   }

   @Override
   public float getHeatEffectModifier() {
      return this.hem;
   }

   @Override
   public void setHeatEffectModifier(float newHEM) {
      this.hem = newHEM;
   }

   @Override
   public float getReactorEnergyOutput() {
      return this.output;
   }

   @Override
   public float addOutput(float energy) {
      return this.output += energy;
   }

   @Override
   public boolean isFluidCooled() {
      return this.fluidCooled;
   }

   private void createChamberRedstoneLinks() {
      Level world = this.getLevel();

      for (Direction facing : Direction.values()) {
         BlockPos cPos = this.getBlockPos().relative(facing);
         if (world.getBlockEntity(cPos) instanceof TileEntityReactorChamberElectric chamber) {
            if (chamber.redstone.isLinked() && chamber.redstone.getLinkReceiver() != this.redstone) {
               chamber.destoryChamber(true);
            } else {
               chamber.redstone.linkTo(this.redstone);
            }
         }
      }
   }

   private void createCasingRedstoneLinks() {
      WorldSearchUtil.findTileEntities(this.getLevel(), this.getBlockPos(), 2, new WorldSearchUtil.ITileEntityResultHandler() {
         {
            Objects.requireNonNull(TileEntityNuclearReactorElectric.this);
         }

         @Override
         public boolean onMatch(BlockEntity te) {
            if (te instanceof TileEntityReactorRedstonePort) {
               ((TileEntityReactorRedstonePort)te).redstone.linkTo(TileEntityNuclearReactorElectric.this.redstone);
            }

            return false;
         }
      });
   }

   private void removeCasingRedstoneLinks() {
      for (Redstone rs : this.redstone.getLinkedOrigins()) {
         if (rs.getParent() instanceof TileEntityReactorRedstonePort) {
            rs.unlinkOutbound();
         }
      }
   }

   private void enableFluidMode() {
      if (this.addedToEnergyNet) {
         NeoForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
         this.addedToEnergyNet = false;
      }

      this.createCasingRedstoneLinks();
      this.openTanks();
   }

   private void disableFluidMode() {
      if (!this.addedToEnergyNet) {
         this.refreshChambers();
         NeoForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
         this.addedToEnergyNet = true;
      }

      this.removeCasingRedstoneLinks();
      this.closeTanks();
   }

   private void openTanks() {
      this.fluids.changeConnectivity(this.inputTank, InvSlot.Access.I, InvSlot.InvSide.ANY);
      this.fluids.changeConnectivity(this.outputTank, InvSlot.Access.O, InvSlot.InvSide.ANY);
   }

   private void closeTanks() {
      this.fluids.changeConnectivity(this.inputTank, InvSlot.Access.NONE, InvSlot.InvSide.ANY);
      this.fluids.changeConnectivity(this.outputTank, InvSlot.Access.NONE, InvSlot.InvSide.ANY);
   }

   private boolean isFluidReactor() {
      if (!this.isFullSize()) {
         return false;
      } else if (!this.hasFluidChamber()) {
         return false;
      } else {
         int range = 2;
         final MutableBoolean foundConflict = new MutableBoolean();
         WorldSearchUtil.findTileEntities(this.getLevel(), this.getBlockPos(), 4, new WorldSearchUtil.ITileEntityResultHandler() {
            {
               Objects.requireNonNull(TileEntityNuclearReactorElectric.this);
            }

            @Override
            public boolean onMatch(BlockEntity te) {
               if (!(te instanceof TileEntityNuclearReactorElectric)) {
                  return false;
               } else if (te == TileEntityNuclearReactorElectric.this) {
                  return false;
               } else {
                  TileEntityNuclearReactorElectric reactor = (TileEntityNuclearReactorElectric)te;
                  if (reactor.isFullSize() && reactor.hasFluidChamber()) {
                     foundConflict.setTrue();
                     return true;
                  } else {
                     return false;
                  }
               }
            }
         });
         return !foundConflict.getValue();
      }
   }

   private boolean hasFluidChamber() {
      int range = 2;
      PathNavigationRegion cache = new PathNavigationRegion(this.getLevel(), this.getBlockPos().offset(-2, -2, -2), this.getBlockPos().offset(2, 2, 2));
      MutableBlockPos cPos = new MutableBlockPos();

      for (int i = 0; i < 2; i++) {
         int y = this.getBlockPos().getY() + 2 * (i * 2 - 1);

         for (int z = this.getBlockPos().getZ() - 2; z <= this.getBlockPos().getZ() + 2; z++) {
            for (int x = this.getBlockPos().getX() - 2; x <= this.getBlockPos().getX() + 2; x++) {
               cPos.set(x, y, z);
               if (!isFluidChamberBlock(cache, cPos)) {
                  return false;
               }
            }
         }
      }

      for (int i = 0; i < 2; i++) {
         int z = this.getBlockPos().getZ() + 2 * (i * 2 - 1);

         for (int y = this.getBlockPos().getY() - 2 + 1; y <= this.getBlockPos().getY() + 2 - 1; y++) {
            for (int xx = this.getBlockPos().getX() - 2; xx <= this.getBlockPos().getX() + 2; xx++) {
               cPos.set(xx, y, z);
               if (!isFluidChamberBlock(cache, cPos)) {
                  return false;
               }
            }
         }
      }

      for (int i = 0; i < 2; i++) {
         int xxx = this.getBlockPos().getX() + 2 * (i * 2 - 1);

         for (int y = this.getBlockPos().getY() - 2 + 1; y <= this.getBlockPos().getY() + 2 - 1; y++) {
            for (int z = this.getBlockPos().getZ() - 2 + 1; z <= this.getBlockPos().getZ() + 2 - 1; z++) {
               cPos.set(xxx, y, z);
               if (!isFluidChamberBlock(cache, cPos)) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   private static boolean isFluidChamberBlock(BlockGetter world, BlockPos pos) {
      BlockState state = world.getBlockState(pos);
      if (state == BlockName.resource.getBlockState(ResourceBlock.reactor_vessel)) {
         return true;
      } else {
         BlockEntity te = world.getBlockEntity(pos);
         return te == null ? false : te instanceof IReactorChamber && ((IReactorChamber)te).isWall();
      }
   }

   @Override
   public double getGuiValue(String name) {
      if ("heat".equals(name)) {
         return this.maxHeat == 0 ? 0.0 : (double)this.heat / this.maxHeat;
      } else {
         throw new IllegalArgumentException("Invalid value: " + name);
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

   public Ic2FluidTank getinputtank() {
      return this.inputTank;
   }

   public Ic2FluidTank getoutputtank() {
      return this.outputTank;
   }

   @Override
   public int getMaxStackSize() {
      return 1;
   }
}
