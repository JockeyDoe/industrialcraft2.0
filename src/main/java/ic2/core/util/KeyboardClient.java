package ic2.core.util;

import ic2.core.IC2;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping.Category;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT)
public class KeyboardClient extends Keyboard {
   private static final Category keyCategory = Category.register(Identifier.fromNamespaceAndPath("ic2", "controls"));
   private static final KeyMapping altKey = new KeyMapping("ALT Key", 342, keyCategory);
   private static final KeyMapping boostKey = new KeyMapping("Boost Key", 341, keyCategory);
   private static final KeyMapping modeSwitchKey = new KeyMapping("Mode Switch Key", 77, keyCategory);
   private static final KeyMapping sideinventoryKey = new KeyMapping("Side Inventory Key", 67, keyCategory);
   private static final KeyMapping expandinfo = new KeyMapping("Hub Expand Key", 88, keyCategory);
   private static boolean registeredKeys = false;
   private int lastKeyState = 0;

   @SubscribeEvent
   public static void registerKeys(RegisterKeyMappingsEvent event) {
      event.register(altKey);
      event.register(boostKey);
      event.register(modeSwitchKey);
      event.register(sideinventoryKey);
      event.register(expandinfo);
   }

   @Override
   public void sendKeyUpdate() {
      Set<Keyboard.Key> keys = EnumSet.noneOf(Keyboard.Key.class);
      Screen currentScreen = Minecraft.getInstance().gui.screen();
      if (currentScreen == null) {
         if (altKey.isDown()) {
            keys.add(Keyboard.Key.alt);
         }

         if (boostKey.isDown()) {
            keys.add(Keyboard.Key.boost);
         }

         if (Minecraft.getInstance().options.keyUp.isDown()) {
            keys.add(Keyboard.Key.forward);
         }

         if (modeSwitchKey.isDown()) {
            keys.add(Keyboard.Key.modeSwitch);
         }

         if (Minecraft.getInstance().options.keyJump.isDown()) {
            keys.add(Keyboard.Key.jump);
         }

         if (sideinventoryKey.isDown()) {
            keys.add(Keyboard.Key.sideInventory);
         }

         if (expandinfo.isDown()) {
            keys.add(Keyboard.Key.hubMode);
         }

         for (Keyboard.IKeyWatcher watcher : this.watchers) {
            watcher.checkForKey(keys);
         }
      }

      int currentKeyState = Keyboard.Key.toInt(keys);
      if (currentKeyState != this.lastKeyState) {
         IC2.network.get(false).initiateKeyUpdate(currentKeyState);
         super.processKeyUpdate(IC2.platform.getPlayerInstance(), currentKeyState);
         this.lastKeyState = currentKeyState;
      }
   }
}
