package ic2.core.block.storage.box;

import ic2.core.ContainerBase;
import ic2.core.IHasGui;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.invslot.InvSlot;
import ic2.core.gui.dynamic.DynamicContainer;
import ic2.core.gui.dynamic.DynamicGui;
import ic2.core.gui.dynamic.GuiParser;
import ic2.core.util.ItemData;
import ic2.core.util.StackUtil;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public abstract class TileEntityStorageBox extends TileEntityInventory implements IHasGui {
   protected final InvSlot inventory;

   public TileEntityStorageBox(int inventorySize) {
      this.inventory = new InvSlot(this, "inventory", InvSlot.Access.IO, inventorySize, InvSlot.InvSide.ANY);
   }

   @Override
   protected List<ItemStack> getAuxDrops(int fortune) {
      return Collections.emptyList();
   }

   @Override
   public void onPlaced(ItemStack stack, LivingEntity placer, Direction facing) {
      super.onPlaced(stack, placer, facing);
      if (!this.getLevel().isClientSide()) {
         CompoundTag nbt = StackUtil.getOrCreateNbtData(stack);
         this.inventory.readFromNbt(nbt);
      }
   }

   @Override
   protected ItemStack adjustDrop(ItemStack drop, boolean wrench) {
      CompoundTag nbt = StackUtil.getOrCreateNbtData(drop);
      if (!this.inventory.isEmpty()) {
         this.inventory.writeToNbt(nbt);
         ItemData.commit(drop, nbt);
      }

      return drop;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void addInformation(ItemStack stack, List<String> info, TooltipFlag advanced) {
      info.add("Stores items even when broken");
      info.add("Inventory size: " + this.inventory.size());
   }

   @Override
   protected SoundType getBlockSound(Entity entity) {
      return SoundType.METAL;
   }

   @Override
   public ContainerBase<? extends TileEntityStorageBox> getGuiContainer(Player player) {
      return DynamicContainer.create(this, player, GuiParser.parse(this.teBlock));
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Screen getGui(Player player, boolean isAdmin) {
      return DynamicGui.<TileEntityStorageBox>create(this, player, GuiParser.parse(this.teBlock));
   }

   @Override
   public void onGuiClosed(Player player) {
   }
}
