package ic2.core.block.steam;

import ic2.core.ContainerBase;
import ic2.core.IC2;
import ic2.core.IHasGui;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.gui.dynamic.DynamicContainer;
import ic2.core.gui.dynamic.DynamicGui;
import ic2.core.gui.dynamic.GuiParser;
import ic2.core.gui.dynamic.IGuiValueProvider;
import ic2.core.network.GuiSynced;
import ic2.core.recipe.dynamic.DynamicRecipe;
import ic2.core.recipe.dynamic.DynamicRecipeManager;
import ic2.core.recipe.dynamic.RecipeOutputFluidStack;
import ic2.core.recipe.dynamic.RecipeOutputIngredient;
import ic2.core.recipe.dynamic.RecipeOutputItemStack;
import ic2.core.ref.BlockName;
import ic2.core.ref.FluidName;
import ic2.core.ref.ItemName;
import ic2.core.util.ItemStacks;
import ic2.core.util.ParticleUtil;
import ic2.core.util.StackUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;

public class TileEntityCokeKiln extends TileEntityInventory implements IMultiBlockController, IHasGui, IGuiValueProvider {
   protected int tickRate = 20;
   protected int updateTicker;
   protected boolean isFormed = false;
   protected final InvSlotOutput outputSlot;
   public static DynamicRecipeManager recipeManager;
   protected int progress = 0;
   protected int operationLength = 0;
   protected DynamicRecipe recipe = null;
   @GuiSynced
   protected float guiProgress;

   public TileEntityCokeKiln() {
      this.updateTicker = IC2.random.nextInt(this.tickRate);
      this.outputSlot = new InvSlotOutput(this, "output", 1, InvSlot.InvSide.ANY);
   }

   public static void init() {
      recipeManager = new DynamicRecipeManager();
      recipeManager.createRecipe()
         .withInput("logWood")
         .withOutput(ItemStacks.create(Items.COAL, 1, 1))
         .withOutput(new FluidStack(FluidName.creosote.getInstance(), 250))
         .withOperationDurationTicks(1800)
         .register();
      recipeManager.createRecipe()
         .withInput(ItemStacks.create(Items.COAL, 1, 0))
         .withOutput(ItemName.coke.getItemStack())
         .withOutput(new FluidStack(FluidName.creosote.getInstance(), 500))
         .withOperationDurationTicks(1800)
         .register();
   }

   @Override
   public void loadAdditional(CompoundTag nbt) {
      super.loadAdditional(nbt);
      this.progress = nbt.getIntOr("progress", 0);
      this.operationLength = nbt.getIntOr("operationLength", 0);
   }

   @Override
   public CompoundTag saveAdditional(CompoundTag nbt) {
      super.saveAdditional(nbt);
      nbt.putInt("progress", this.progress);
      nbt.putInt("operationLength", this.operationLength);
      return nbt;
   }

   @Override
   protected void updateEntityServer() {
      super.updateEntityServer();
      if (this.updateTicker++ % this.tickRate == 0) {
         this.isFormed = this.hasValidStructure();
         if (!this.isFormed) {
            this.progress = 0;
            this.guiProgress = 0.0F;
            this.setActive(false);
         } else {
            boolean needsInventoryUpdate = false;
            if (this.canWork()) {
               this.setActive(true);
               if (this.progress == 0) {
                  needsInventoryUpdate = true;
               }

               int progressNeeded = this.recipe.getOperationDuration();
               if (this.progress < progressNeeded) {
                  this.progress += 20;
               }

               if (this.progress >= progressNeeded) {
                  this.finishWork();
                  needsInventoryUpdate = true;
               }
            } else {
               this.setActive(false);
            }

            if (this.progress != 0 && this.operationLength != 0) {
               this.guiProgress = (float)this.progress / this.operationLength;
            } else {
               this.guiProgress = 0.0F;
            }

            if (needsInventoryUpdate) {
               super.setChanged();
            }
         }
      }
   }

