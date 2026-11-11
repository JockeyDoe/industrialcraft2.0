package ic2.core.gui;

import com.google.common.base.Supplier;
import com.mojang.authlib.GameProfile;
import ic2.core.GuiIC2;
import ic2.core.util.VanillaVariants;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import org.apache.commons.lang3.StringUtils;

public class PlayerHead extends ItemImage {
   private static final Map<GameProfile, Supplier<ItemStack>> IMAGE_MAKER = new WeakHashMap<>();
   private final GameProfile player;

   public PlayerHead(GuiIC2<?> gui, int x, int y, GameProfile player) {
      this(gui, x, y, player, null);
   }

   private PlayerHead(GuiIC2<?> gui, int x, int y, GameProfile player, Void skip) {
      super(gui, x, y, IMAGE_MAKER.computeIfAbsent(player, profile -> {
         ItemStack skull = new ItemStack(VanillaVariants.skull(3), 1);
         skull.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));
         return () -> skull;
      }));
      this.player = player;
   }

   @Override
   protected List<String> getToolTip() {
      List<String> tooltip = super.getToolTip();
      if (StringUtils.isNotBlank(this.player.name())) {
         tooltip.add(this.player.name());
      }

      return tooltip;
   }
}
