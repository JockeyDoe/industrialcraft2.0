package ic2.core.item.tool;

import ic2.core.IC2;
import ic2.core.ref.ItemName;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class PlasmaLauncher extends ItemElectricTool {
   public PlasmaLauncher() {
      super(ItemName.plasma_launcher, 100);
      this.maxCharge = 40000;
      this.transferLimit = 128;
      this.tier = 3;
   }

   @Override
   public InteractionResult use(Level world, Player player, InteractionHand hand) {
      if (!IC2.platform.isSimulating()) {
         return InteractionResult.PASS;
      } else {
         EntityParticle particle = new EntityParticle(world, player, 8.0F, 1.0, 2.0);
         world.addFreshEntity(particle);
         return super.use(world, player, hand);
      }
   }
}