   protected boolean canWork() {
      BlockPos hatchPos = new BlockPos(
         this.getBlockPos().getX() + -this.getFacing().getStepX(), this.getBlockPos().getY() + 1, this.getBlockPos().getZ() + -this.getFacing().getStepZ()
      );
      BlockEntity hatch = this.getLevel().getBlockEntity(hatchPos);
      if (!(hatch instanceof TileEntityCokeKilnHatch)) {
         return false;
      } else {
         ItemStack input = ((TileEntityCokeKilnHatch)hatch).inventory.get();
         if (input.isEmpty()) {
            return false;
         } else {
            if (this.recipe != null) {
               boolean canUse = recipeManager.apply(this.recipe, new ItemStack[]{input}, new FluidStack[0], true);
               if (!canUse) {
                  this.reset();
               }
            }

            DynamicRecipe maybeRecipe = this.recipe;
            if (maybeRecipe != null) {
               for (RecipeOutputIngredient<?> entry : this.recipe.getOutputIngredients()) {
                  if (entry instanceof RecipeOutputItemStack) {
                     if (!this.outputSlot.canAdd((ItemStack)entry.ingredient)) {
                        return false;
                     }
                  } else if (entry instanceof RecipeOutputFluidStack) {
                     BlockPos gratePos = new BlockPos(
                        this.getBlockPos().getX() + -this.getFacing().getStepX(),
                        this.getBlockPos().getY() - 1,
                        this.getBlockPos().getZ() + -this.getFacing().getStepZ()
                     );
                     BlockEntity grate = this.getLevel().getBlockEntity(gratePos);
                     if (!(grate instanceof TileEntityCokeKilnGrate)) {
                        return false;
                     }

                     if (((TileEntityCokeKilnGrate)grate).fluidTank.fillInternal((FluidStack)entry.ingredient, false)
                        < ((FluidStack)entry.ingredient).getAmount()) {
                        return false;
                     }
                  }
               }

               return true;
            } else {
               maybeRecipe = recipeManager.findRecipe(new ItemStack[]{input}, new FluidStack[0]);
               if (maybeRecipe == null) {
                  return false;
               } else {
                  this.updateRecipe(maybeRecipe);

                  for (RecipeOutputIngredient<?> entryx : this.recipe.getOutputIngredients()) {
                     if (entryx instanceof RecipeOutputItemStack) {
                        if (!this.outputSlot.canAdd((ItemStack)entryx.ingredient)) {
                           return false;
                        }
                     } else if (entryx instanceof RecipeOutputFluidStack) {
                        BlockPos gratePosx = new BlockPos(
                           this.getBlockPos().getX() + -this.getFacing().getStepX(),
                           this.getBlockPos().getY() - 1,
                           this.getBlockPos().getZ() + -this.getFacing().getStepZ()
                        );
                        BlockEntity gratex = this.getLevel().getBlockEntity(gratePosx);
                        if (!(gratex instanceof TileEntityCokeKilnGrate)) {
                           return false;
                        }

                        if (((TileEntityCokeKilnGrate)gratex).fluidTank.fillInternal((FluidStack)entryx.ingredient, false)
                           < ((FluidStack)entryx.ingredient).getAmount()) {
                           return false;
                        }
                     }
                  }

                  return true;
               }
            }
         }
      }
   }

   protected void finishWork() {
      BlockPos hatchPos = new BlockPos(
         this.getBlockPos().getX() + -this.getFacing().getStepX(), this.getBlockPos().getY() + 1, this.getBlockPos().getZ() + -this.getFacing().getStepZ()
      );
      BlockEntity hatch = this.getLevel().getBlockEntity(hatchPos);
      if (hatch instanceof TileEntityCokeKilnHatch) {
         InvSlot inventory = ((TileEntityCokeKilnHatch)hatch).inventory;
         if (!inventory.get().isEmpty()) {
            recipeManager.apply(this.recipe, new ItemStack[]{inventory.get()}, new FluidStack[0], false);
            List<ItemStack> itemOutputs = new ArrayList<>();
            List<FluidStack> fluidOutputs = new ArrayList<>();

            for (RecipeOutputIngredient<?> entry : this.recipe.getOutputIngredients()) {
               if (entry instanceof RecipeOutputItemStack) {
                  itemOutputs.add(StackUtil.copy((ItemStack)entry.ingredient));
               } else if (entry instanceof RecipeOutputFluidStack) {
                  fluidOutputs.add(((FluidStack)entry.ingredient).copy());
               }
            }

            for (ItemStack stack : itemOutputs) {
               int amount = this.outputSlot.add(StackUtil.copy(stack));
               stack.shrink(amount);
            }

            itemOutputs.clear();
            BlockPos gratePos = new BlockPos(
               this.getBlockPos().getX() + -this.getFacing().getStepX(),
               this.getBlockPos().getY() - 1,
               this.getBlockPos().getZ() + -this.getFacing().getStepZ()
            );
            BlockEntity grate = this.getLevel().getBlockEntity(gratePos);
            if (grate instanceof TileEntityCokeKilnGrate) {
               for (FluidStack stack : fluidOutputs) {
                  int amount = ((TileEntityCokeKilnGrate)grate).fluidTank.fillInternal(stack, true);
                  stack.setAmount(stack.getAmount() - amount);
               }
            }

            fluidOutputs.clear();
            this.progress = 0;
         }
      }
   }

   protected void updateRecipe(DynamicRecipe recipe) {
      this.operationLength = recipe.getOperationDuration();
      this.recipe = recipe;
   }

