package ic2.core.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import ic2.core.block.BlockTileEntity;
import ic2.core.block.transport.items.PipeType;
import ic2.core.item.armor.ItemArmorQuantumSuit;
import ic2.core.item.block.ItemFluidPipe;
import ic2.core.item.logistics.ItemPumpCover;
import ic2.core.ref.BlockName;
import ic2.core.ref.MetaTeBlock;
import java.util.List;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.BlockTintSources;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.ItemTintSources;

@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT)
public record Ic2ItemTint(int layer) implements ItemTintSource {
   public static final MapCodec<Ic2ItemTint> CODEC = Codec.INT.fieldOf("layer").xmap(Ic2ItemTint::new, Ic2ItemTint::layer);

   public MapCodec<Ic2ItemTint> type() {
      return CODEC;
   }

   public int calculate(ItemStack stack, ClientLevel level, LivingEntity owner) {
      return color(stack, this.layer);
   }

   public static int storageColor(String variant) {
      if (variant == null) {
         return -1;
      } else {
         return 0xFF000000 | switch (variant) {
            case "wooden_storage_box" -> 10454093;
            case "iron_storage_box" -> 13158600;
            case "bronze_storage_box" -> 16744448;
            case "steel_storage_box" -> 8421504;
            default -> 16777215;
         };
      }
   }

   public static int color(ItemStack stack, int layer) {
      if (BlockName.leaves.hasInstance() && stack.is(BlockName.leaves.getInstance().asItem())) {
         return -10053308;
      } else if (stack.getItem() instanceof ItemArmorQuantumSuit armor) {
         return layer == 0 ? 0xFF000000 | armor.func_82814_b(stack) : -1;
      } else if (stack.getItem() instanceof ItemFluidPipe) {
         PipeType pipe = ItemFluidPipe.getPipeType(stack);
         return 0xFF000000 | (pipe.red & 0xFF) << 16 | (pipe.green & 0xFF) << 8 | pipe.blue & 0xFF;
      } else if (BlockName.te.hasInstance() && stack.is(BlockName.te.getInstance().asItem())) {
         return storageColor(BlockName.te.getVariant(stack));
      } else if (stack.getItem() instanceof ItemPumpCover cover) {
         return layer == 1 ? 0xFF000000 | cover.getType(stack).color : -1;
      } else {
         return -1;
      }
   }

   @SubscribeEvent
   public static void items(ItemTintSources event) {
      event.register(Identifier.fromNamespaceAndPath("ic2", "item_color"), CODEC);
   }

   @SubscribeEvent
   public static void blocks(BlockTintSources event) {
      event.register(List.of(net.minecraft.client.color.block.BlockTintSources.constant(-10053308)), new Block[]{BlockName.leaves.getInstance()});
      event.register(
         List.of(state -> storageColor(((MetaTeBlock)state.getValue(((BlockTileEntity)state.getBlock()).typeProperty)).teBlock.getName())),
         new Block[]{BlockName.te.getInstance()}
      );
   }
}
