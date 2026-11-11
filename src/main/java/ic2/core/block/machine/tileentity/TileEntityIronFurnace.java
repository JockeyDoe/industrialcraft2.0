package ic2.core.block.machine.tileentity;

import ic2.api.network.INetworkClientTileEntityEventListener;
import ic2.api.recipe.MachineRecipeResult;
import ic2.core.ContainerBase;
import ic2.core.IC2;
import ic2.core.IHasGui;
import ic2.core.audio.AudioSource;
import ic2.core.audio.PositionSpec;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.invslot.InvSlotConsumableFuel;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.block.invslot.InvSlotProcessableSmelting;
import ic2.core.gui.dynamic.DynamicContainer;
import ic2.core.gui.dynamic.DynamicGui;
import ic2.core.gui.dynamic.GuiParser;
import ic2.core.gui.dynamic.IGuiValueProvider;
import ic2.core.network.GuiSynced;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class TileEntityIronFurnace extends TileEntityInventory implements IHasGui, IGuiValueProvider, INetworkClientTileEntityEventListener {
   public final InvSlotProcessableSmelting inputSlot;
   public final InvSlotOutput outputSlot;
   public final InvSlotConsumableFuel fuelSlot;
   protected AudioSource audioSource;
   @GuiSynced
   public int fuel = 0;
   @GuiSynced
   public int totalFuel = 0;
   @GuiSynced
   public short progress = 0;
   protected double xp = 0.0;
   public static final short operationLength = 160;

   public TileEntityIronFurnace() {
      this.inputSlot = new InvSlotProcessableSmelting(this, "input", 1);
      this.outputSlot = new InvSlotOutput(this, "output", 1);
      this.fuelSlot = new InvSlotConsumableFuel(this, "fuel", 1, true);
   }

   @Override
   protected void onUnloaded() {
      if (IC2.platform.isRendering() && this.audioSource != null) {
         IC2.audioManager.removeSources(this);
         this.audioSource = null;
      }

      super.onUnloaded();
   }

   @Override
   public void loadAdditional(CompoundTag nbt) {
      super.loadAdditional(nbt);
      this.fuel = nbt.getIntOr("fuel", 0);
      this.totalFuel = nbt.getIntOr("totalFuel", 0);
      this.progress = nbt.getShortOr("progress", (short)0);
      this.xp = nbt.getDoubleOr("xp", 0.0);
   }

   @Override
   public CompoundTag saveAdditional(CompoundTag nbt) {
      super.saveAdditional(nbt);
      nbt.putInt("fuel", this.fuel);
      nbt.putInt("totalFuel", this.totalFuel);
      nbt.putShort("progress", this.progress);
      nbt.putDouble("xp", this.xp);
      return nbt;
   }

   @Override
   protected void updateEntityServer() {
      super.updateEntityServer();
      boolean needsInvUpdate = false;
      if (this.fuel <= 0 && this.canOperate()) {
         this.fuel = this.totalFuel = this.fuelSlot.consumeFuel();
         if (this.fuel > 0) {
            needsInvUpdate = true;
         }
      }

      if (this.fuel > 0 && this.canOperate()) {
         this.progress++;
         if (this.progress >= 160) {
            this.progress = 0;
            this.operate();
            needsInvUpdate = true;
         }
      } else {
         this.progress = 0;
      }

      if (this.fuel > 0) {
         this.fuel--;
         this.setActive(true);
      } else {
         this.setActive(false);
      }

      if (needsInvUpdate) {
         this.setChanged();
      }
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   protected void updateEntityClient() {
      super.updateEntityClient();
      if (this.getActive()) {
         Level world = this.getLevel();
         showFlames(world, this.getBlockPos(), this.getFacing());
         if (world.getRandom().nextDouble() < 0.1) {
            world.playLocalSound(
               this.getBlockPos().getX() + 0.5,
               this.getBlockPos().getY(),
               this.getBlockPos().getZ() + 0.5,
               SoundEvents.FURNACE_FIRE_CRACKLE,
               SoundSource.BLOCKS,
               1.0F,
               1.0F,
               false
            );
         }
      }
   }

   public static void showFlames(Level world, BlockPos pos, Direction facing) {
      if (world.getRandom().nextInt(8) == 0) {
         double width = 0.625;
         double height = 0.375;
         double depthOffset = 0.02;
         double x = pos.getX() + (facing.getStepX() * 1.04 + 1.0) / 2.0;
         double y = pos.getY() + world.getRandom().nextFloat() * 0.375;
         double z = pos.getZ() + (facing.getStepZ() * 1.04 + 1.0) / 2.0;
         if (facing.getAxis() == Axis.X) {
            z += world.getRandom().nextFloat() * 0.625 - 0.3125;
         } else {
            x += world.getRandom().nextFloat() * 0.625 - 0.3125;
         }

         world.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
         world.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.0, 0.0);
      }
   }

   public static double spawnXP(Player player, double xp) {
      Level world = player.level();
      long balls = (long)Math.floor(xp);

      while (balls > 0L) {
         int amount;
         if (balls < 2477L) {
            amount = ExperienceOrb.getExperienceValue((int)balls);
         } else {
            amount = 2477;
         }

         balls -= amount;
         world.addFreshEntity(new ExperienceOrb(world, player.getX(), player.getY() + 0.5, player.getZ() + 0.5, amount));
      }

      return xp - Math.floor(xp);
   }

   private void operate() {
      MachineRecipeResult<ItemStack, ItemStack, ItemStack> result = this.inputSlot.process();
      ItemStack output = result.getOutput();
      this.outputSlot.add(output);
      this.inputSlot.consume(result);
      this.xp = this.xp + result.getRecipe().getMetaData().getFloatOr("experience", 0.0F);
   }

   private boolean canOperate() {
      MachineRecipeResult<ItemStack, ItemStack, ItemStack> result = this.inputSlot.process();
      return result == null ? false : this.outputSlot.canAdd(result.getOutput());
   }

   public double getProgress() {
      return this.progress / 160.0;
   }

   public double getFuelRatio() {
      return this.fuel <= 0 ? 0.0 : (double)this.fuel / this.totalFuel;
   }

   @Override
   public ContainerBase<TileEntityIronFurnace> getGuiContainer(Player player) {
      return DynamicContainer.create(this, player, GuiParser.parse(this.teBlock));
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Screen getGui(Player player, boolean isAdmin) {
      return DynamicGui.<TileEntityIronFurnace>create(this, player, GuiParser.parse(this.teBlock));
   }

   @Override
   public void onGuiClosed(Player player) {
   }

   @Override
   public double getGuiValue(String name) {
      if (name.equals("fuel")) {
         return this.fuel == 0 ? 0.0 : (double)this.fuel / this.totalFuel;
      } else if (name.equals("progress")) {
         return this.progress == 0 ? 0.0 : this.progress / 160.0;
      } else {
         throw new IllegalArgumentException();
      }
   }

   @Override
   public void onNetworkEvent(Player player, int event) {
      if (event == 0) {
         assert !this.getLevel().isClientSide();

         this.xp = spawnXP(player, this.xp);
      }
   }

   @Override
   public void onNetworkUpdate(String field) {
      if (field.equals("active")) {
         if (this.audioSource == null) {
            this.audioSource = IC2.audioManager
               .createSource(this, PositionSpec.Center, "Machines/IronFurnaceOp.ogg", true, false, IC2.audioManager.getDefaultVolume());
         }

         if (this.getActive()) {
            if (this.audioSource != null) {
               this.audioSource.play();
            }
         } else if (this.audioSource != null) {
            this.audioSource.stop();
         }
      }

      super.onNetworkUpdate(field);
   }
}
