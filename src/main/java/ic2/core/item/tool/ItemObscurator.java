package ic2.core.item.tool;

import ic2.api.event.RetextureEvent;
import ic2.api.item.ElectricItem;
import ic2.core.IC2;
import ic2.core.block.state.BlockStateUtil;
import ic2.core.item.BaseElectricItem;
import ic2.core.item.ItemDefaults;
import ic2.core.model.Ic2Geometry;
import ic2.core.model.ModelUtil;
import ic2.core.network.IPlayerItemDataListener;
import ic2.core.ref.ItemName;
import ic2.core.util.ItemData;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import ic2.core.util.Vector3;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector3fc;

public class ItemObscurator extends BaseElectricItem implements IPlayerItemDataListener {
   private final int scanOperationCost = 20000;
   private final int printOperationCost = 5000;
   private static ThreadLocal<ItemObscurator.ExtractingVertexConsumer> testConsumers = new ThreadLocal<ItemObscurator.ExtractingVertexConsumer>() {
      protected ItemObscurator.ExtractingVertexConsumer initialValue() {
         return new ItemObscurator.ExtractingVertexConsumer();
      }
   };
   private static final int[] noTint = new int[]{-1};
   private static final int[] zeroTint = new int[]{0};
   private static final int[] defaultColorMultiplier = new int[]{16777215};
   private static final int[] colorMultiplierOpaqueWhite = new int[]{-1};

   public ItemObscurator() {
      super(ItemName.obscurator, 100000.0, 250.0, 2);
      ItemDefaults.durability(this, 27);
      ItemDefaults.stackSize(this, 1);
      ItemDefaults.noRepair(this);
   }

   @Override
   public List<String> getHudInfo(ItemStack stack, boolean advanced) {
      List<String> info = new LinkedList<>();
      info.add(ElectricItem.manager.getToolTip(stack));
      return info;
   }

   public InteractionResult onItemUseFirst(Player player, Level world, BlockPos pos, Direction side, float hitX, float hitY, float hitZ, InteractionHand hand) {
      ItemStack stack = StackUtil.get(player, hand);
      if (!player.isShiftKeyDown() && !world.isClientSide() && ElectricItem.manager.canUse(stack, 5000.0)) {
         CompoundTag nbt = StackUtil.getOrCreateNbtData(stack);
         BlockState refState;
         Direction refSide;
         int[] colorMultipliers;
         if ((refState = getState(nbt)) != null && (refSide = getSide(nbt)) != null && (colorMultipliers = getColorMultipliers(nbt)) != null) {
            BlockState state = world.getBlockState(pos);
            RetextureEvent event = new RetextureEvent(world, pos, state, side, player, refState, getVariant(nbt), refSide, colorMultipliers);
            NeoForge.EVENT_BUS.post(event);
            if (event.applied) {
               ElectricItem.manager.use(stack, 5000.0, player);
               return InteractionResult.SUCCESS;
            } else {
               return InteractionResult.PASS;
            }
         } else {
            clear(nbt);
            return InteractionResult.PASS;
         }
      } else if (player.isShiftKeyDown() && world.isClientSide() && ElectricItem.manager.canUse(stack, 20000.0)) {
         return (InteractionResult)(this.scanBlock(stack, player, world, pos, side) ? InteractionResult.SUCCESS : InteractionResult.PASS);
      } else {
         return InteractionResult.PASS;
      }
   }

   private boolean scanBlock(ItemStack stack, Player player, Level world, BlockPos pos, Direction side) {
      assert world.isClientSide();

      BlockState state = Util.getBlockState(world, pos);
      if (state.isAir()) {
         return false;
      } else {
         ItemObscurator.ObscuredRenderInfo renderInfo = getRenderInfo(state, side);
         if (renderInfo == null) {
            return false;
         } else {
            String variant = ModelUtil.getVariant(state);
            int[] colorMultipliers = new int[renderInfo.tints.length];

            for (int i = 0; i < renderInfo.tints.length; i++) {
               colorMultipliers[i] = IC2.platform.getColorMultiplier(state, world, pos, renderInfo.tints[i]);
            }

            CompoundTag nbt = StackUtil.getOrCreateNbtData(stack);
            if (getState(nbt) == state && variant.equals(getVariant(nbt)) && getSide(nbt) == side && Arrays.equals(getColorMultipliers(nbt), colorMultipliers)) {
               return false;
            } else {
               IC2.network.get(false).sendPlayerItemData(player, player.getInventory().getSelectedSlot(), state.getBlock(), variant, side, colorMultipliers);
               return true;
            }
         }
      }
   }