   protected void reset() {
      this.progress = 0;
      this.operationLength = 0;
      this.recipe = null;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   protected void updateEntityClient() {
      super.updateEntityClient();
      if (this.getActive()) {
         Level world = this.getLevel();
         ParticleUtil.showFlames(world, this.getBlockPos(), this.getFacing());
         if (world.getRandom().nextDouble() < 0.1) {
            world.playLocalSound(
               this.getBlockPos().getX() + 0.5,
               this.getBlockPos().getY() + 0.5,
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

   @Override
   public boolean hasValidStructure() {
      int range = 2;
      PathNavigationRegion cache = new PathNavigationRegion(this.getLevel(), this.getBlockPos().offset(-2, -2, -2), this.getBlockPos().offset(2, 2, 2));
      MutableBlockPos cPos = new MutableBlockPos();

      for (int x = -1; x <= 1; x++) {
         for (int z = -1; z <= 1; z++) {
            cPos.set(
               this.getBlockPos().getX() + (x - this.getFacing().getStepX()),
               this.getBlockPos().getY() - 1,
               this.getBlockPos().getZ() + (z - this.getFacing().getStepZ())
            );
            if (x == 0 && z == 0) {
               BlockEntity tileEntity = cache.getBlockEntity(cPos);
               if (tileEntity == null) {
                  return false;
               }

               if (!(tileEntity instanceof TileEntityCokeKilnGrate)) {
                  return false;
               }
            } else {
               BlockState state = cache.getBlockState(cPos);
               if (state.getBlock() != BlockName.refractory_bricks.getInstance()) {
                  return false;
               }
            }
         }
      }

      for (int x = -1; x <= 1; x++) {
         for (int zx = -1; zx <= 1; zx++) {
            cPos.set(
               this.getBlockPos().getX() + (x - this.getFacing().getStepX()),
               this.getBlockPos().getY(),
               this.getBlockPos().getZ() + (zx - this.getFacing().getStepZ())
            );
            if (x == 0 && zx == 0) {
               BlockState state = cache.getBlockState(cPos);
               if (state.getBlock() != Blocks.AIR) {
                  return false;
               }
            } else if (this.getBlockPos().getX() == cPos.getX() && this.getBlockPos().getZ() == cPos.getZ()) {
               BlockEntity tileEntityx = cache.getBlockEntity(cPos);
               if (tileEntityx == null) {
                  return false;
               }

               if (tileEntityx != this) {
                  return false;
               }
            } else {
               BlockState state = cache.getBlockState(cPos);
               if (state.getBlock() != BlockName.refractory_bricks.getInstance()) {
                  return false;
               }
            }
         }
      }

      for (int x = -1; x <= 1; x++) {
         for (int zxx = -1; zxx <= 1; zxx++) {
            cPos.set(
               this.getBlockPos().getX() + (x - this.getFacing().getStepX()),
               this.getBlockPos().getY() + 1,
               this.getBlockPos().getZ() + (zxx - this.getFacing().getStepZ())
            );
            if (x == 0 && zxx == 0) {
               BlockEntity tileEntityxx = cache.getBlockEntity(cPos);
               if (tileEntityxx == null) {
                  return false;
               }

               if (!(tileEntityxx instanceof TileEntityCokeKilnHatch)) {
                  return false;
               }
            } else {
               BlockState state = cache.getBlockState(cPos);
               if (state.getBlock() != BlockName.refractory_bricks.getInstance()) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   @Override
   public boolean isFormed() {
      return this.isFormed;
   }

   @Override
   public ContainerBase<TileEntityCokeKiln> getGuiContainer(Player player) {
      return DynamicContainer.create(this, player, GuiParser.parse(this.teBlock));
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Screen getGui(Player player, boolean isAdmin) {
      return DynamicGui.<TileEntityCokeKiln>create(this, player, GuiParser.parse(this.teBlock));
   }

   @Override
   public void onGuiClosed(Player player) {
   }

   @Override
   public double getGuiValue(String name) {
      if (name.equals("progress")) {
         return this.guiProgress;
      } else {
         throw new IllegalArgumentException(this.getClass().getSimpleName() + " Cannot get value for " + name);
      }
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void addInformation(ItemStack stack, List<String> info, TooltipFlag advanced) {
      info.add("");
      info.add("MultiBlock Structure:");
      info.add("");
      info.add(" Bottom Layer - 3x3 of Refractory Blocks with a Coke Kiln Grate in the centre");
      info.add("");
      info.add(" Middle Layer - 3x3 of Refractory Blocks with a hollow centre and this block in the middle of one of the sides");
      info.add("");
      info.add(" Top Layer - 3x3 of Refractory Blocks with a Coke Kiln Hatch in the centre");
      info.add("");
   }
}
