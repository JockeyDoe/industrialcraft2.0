package ic2.core.block.machine.tileentity;

import com.google.common.base.Predicate;
import ic2.api.network.ClientModifiable;
import ic2.api.network.INetworkClientTileEntityEventListener;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.IUpgradeItem;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.ContainerBase;
import ic2.core.IHasGui;
import ic2.core.block.IInventorySlotHolder;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.block.machine.container.ContainerBatchCrafter;
import ic2.core.block.machine.gui.GuiBatchCrafter;
import ic2.core.gui.dynamic.IGuiValueProvider;
import ic2.core.profile.NotClassic;
import ic2.core.recipe.NativeCrafting;
import ic2.core.util.InventorySlotCrafting;
import ic2.core.util.ItemStacks;
import ic2.core.util.NbtChecks;
import ic2.core.util.StackUtil;
import ic2.core.util.Tuple;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@NotClassic
public class TileEntityBatchCrafter
   extends TileEntityElectricMachine
   implements IHasGui,
   IUpgradableBlock,
   IGuiValueProvider,
   INetworkClientTileEntityEventListener {
   private static final Set<UpgradableProperty> UPGRADES = EnumSet.of(
      UpgradableProperty.Processing,
      UpgradableProperty.Transformer,
      UpgradableProperty.EnergyStorage,
      UpgradableProperty.ItemConsuming,
      UpgradableProperty.ItemProducing
   );
   public static final int defaultTier = 1;
   public static final int defaultEnergyConsume = 2;
   public static final int defaultOperationLength = 40;
   public static final int defaultEnergyStorage = 20000;
   @ClientModifiable
   public final ItemStack[] craftingGrid = new ItemStack[9];
   public final InvSlot[] ingredientsRow = new InvSlot[this.craftingGrid.length];
   public final InvSlotOutput craftingOutput = new InvSlotOutput(this, "output", 1, InvSlot.InvSide.SIDE);
   public final InvSlotOutput containerOutput = new InvSlotOutput(this, "containersOut", this.craftingGrid.length, InvSlot.InvSide.NOTSIDE);
   public final InvSlotUpgrade upgradeSlot = new InvSlotUpgrade(this, "upgrade", 4);
   protected final CraftingContainer crafting = new InventorySlotCrafting(3, 3) {
      {
         Objects.requireNonNull(TileEntityBatchCrafter.this);
      }

      @Override
      protected ItemStack get(int index) {
         return StackUtil.wrapEmpty(TileEntityBatchCrafter.this.craftingGrid[index]);
      }

      @Override
      protected void put(int index, ItemStack stack) {
         TileEntityBatchCrafter.this.craftingGrid[index] = stack;
      }

      @Override
      public boolean isEmpty() {
         for (ItemStack stack : TileEntityBatchCrafter.this.craftingGrid) {
            if (!StackUtil.isEmpty(stack)) {
               return false;
            }
         }

         return true;
      }

      @Override
      public void clearContent() {
         Arrays.fill(TileEntityBatchCrafter.this.craftingGrid, StackUtil.emptyStack);
      }
   };
   public final CraftingContainer ingredients = new InventorySlotCrafting(3, 3) {
      {
         Objects.requireNonNull(TileEntityBatchCrafter.this);
      }

      @Override
      protected ItemStack get(int index) {
         return TileEntityBatchCrafter.this.ingredientsRow[index].get();
      }

      @Override
      protected void put(int index, ItemStack stack) {
         TileEntityBatchCrafter.this.ingredientsRow[index].put(stack);
      }

      @Override
      public boolean isEmpty() {
         for (InvSlot slot : TileEntityBatchCrafter.this.ingredientsRow) {
            if (!slot.isEmpty()) {
               return false;
            }
         }

         return true;
      }

      @Override
      public void clearContent() {
         for (InvSlot slot : TileEntityBatchCrafter.this.ingredientsRow) {
            slot.clear();
         }
      }
   };
   public final Predicate<Tuple.T2<ItemStack, Integer>> acceptPredicate = new Predicate<Tuple.T2<ItemStack, Integer>>() {
      {
         Objects.requireNonNull(TileEntityBatchCrafter.this);
      }

      public boolean apply(Tuple.T2<ItemStack, Integer> input) {
         return TileEntityBatchCrafter.this.ingredientsRow[input.b].accepts(input.a);
      }
   };
   protected Recipe recipe = null;
   protected boolean canCraft = false;
   protected boolean newChange = true;
   protected boolean attemptToBalance = false;
   public ItemStack recipeOutput = StackUtil.emptyStack;
   public int energyConsume;
   public int operationLength;
   public int operationsPerTick;
   protected short progress = 0;
   protected float guiProgress = 0.0F;

   public TileEntityBatchCrafter() {
      super(20000, 1);

      for (int i = 0; i < this.ingredientsRow.length; i++) {
         final int slot = i;
         this.ingredientsRow[slot] = new InvSlot(this, "ingredient[" + slot + "]", InvSlot.Access.I, 1) {
            {
               Objects.requireNonNull(TileEntityBatchCrafter.this);
            }

            @Override
            public boolean accepts(ItemStack ingredient) {
               Recipe recipe = TileEntityBatchCrafter.this.getLevel().isClientSide()
                  ? TileEntityBatchCrafter.this.findRecipe()
                  : TileEntityBatchCrafter.this.recipe;
               if (recipe == null) {
                  return false;
               } else {
                  assert NativeCrafting.matches(recipe, TileEntityBatchCrafter.this.crafting, TileEntityBatchCrafter.this.getLevel());

                  ItemStack recipeStack = TileEntityBatchCrafter.this.craftingGrid[slot];

                  boolean var4;
                  try {
                     TileEntityBatchCrafter.this.craftingGrid[slot] = ingredient;
                     var4 = NativeCrafting.matches(recipe, TileEntityBatchCrafter.this.crafting, TileEntityBatchCrafter.this.getLevel());
                  } finally {
                     TileEntityBatchCrafter.this.craftingGrid[slot] = recipeStack;
                  }

                  return var4;
               }
            }

            @Override
            public void onChanged() {
               super.onChanged();
               TileEntityBatchCrafter.this.ingredientChange(slot);
            }
         };
      }

      this.energyConsume = 2;
      this.operationLength = 40;
      this.operationsPerTick = 1;
      this.comparator.setUpdate(() -> this.progress * 15 / this.operationLength);
   }

   @Override
   public void loadAdditional(CompoundTag nbt) {
      super.loadAdditional(nbt);
      this.progress = nbt.getShortOr("progress", (short)0);
      ListTag grid = NbtChecks.getList(nbt, "grid", 10);

      for (int i = 0; i < grid.size(); i++) {
         CompoundTag contentTag = grid.getCompoundOrEmpty(i);
         this.craftingGrid[contentTag.getByteOr("index", (byte)0)] = ItemStacks.read(contentTag);
      }
   }

   @Override
   public CompoundTag saveAdditional(CompoundTag nbt) {
      super.saveAdditional(nbt);
      nbt.putShort("progress", this.progress);
      ListTag grid = new ListTag();

      for (byte i = 0; i < this.craftingGrid.length; i++) {
         ItemStack content = this.craftingGrid[i];
         if (!StackUtil.isEmpty(content)) {
            CompoundTag contentTag = new CompoundTag();
            contentTag.putByte("index", i);
            ItemStacks.write(content, contentTag);
            grid.add(contentTag);
         }
      }

      nbt.put("grid", grid);
      return nbt;
   }

   protected Recipe findRecipe() {
      Level world = this.getLevel();
      return NativeCrafting.find(this.crafting, world);
   }

   public void matrixChange(int slot) {
      if (this.recipe == null || !NativeCrafting.matches(this.recipe, this.crafting, this.getLevel())) {
         this.recipe = this.findRecipe();
      }

      this.recipeOutput = this.recipe != null ? NativeCrafting.assemble(this.recipe, this.crafting) : StackUtil.emptyStack;
      this.newChange = true;
   }

   public void ingredientChange(int slot) {
      this.newChange = true;
   }

   @Override
   protected void onLoaded() {
      super.onLoaded();
      if (!this.getLevel().isClientSide()) {
         this.setOverclockRates();
         this.matrixChange(-1);
      }
   }

   @Override
   public void setChanged() {
      super.setChanged();
      if (!this.getLevel().isClientSide()) {
         this.setOverclockRates();
         this.attemptToBalance = true;
      }
   }

   @Override
   protected void updateEntityServer() {
      super.updateEntityServer();
      boolean needsInvUpdate = false;
      if (this.attemptToBalance) {
         if (!this.ingredients.isEmpty()) {
            needsInvUpdate |= !StackUtil.balanceStacks(this.ingredients, this.acceptPredicate).b.isEmpty();
         }

         this.attemptToBalance = false;
      }

      if (this.newChange) {
         this.canCraft = this.canCraft();
         this.newChange = false;
      }

      if (this.canCraft && this.craftingOutput.canAdd(this.recipeOutput) && this.energy.useEnergy(this.energyConsume)) {
         this.setActive(true);
         if (++this.progress >= this.operationLength) {
            this.doCrafting();
            needsInvUpdate = true;
            this.newChange = true;
            this.progress = 0;
         }
      } else {
         if (!this.hasRecipe()) {
            this.progress = 0;
         }

         this.setActive(false);
      }

      needsInvUpdate |= this.upgradeSlot.tickNoMark();
      this.guiProgress = (float)this.progress / this.operationLength;
      if (needsInvUpdate) {
         super.setChanged();
      }
   }

   public boolean hasRecipe() {
      return this.recipe != null;
   }

   public boolean canCraft() {
      if (!this.hasRecipe()) {
         return false;
      } else {
         for (int slot = 0; slot < this.craftingGrid.length; slot++) {
            if (!StackUtil.isEmpty(this.craftingGrid[slot]) && !this.ingredientsRow[slot].accepts(this.ingredientsRow[slot].get())) {
               return false;
            }
         }

         return true;
      }
   }

   protected void doCrafting() {
      for (int operation = 0; operation < this.operationsPerTick; operation++) {
         List<ItemStack> outputs = Collections.singletonList(this.recipeOutput);

         for (ItemStack stack : this.upgradeSlot) {
            if (stack != null && stack.getItem() instanceof IUpgradeItem) {
               ((IUpgradeItem)stack.getItem()).onProcessEnd(stack, this, outputs);
            }
         }

         this.craft();
         if (!this.hasRecipe() || !this.craftingOutput.canAdd(this.recipeOutput)) {
            break;
         }
      }
   }

   protected void craft() {
      assert this.hasRecipe();

      assert this.craftingOutput.canAdd(this.recipeOutput);

      this.craftingOutput.add(this.recipeOutput);
      List<ItemStack> stacks = NativeCrafting.remaining(this.recipe, this.ingredients);
      Level world = this.getLevel();

      for (int slot = 0; slot < this.ingredientsRow.length; slot++) {
         ItemStack oldStack = this.ingredientsRow[slot].get();
         if (!StackUtil.isEmpty(oldStack) && !StackUtil.isEmpty(this.craftingGrid[slot])) {
            oldStack = StackUtil.decSize(oldStack);
            this.ingredientsRow[slot].put(oldStack);
         }

         if (stacks.size() > slot && !StackUtil.isEmpty(stacks.get(slot))) {
            ItemStack newStack = stacks.get(slot);
            if (StackUtil.isEmpty(oldStack) && this.ingredientsRow[slot].accepts(newStack)) {
               this.ingredientsRow[slot].put(newStack);
            } else if (StackUtil.checkItemEqualityStrict(oldStack, newStack)) {
               this.ingredientsRow[slot].put(StackUtil.incSize(newStack, StackUtil.getSize(oldStack)));
            } else if (this.containerOutput.canAdd(newStack)) {
               this.containerOutput.add(newStack);
            } else {
               StackUtil.dropAsEntity(world, this.getBlockPos(), newStack);
            }
         }
      }

      for (int i = this.ingredientsRow.length; i < stacks.size(); i++) {
         ItemStack newStack = stacks.get(i);
         if (this.containerOutput.canAdd(newStack)) {
            this.containerOutput.add(newStack);
         } else {
            StackUtil.dropAsEntity(world, this.getBlockPos(), newStack);
         }
      }
   }

   protected void setOverclockRates() {
      this.upgradeSlot.onChanged();
      double previousProgress = (double)this.progress / this.operationLength;
      this.operationsPerTick = this.upgradeSlot.getOperationsPerTick(40);
      this.operationLength = this.upgradeSlot.getOperationLength(40);
      this.energyConsume = this.upgradeSlot.getEnergyDemand(2);
      int tier = this.upgradeSlot.getTier(1);
      this.energy.setSinkTier(tier);
      this.dischargeSlot.setTier(tier);
      this.energy.setCapacity(this.upgradeSlot.getEnergyStorage(20000, 40, 2));
      this.progress = (short)Math.floor(previousProgress * this.operationLength + 0.1);
   }

   @Override
   public void onNetworkEvent(Player player, int event) {
      switch (event) {
         case 0:
            this.matrixChange(-1);
      }
   }

   @Override
   public Set<UpgradableProperty> getUpgradableProperties() {
      return UPGRADES;
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
   public ContainerBase<?> getGuiContainer(Player player) {
      return new ContainerBatchCrafter(player, this);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Screen getGui(Player player, boolean isAdmin) {
      return new GuiBatchCrafter(new ContainerBatchCrafter(player, this));
   }

   @Override
   public void onGuiClosed(Player player) {
   }

   @Override
   public double getGuiValue(String name) {
      if ("progress".equals(name)) {
         return this.guiProgress;
      } else {
         throw new IllegalArgumentException("Unexpected value requested: " + name);
      }
   }
}
