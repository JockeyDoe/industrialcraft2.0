package ic2.core.util;

import ic2.core.block.state.IIdProvider;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.item.DyeColor;

public enum Ic2Color implements IIdProvider {
   black(DyeColor.BLACK, "dyeBlack"),
   blue(DyeColor.BLUE, "dyeBlue"),
   brown(DyeColor.BROWN, "dyeBrown"),
   cyan(DyeColor.CYAN, "dyeCyan"),
   gray(DyeColor.GRAY, "dyeGray"),
   green(DyeColor.GREEN, "dyeGreen"),
   light_blue(DyeColor.LIGHT_BLUE, "dyeLightBlue"),
   light_gray(DyeColor.LIGHT_GRAY, "dyeLightGray"),
   lime(DyeColor.LIME, "dyeLime"),
   magenta(DyeColor.MAGENTA, "dyeMagenta"),
   orange(DyeColor.ORANGE, "dyeOrange"),
   pink(DyeColor.PINK, "dyePink"),
   purple(DyeColor.PURPLE, "dyePurple"),
   red(DyeColor.RED, "dyeRed"),
   white(DyeColor.WHITE, "dyeWhite"),
   yellow(DyeColor.YELLOW, "dyeYellow");

   public static final Ic2Color[] values = values();
   private static final Map<DyeColor, Ic2Color> mcColorMap = new EnumMap<>(DyeColor.class);
   public final DyeColor mcColor;
   public final String oreDictDyeName;

   private Ic2Color(DyeColor mcColor, String oreDictDyeName) {
      this.mcColor = mcColor;
      this.oreDictDyeName = oreDictDyeName;
   }

   @Override
   public String getName() {
      return this.name();
   }

   @Override
   public int getId() {
      return this.ordinal();
   }

   public static Ic2Color get(DyeColor mcColor) {
      return mcColorMap.get(mcColor);
   }

   static {
      for (Ic2Color color : values) {
         mcColorMap.put(color.mcColor, color);
      }
   }
}