   @Override
   public void onPlayerItemNetworkData(Player player, int slot, Object... data) {
      if (data[0] instanceof Block && data[1] instanceof String && data[2] instanceof Integer && data[3] instanceof int[]) {
         ItemStack stack = (ItemStack)player.getInventory().getNonEquipmentItems().get(slot);
         if (ElectricItem.manager.use(stack, 20000.0, player)) {
            CompoundTag nbt = StackUtil.getOrCreateNbtData(stack);
            setState(nbt, (Block)data[0], (String)data[1]);
            setSide(nbt, (Integer)data[2]);
            setColorMultipliers(nbt, (int[])data[3]);
            ItemData.commit(stack, nbt);
         }
      }
   }

   public static BlockState getState(CompoundTag nbt) {
      String blockName = nbt.getStringOr("refBlock", "");
      if (blockName.isEmpty()) {
         return null;
      } else {
         Block block = Util.getBlock(blockName);
         if (block == null) {
            return null;
         } else {
            String variant = getVariant(nbt);
            return BlockStateUtil.getState(block, variant);
         }
      }
   }

   public static String getVariant(CompoundTag nbt) {
      return nbt.getStringOr("refVariant", "");
   }

   private static void setState(CompoundTag nbt, Block block, String variant) {
      nbt.putString("refBlock", Util.getName(block).toString());
      nbt.putString("refVariant", variant);
   }

   public static Direction getSide(CompoundTag nbt) {
      int ordinal = nbt.getByteOr("refSide", (byte)0);
      return ordinal >= 0 && ordinal < Direction.values().length ? Direction.values()[ordinal] : null;
   }

   private static void setSide(CompoundTag nbt, int side) {
      nbt.putByte("refSide", (byte)side);
   }

   public static int[] getColorMultipliers(CompoundTag nbt) {
      int[] ret = nbt.getIntArray("refColorMuls").orElseGet(() -> new int[0]);
      return ret.length == 0 ? null : internColorMultipliers(ret);
   }

   public static void setColorMultipliers(CompoundTag nbt, int[] colorMultipliers) {
      if (colorMultipliers.length == 0) {
         throw new IllegalArgumentException();
      } else {
         nbt.putIntArray("refColorMuls", colorMultipliers);
      }
   }

   private static void clear(CompoundTag nbt) {
      nbt.remove("refBlock");
      nbt.remove("refVariant");
      nbt.remove("refSide");
      nbt.remove("refColorMul");
   }

