package ic2.core.item.block;

import ic2.core.block.BlockBase;
import ic2.core.block.BlockScaffold;
import ic2.core.init.Localization;
import ic2.core.init.NativeRegistration;
import ic2.core.ref.BlockMaterial;
import ic2.core.ref.BlockName;
import ic2.core.util.ItemStacks;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class ItemBlockIC2 extends BlockItem {
   public static Function<Block, Item> supplier = ItemBlockIC2::new;

   public ItemBlockIC2(Block block) {
      super(
         block,
         new Properties().setId(ResourceKey.create(Registries.ITEM, Objects.requireNonNull(NativeRegistration.name(block), "Unregistered IC2 block item")))
      );
   }

   protected BlockState getPlacementState(BlockPlaceContext context) {
      if (this.getBlock() instanceof BlockBase block) {
         BlockPos var5 = context.getClickedPos();
         Vec3 hit = context.getClickLocation();
         return block.func_180642_a(
            context.getLevel(),
            var5,
            context.getClickedFace(),
            (float)(hit.x - var5.getX()),
            (float)(hit.y - var5.getY()),
            (float)(hit.z - var5.getZ()),
            ItemStacks.rawMeta(context.getItemInHand()),
            context.getPlayer()
         );
      } else {
         return super.getPlacementState(context);
      }
   }

   protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
      BlockPos pos = context.getClickedPos();
      Vec3 hit = context.getClickLocation();
      return this.placeBlockAt(
         context.getItemInHand(),
         context.getPlayer(),
         context.getLevel(),
         pos,
         context.getClickedFace(),
         (float)(hit.x - pos.getX()),
         (float)(hit.y - pos.getY()),
         (float)(hit.z - pos.getZ()),
         state
      );
   }

   public boolean placeBlockAt(ItemStack stack, Player player, Level world, BlockPos pos, Direction side, float x, float y, float z, BlockState state) {
      BlockPlaceContext context = new BlockPlaceContext(
         world, player, InteractionHand.MAIN_HAND, stack, new BlockHitResult(new Vec3(pos.getX() + x, pos.getY() + y, pos.getZ() + z), side, pos, false)
      );
      return super.placeBlock(context, state);
   }

   public String func_77667_c(ItemStack stack) {
      Identifier id = NativeRegistration.name(this.getBlock());
      return id == null ? this.getDescriptionId() : id.getNamespace() + "." + id.getPath().replace('/', '.');
   }

   public Component getName(ItemStack stack) {
      return Component.translatable(this.func_77667_c(stack));
   }

   public String func_77653_i(ItemStack stack) {
      return Localization.translate(this.func_77667_c(stack));
   }

   public boolean canHarvestBlock(BlockState block, ItemStack stack) {
      return block.getBlock() == BlockName.scaffold.getInstance();
   }

   public int getItemBurnTime(ItemStack stack) {
      if (this.getBlock() == BlockName.scaffold.getInstance()) {
         BlockScaffold scaffold = (BlockScaffold)this.getBlock();
         BlockState state = scaffold.getState(scaffold.getVariant(stack));
         return BlockMaterial.of(state) == BlockMaterial.WOOD ? 300 : 0;
      } else {
         return -1;
      }
   }

   public Rarity func_77613_e(ItemStack stack) {
      return this.getBlock() instanceof BlockBase ? ((BlockBase)this.getBlock()).getRarity(stack) : stack.getRarity();
   }
}
