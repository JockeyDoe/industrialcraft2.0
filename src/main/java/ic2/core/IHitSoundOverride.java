package ic2.core;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public interface IHitSoundOverride {
   @OnlyIn(Dist.CLIENT)
   String getHitSoundForBlock(LocalPlayer var1, Level var2, BlockPos var3, ItemStack var4);

   @OnlyIn(Dist.CLIENT)
   String getBreakSoundForBlock(LocalPlayer var1, Level var2, BlockPos var3, ItemStack var4);
}
