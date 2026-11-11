package ic2.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class NativeMenus {
   private static final ThreadLocal<Integer> id = ThreadLocal.withInitial(() -> 0);
   private static final Method nextCounter = method("nextContainerCounter");
   private static final Method init = method("initMenu", AbstractContainerMenu.class);
   private static final Field counter = field("containerCounter");

   private NativeMenus() {
   }

   public static int currentId() {
      return id.get();
   }

   private static Method method(String name, Class<?>... args) {
      try {
         Method m = ServerPlayer.class.getDeclaredMethod(name, args);
         m.setAccessible(true);
         return m;
      } catch (ReflectiveOperationException var3) {
         throw new ExceptionInInitializerError(var3);
      }
   }

   private static Field field(String name) {
      try {
         Field f = ServerPlayer.class.getDeclaredField(name);
         f.setAccessible(true);
         return f;
      } catch (ReflectiveOperationException var2) {
         throw new ExceptionInInitializerError(var2);
      }
   }

   public static <T> T create(int menuId, Supplier<T> factory) {
      int previous = id.get();
      id.set(menuId);

      Object var3;
      try {
         var3 = factory.get();
      } finally {
         id.set(previous);
      }

      return (T)var3;
   }

   public static boolean launch(ServerPlayer player, IHasGui inventory, Integer subId) {
      player.closeContainer();

      try {
         nextCounter.invoke(player);
         int menuId = counter.getInt(player);
         IC2.network.get(true).initiateGuiDisplay(player, inventory, menuId, subId);
         ContainerBase<?> menu = create(menuId, () -> inventory.getGuiContainer(player));
         player.containerMenu = menu;
         menu.addViewer(player);
         init.invoke(player, menu);
         return true;
      } catch (ReflectiveOperationException var5) {
         throw new IllegalStateException("Cannot initialize the native IC2 menu", var5);
      }
   }

   public static boolean launchClient(int menuId, Player player, IHasGui inventory, boolean admin) {
      return create(menuId, () -> IC2.platform.launchGuiClient(player, inventory, admin));
   }
}