   public static ItemObscurator.ObscuredRenderInfo getRenderInfo(BlockState state, Direction side) {
      Block block = state.getBlock();
      Ic2Geometry model = ModelUtil.getBlockModel(state);
      if (model == null) {
         return null;
      } else {
         List<BakedQuad> faceQuads = model.func_188616_a(state, side, 0L);
         if (faceQuads.stream().anyMatch(quad -> quad.materialInfo().layer() == ChunkSectionLayer.TRANSLUCENT)) {
            return null;
         } else if (faceQuads.isEmpty()) {
            return null;
         } else {
            float[] uvs = new float[faceQuads.size() * 4];
            int uvsOffset = 0;
            int[] tints = new int[faceQuads.size()];
            TextureAtlasSprite[] sprites = new TextureAtlasSprite[faceQuads.size()];
            ItemObscurator.ExtractingVertexConsumer testConsumer = testConsumers.get();

            for (BakedQuad faceQuad : faceQuads) {
               testConsumer.read(faceQuad);
               float[] positions = testConsumer.positions;
               int dx = side.getStepX();
               int dy = side.getStepY();
               int dz = side.getStepZ();
               int xS = (dx + 1) / 2;
               int yS = (dy + 1) / 2;
               int zS = (dz + 1) / 2;
               byte vertices = 4;
               byte positionElements = 3;
               int firstVertex = -1;

               for (int v1 = 0; v1 < 4; v1++) {
                  int vo = v1 * 3;
                  if (Util.isSimilar(positions[vo + 0], (float)xS)
                     && Util.isSimilar(positions[vo + 1], (float)yS)
                     && Util.isSimilar(positions[vo + 2], (float)zS)) {
                     firstVertex = v1;
                     break;
                  }
               }

               if (firstVertex != -1) {
                  Vector3 v1x = new Vector3(positions[3] - positions[0], positions[4] - positions[1], positions[5] - positions[2]);
                  if (Util.isSimilar(v1x.lengthSquared(), 1.0)) {
                     Vector3 v4 = new Vector3(positions[9] - positions[0], positions[10] - positions[1], positions[11] - positions[2]);
                     if (Util.isSimilar(v4.lengthSquared(), 1.0)) {
                        Vector3 v3 = new Vector3(positions[9] - positions[6], positions[10] - positions[7], positions[11] - positions[8]);
                        if (Util.isSimilar(v3.copy().add(v1x).lengthSquared(), 0.0)) {
                           Vector3 normal = v1x.copy().cross(v4);
                           if (Util.isSimilar(normal.copy().sub(dx, dy, dz).lengthSquared(), 0.0)) {
                              tints[uvsOffset / 4] = testConsumer.tint;
                              sprites[uvsOffset / 4] = testConsumer.sprite;
                              uvs[uvsOffset++] = testConsumer.uvs[firstVertex * 2];
                              uvs[uvsOffset++] = testConsumer.uvs[firstVertex * 2 + 1];
                              uvs[uvsOffset++] = testConsumer.uvs[(firstVertex + 2) % 4 * 2];
                              uvs[uvsOffset++] = testConsumer.uvs[(firstVertex + 2) % 4 * 2 + 1];
                           }
                        }
                     }
                  }
               }
            }

            if (uvsOffset == 0) {
               return null;
            } else {
               if (uvsOffset < uvs.length) {
                  uvs = Arrays.copyOf(uvs, uvsOffset);
                  tints = Arrays.copyOf(tints, uvsOffset / 4);
                  sprites = Arrays.copyOf(sprites, uvsOffset / 4);
               }

               tints = internTints(tints);
               return new ItemObscurator.ObscuredRenderInfo(uvs, tints, sprites);
            }
         }
      }
   }

   public static int[] internTints(int[] tints) {
      if (tints.length == 1) {
         if (tints[0] == noTint[0]) {
            return noTint;
         }

         if (tints[0] == zeroTint[0]) {
            return zeroTint;
         }
      }

      return tints;
   }

   public static int[] internColorMultipliers(int[] colorMultipliers) {
      if (colorMultipliers.length == 1) {
         if (colorMultipliers[0] == defaultColorMultiplier[0]) {
            return defaultColorMultiplier;
         }

         if (colorMultipliers[0] == colorMultiplierOpaqueWhite[0]) {
            return colorMultiplierOpaqueWhite;
         }
      }

      return colorMultipliers;
   }

   private static class ExtractingVertexConsumer {
      private final float[] positions = new float[12];
      private final float[] uvs = new float[8];
      private int tint;
      private TextureAtlasSprite sprite;

      void read(BakedQuad quad) {
         this.tint = quad.materialInfo().tintIndex();
         this.sprite = quad.materialInfo().sprite();

         for (int vertex = 0; vertex < 4; vertex++) {
            Vector3fc position = quad.position(vertex);
            this.positions[vertex * 3] = position.x();
            this.positions[vertex * 3 + 1] = position.y();
            this.positions[vertex * 3 + 2] = position.z();
            this.uvs[vertex * 2] = UVPair.unpackU(quad.packedUV(vertex));
            this.uvs[vertex * 2 + 1] = UVPair.unpackV(quad.packedUV(vertex));
         }
      }
   }

   public static class ObscuredRenderInfo {
      public final float[] uvs;
      public final int[] tints;
      public final TextureAtlasSprite[] sprites;

      private ObscuredRenderInfo(float[] uvs, int[] tints, TextureAtlasSprite[] sprites) {
         this.uvs = uvs;
         this.tints = tints;
         this.sprites = sprites;
      }
   }
}
