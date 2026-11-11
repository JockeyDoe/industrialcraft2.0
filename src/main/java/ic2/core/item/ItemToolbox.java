package ic2.core.item;

import ic2.core.IC2;
import ic2.core.IHasGui;
import ic2.core.item.tool.ContainerToolbox;
import ic2.core.item.tool.HandHeldToolbox;
import ic2.core.model.ModelKey;
import ic2.core.model.ModelRegistry;
import ic2.core.ref.ItemName;
import ic2.core.util.StackUtil;
import java.util.Objects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ItemToolbox extends ItemIC2 implements IHandHeldInventory {
   public ItemToolbox() {
      super(ItemName.tool_box);
      ItemDefaults.stackSize(this, 1);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void registerModels(final ItemName name) {
      ModelRegistry.setCustomMeshDefinition(
         this,
         new ModelRegistry.ItemMeshDefinition() {
            {
               Objects.requireNonNull(ItemToolbox.this);
            }

            @Override
            public ModelKey func_178113_a(ItemStack stack) {
               Player player = IC2.platform.getPlayerInstance();
               ItemStack mainHandItem;
               boolean open = player != null
                  && player.containerMenu instanceof ContainerToolbox
                  && (
                     StackUtil.checkItemEquality(mainHandItem = player.getMainHandItem(), stack)
                        || StackUtil.checkItemEquality(player.getOffhandItem(), stack)
                           && (mainHandItem == null || !(mainHandItem.getItem() instanceof IHandHeldInventory))
                  );
               return ItemIC2.getModelLocation(name, open ? "open" : null);
            }
         }
      );
      ModelRegistry.registerItemVariants(this, new ModelKey[]{getModelLocation(name, null)});
      ModelRegistry.registerItemVariants(this, new ModelKey[]{getModelLocation(name, "open")});
   }

   public InteractionResult use(Level world, Player player, InteractionHand hand) {
      ItemStack stack = StackUtil.get(player, hand);
      if (IC2.platform.isSimulating()) {
         IC2.platform.launchGui(player, this.getInventory(player, stack));
      }

      return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
   }

   public boolean onDroppedByPlayer(ItemStack stack, Player player) {
      if (!player.level().isClientSide() && !StackUtil.isEmpty(stack) && player.containerMenu instanceof ContainerToolbox) {
         HandHeldToolbox toolbox = ((ContainerToolbox)player.containerMenu).base;
         if (toolbox.isThisContainer(stack)) {
            toolbox.saveAsThrown(stack);
            player.closeContainer();
         }
      }

      return true;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Rarity func_77613_e(ItemStack stack) {
      return Rarity.UNCOMMON;
   }

   @Override
   public IHasGui getInventory(Player player, ItemStack stack) {
      return new HandHeldToolbox(player, stack, 9);
   }
}
