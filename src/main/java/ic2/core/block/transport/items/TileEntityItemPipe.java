package ic2.core.block.transport.items;

import ic2.api.transport.IItemTransportTile;
import ic2.core.block.transport.TileEntityPipe;
import ic2.core.item.block.ItemPipe;
import ic2.core.util.ItemStacks;
import ic2.core.util.NbtChecks;
import ic2.core.util.StackUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

public class TileEntityItemPipe extends TileEntityPipe implements IItemTransportTile {
   protected PipeType type = PipeType.bronze;
   protected PipeSize size = PipeSize.small;
   protected ItemStack contents;

   public TileEntityItemPipe() {
   }

   @Override
   public void flipConnection(Direction facing) {
   }

   public TileEntityItemPipe(PipeType type, PipeSize size) {
      this();
      this.type = type;
      this.size = size;
   }

   @Override
   public int putItems(ItemStack stack, Direction facing, boolean simulate) {
      if (StackUtil.isEmpty(stack)) {
         return 0;
      } else if (!StackUtil.isEmpty(this.contents)) {
         return 0;
      } else if (stack.getCount() > this.getMaxStackSizeAllowed()) {
         return 0;
      } else {
         if (!simulate) {
            this.contents = StackUtil.copy(stack);
         }

         return stack.getCount();
      }
   }

   @Override
   public int getMaxStackSizeAllowed() {
      return this.size.maxStackSize;
   }

   @Override
   public int getTransferRate() {
      return this.type.transferRate;
   }

   @Override
   public ItemStack getContents() {
      return this.contents;
   }

   @Override
   public void setContents(ItemStack stack) {
      this.contents = stack;
   }

   @Override
   protected void updateEntityServer() {
      super.updateEntityServer();
      boolean needsInventoryUpdate = false;
      if (!StackUtil.isEmpty(this.contents)) {
         Direction facing = this.getFacing();
         BlockEntity target = this.getLevel().getBlockEntity(this.getBlockPos().relative(facing));
         if (target instanceof IItemTransportTile && ((IItemTransportTile)target).putItems(this.contents, facing.getOpposite(), true) > 0) {
            int amount = ((IItemTransportTile)target).putItems(this.contents, facing.getOpposite(), false);
            ItemStack newStack = StackUtil.copyShrunk(this.contents, amount);

            assert newStack.isEmpty();

            this.contents = null;
            needsInventoryUpdate = true;
         }
      }

      if (needsInventoryUpdate) {
         this.setChanged();
      }
   }

   @Override
   public void loadAdditional(CompoundTag nbt) {
      super.loadAdditional(nbt);
      this.type = PipeType.values[nbt.getByteOr("type", (byte)0) & 255];
      this.size = PipeSize.values()[nbt.getByteOr("size", (byte)0) & 255];
      ListTag contentsTag = NbtChecks.getList(nbt, "contents", 10);

      for (int i = 0; i < contentsTag.size(); i++) {
         CompoundTag contentTag = contentsTag.getCompoundOrEmpty(i);
         ItemStack stack = ItemStacks.read(contentTag);
         if (!StackUtil.isEmpty(stack)) {
            this.contents = stack;
         }
      }
   }

   @Override
   public CompoundTag saveAdditional(CompoundTag nbt) {
      super.saveAdditional(nbt);
      nbt.putByte("type", (byte)this.type.ordinal());
      nbt.putByte("size", (byte)this.size.ordinal());
      ListTag contentsTag = new ListTag();
      if (!StackUtil.isEmpty(this.contents)) {
         CompoundTag contentTag = new CompoundTag();
         ItemStacks.write(this.contents, contentTag);
         contentsTag.add(contentTag);
         nbt.put("contents", contentsTag);
      }

      return nbt;
   }

   @Override
   protected void updateConnectivity() {
   }

   @Override
   protected ItemStack getPickBlock(Player player, HitResult target) {
      return ItemPipe.getPipe(this.type, this.size);
   }

   @Override
   protected List<ItemStack> getAuxDrops(int fortune) {
      List<ItemStack> ret = new ArrayList<>(super.getAuxDrops(fortune));
      if (!StackUtil.isEmpty(this.contents)) {
         ret.add(this.contents);
      }

      return ret;
   }

   @Override
   protected List<AABB> getAabbs(boolean forCollision) {
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
         }
      }

      return ret;
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
}
