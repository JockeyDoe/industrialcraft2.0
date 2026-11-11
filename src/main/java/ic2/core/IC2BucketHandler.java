package ic2.core;

import ic2.core.block.BlockIC2Fluid;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem;

public class IC2BucketHandler {
   @SubscribeEvent
   public void onBucketFill(RightClickItem event) {
      if (event.getItemStack().is(Items.BUCKET)) {
         Player player = event.getEntity();
         Vec3 start = player.getEyePosition();
         Vec3 end = start.add(player.getLookAngle().scale(player.blockInteractionRange()));
         BlockHitResult hit = event.getLevel().clip(new ClipContext(start, end, Block.OUTLINE, Fluid.SOURCE_ONLY, player));
         if (hit.getType() == Type.BLOCK && event.getLevel().getBlockState(hit.getBlockPos()).getBlock() instanceof BlockIC2Fluid) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
         }
      }
   }
}
