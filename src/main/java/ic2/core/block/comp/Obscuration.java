package ic2.core.block.comp;

import ic2.api.event.RetextureEvent;
import ic2.core.block.TileEntityBlock;
import ic2.core.block.state.BlockStateUtil;
import ic2.core.item.tool.ItemObscurator;
import ic2.core.ref.BlockName;
import ic2.core.util.NbtChecks;
import ic2.core.util.Util;
import java.util.Arrays;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;

public class Obscuration extends TileEntityComponent {
   private final Runnable changeHandler;
   private Obscuration.ObscurationData[] dataMap;

   public Obscuration(TileEntityBlock parent, Runnable changeHandler) {
      super(parent);
      this.changeHandler = changeHandler;
   }

   @Override
   public void readFromNbt(CompoundTag nbt) {
      if (!nbt.isEmpty()) {
         for (Direction facing : Direction.values()) {
            if (NbtChecks.hasType(nbt, facing.getName(), 10)) {
               CompoundTag cNbt = nbt.getCompoundOrEmpty(facing.getName());
               Block block = Util.getBlock(cNbt.getStringOr("block", ""));
               if (block != null) {
                  String variant = cNbt.getStringOr("variant", "");
                  BlockState state = BlockStateUtil.getState(block, variant);
                  if (state != null) {
                     int rawSide = cNbt.getByteOr("side", (byte)0);
                     if (rawSide >= 0 && rawSide < Direction.values().length) {
                        Direction side = Direction.values()[rawSide];
                        int[] colorMultipliers = ItemObscurator.internColorMultipliers(cNbt.getIntArray("colorMuls").orElseGet(() -> new int[0]));
                        Obscuration.ObscurationData data = new Obscuration.ObscurationData(state, variant, side, colorMultipliers);
                        if (this.dataMap == null) {
                           this.dataMap = new Obscuration.ObscurationData[Direction.values().length];
                        }

                        this.dataMap[facing.ordinal()] = data.intern();
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public CompoundTag writeToNbt() {
      if (this.dataMap == null) {
         return null;
      } else {
         CompoundTag ret = new CompoundTag();

         for (Direction facing : Direction.values()) {
            Obscuration.ObscurationData data = this.dataMap[facing.ordinal()];
            if (data != null) {
               CompoundTag nbt = new CompoundTag();
               nbt.putString("block", Util.getName(data.state.getBlock()).toString());
               nbt.putString("variant", data.variant);
               nbt.putByte("side", (byte)data.side.ordinal());
               nbt.putIntArray("colorMuls", data.colorMultipliers);
               ret.put(facing.getName(), nbt);
            }
         }

         return ret;
      }
   }

   public boolean applyObscuration(Direction side, Obscuration.ObscurationData data) {
      if (this.dataMap != null && data.equals(this.dataMap[side.ordinal()])) {
         return false;
      } else {
         if (this.dataMap == null) {
            this.dataMap = new Obscuration.ObscurationData[Direction.values().length];
         }

         this.dataMap[side.ordinal()] = data.intern();
         this.changeHandler.run();
         return true;
      }
   }

   public void clear() {
      this.dataMap = null;
      this.changeHandler.run();
   }

   public boolean hasObscuration() {
      return this.dataMap != null;
   }

   public Obscuration.ObscurationData[] getRenderState() {
      return this.dataMap == null ? null : Arrays.copyOf(this.dataMap, this.dataMap.length);
   }

   public static class ObscurationComponentEventHandler {
      public static void init() {
         new Obscuration.ObscurationComponentEventHandler();
      }

      private ObscurationComponentEventHandler() {
         NeoForge.EVENT_BUS.register(this);
      }

      @SubscribeEvent
      public void onObscuration(RetextureEvent event) {
         if (event.state.getBlock() == BlockName.te.getInstance()) {
            BlockEntity teRaw = event.getLevel().getBlockEntity(event.pos);
            if (teRaw instanceof TileEntityBlock) {
               Obscuration obscuration = ((TileEntityBlock)teRaw).getComponent(Obscuration.class);
               if (obscuration != null) {
                  Obscuration.ObscurationData data = new Obscuration.ObscurationData(event.refState, event.refVariant, event.refSide, event.refColorMultipliers);
                  if (obscuration.applyObscuration(event.side, data)) {
                     event.applied = true;
                     event.setCanceled(true);
                  }
               }
            }
         }
      }
   }

   public static class ObscurationData {
      public final BlockState state;
      public final String variant;
      public final Direction side;
      public final int[] colorMultipliers;

      public ObscurationData(BlockState state, String variant, Direction side, int[] colorMultipliers) {
         this.state = state;
         this.variant = variant;
         this.side = side;
         this.colorMultipliers = colorMultipliers;
      }

      @Override
      public boolean equals(Object obj) {
         if (obj == this) {
            return true;
         } else {
            return !(obj instanceof Obscuration.ObscurationData o)
               ? false
               : o.state.equals(this.state)
                  && o.variant.equals(this.variant)
                  && o.side == this.side
                  && Arrays.equals(o.colorMultipliers, this.colorMultipliers);
         }
      }

      @Override
      public int hashCode() {
         return (this.state.hashCode() * 7 + this.side.ordinal()) * 23;
      }

      public Obscuration.ObscurationData intern() {
         return this;
      }
   }
}
