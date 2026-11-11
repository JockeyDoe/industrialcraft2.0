package ic2.core;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public interface IHasGui extends Container {
   String getName();

   ContainerBase<?> getGuiContainer(Player var1);

   @OnlyIn(Dist.CLIENT)
   Screen getGui(Player var1, boolean var2);

   void onGuiClosed(Player var1);
}
