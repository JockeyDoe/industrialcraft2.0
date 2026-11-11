package ic2.core.block.machine.tileentity;

import ic2.api.network.INetworkClientTileEntityEventListener;
import ic2.api.recipe.IPatternStorage;
import ic2.core.ContainerBase;
import ic2.core.IHasGui;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumableId;
import ic2.core.block.machine.container.ContainerPatternStorage;
import ic2.core.block.machine.gui.GuiPatternStorage;
import ic2.core.item.ItemCrystalMemory;
import ic2.core.profile.NotClassic;
import ic2.core.ref.ItemName;
import ic2.core.ref.TeBlock;
import ic2.core.util.ItemData;
import ic2.core.util.ItemStacks;
import ic2.core.util.NbtChecks;
import ic2.core.util.StackUtil;
import ic2.core.uu.UuIndex;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@NotClassic
public class TileEntityPatternStorage extends TileEntityInventory implements IHasGui, INetworkClientTileEntityEventListener, IPatternStorage {
   public final InvSlotConsumableId diskSlot;
   private final List<ItemStack> patterns = new ArrayList<>();
   public int index = 0;
   public int maxIndex;
   public ItemStack pattern;
   public double patternUu;
   public double patternEu;

   public TileEntityPatternStorage() {
      this.diskSlot = new InvSlotConsumableId(this, "SaveSlot", InvSlot.Access.IO, 1, InvSlot.InvSide.ANY, ItemName.crystal_memory.getInstance());
   }

   @Override
   public void loadAdditional(CompoundTag nbttagcompound) {
      super.loadAdditional(nbttagcompound);
      this.readContents(nbttagcompound);
   }

   @Override
   public CompoundTag saveAdditional(CompoundTag nbt) {
      super.saveAdditional(nbt);
      this.writeContentsAsNbtList(nbt);
      return nbt;
   }

   @Override
   public void onPlaced(ItemStack stack, LivingEntity placer, Direction facing) {
      super.onPlaced(stack, placer, facing);
      if (!this.getLevel().isClientSide()) {
         CompoundTag nbt = StackUtil.getOrCreateNbtData(stack);
         this.readContents(nbt);
      }
   }

   @Override
   protected ItemStack adjustDrop(ItemStack drop, boolean wrench) {
      drop = super.adjustDrop(drop, wrench);
      if (wrench || this.teBlock.getDefaultDrop() == TeBlock.DefaultDrop.Self) {
         CompoundTag nbt = StackUtil.getOrCreateNbtData(drop);
         this.writeContentsAsNbtList(nbt);
         ItemData.commit(drop, nbt);
      }

      return drop;
   }

   public void readContents(CompoundTag nbt) {
      ListTag patternList = NbtChecks.getList(nbt, "patterns", 10);

      for (int i = 0; i < patternList.size(); i++) {
         CompoundTag contentTag = patternList.getCompoundOrEmpty(i);
         ItemStack Item = ItemStacks.read(contentTag);
         this.addPattern(Item);
      }

      this.refreshInfo();
   }

   private void writeContentsAsNbtList(CompoundTag nbt) {
      ListTag list = new ListTag();

      for (ItemStack stack : this.patterns) {
         CompoundTag contentTag = new CompoundTag();
         ItemStacks.write(stack, contentTag);
         list.add(contentTag);
      }

      nbt.put("patterns", list);
   }

   @Override
   public ContainerBase<TileEntityPatternStorage> getGuiContainer(Player player) {
      return new ContainerPatternStorage(player, this);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Screen getGui(Player player, boolean isAdmin) {
      return new GuiPatternStorage(new ContainerPatternStorage(player, this));
   }

   @Override
   public void onNetworkEvent(Player player, int event) {
      switch (event) {
         case 0:
            if (!this.patterns.isEmpty()) {
               if (this.index <= 0) {
                  this.index = this.patterns.size() - 1;
               } else {
                  this.index--;
               }

               this.refreshInfo();
            }
            break;
         case 1:
            if (!this.patterns.isEmpty()) {
               if (this.index >= this.patterns.size() - 1) {
                  this.index = 0;
               } else {
                  this.index++;
               }

               this.refreshInfo();
            }
            break;
         case 2:
            if (this.index >= 0 && this.index < this.patterns.size() && !this.diskSlot.isEmpty()) {
               ItemStack crystalMemory = this.diskSlot.get();
               if (crystalMemory.getItem() instanceof ItemCrystalMemory) {
                  ((ItemCrystalMemory)crystalMemory.getItem()).writecontentsTag(crystalMemory, this.patterns.get(this.index));
               }
            }
            break;
         case 3:
            if (!this.diskSlot.isEmpty()) {
               ItemStack crystalMemory = this.diskSlot.get();
               if (crystalMemory.getItem() instanceof ItemCrystalMemory) {
                  ItemStack record = ((ItemCrystalMemory)crystalMemory.getItem()).readItemStack(crystalMemory);
                  if (record != null) {
                     this.addPattern(record);
                  }
               }
            }
      }
   }

   public void refreshInfo() {
      if (this.index < 0 || this.index >= this.patterns.size()) {
         this.index = 0;
      }

      this.maxIndex = this.patterns.size();
      if (this.patterns.isEmpty()) {
         this.pattern = null;
      } else {
         this.pattern = this.patterns.get(this.index);
         this.patternUu = UuIndex.instance.getInBuckets(this.pattern);
      }
   }

   @Override
   public void onGuiClosed(Player player) {
   }

   @Override
   public boolean addPattern(ItemStack stack) {
      if (StackUtil.isEmpty(stack)) {
         throw new IllegalArgumentException("empty stack: " + StackUtil.toStringSafe(stack));
      } else {
         for (ItemStack pattern : this.patterns) {
            if (StackUtil.checkItemEquality(pattern, stack)) {
               return false;
            }
         }

         this.patterns.add(stack);
         this.refreshInfo();
         return true;
      }
   }

   @Override
   public List<ItemStack> getPatterns() {
      return this.patterns;
   }
}
