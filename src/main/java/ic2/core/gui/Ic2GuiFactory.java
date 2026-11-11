package ic2.core.gui;

import ic2.core.init.MainConfig;
import ic2.core.util.Config;
import ic2.core.util.ConfigUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class Ic2GuiFactory {
   public Screen createConfigGui(Screen parent) {
      return new Ic2GuiFactory.IC2ConfigGuiScreen(parent);
   }

   private static class AddUuScreen extends Screen {
      private final Ic2GuiFactory.IC2ConfigGuiScreen parent;
      private EditBox item;
      private EditBox amount;
      private String error = "";

      AddUuScreen(Ic2GuiFactory.IC2ConfigGuiScreen parent) {
         super(Component.literal("Add predefined UU value"));
         this.parent = parent;
      }

      protected void init() {
         this.item = (EditBox)this.addRenderableWidget(new EditBox(this.font, this.width / 2 - 140, 60, 280, 20, Component.literal("Item identifier")));
         this.item.setMaxLength(1024);
         this.amount = (EditBox)this.addRenderableWidget(new EditBox(this.font, this.width / 2 - 140, 100, 280, 20, Component.literal("UU value")));
         this.amount.setValue("0");
         this.addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("Add"), b -> {
            try {
               ConfigUtil.asStack(this.item.getValue());
               double value = Double.parseDouble(this.amount.getValue());
               if (!Double.isFinite(value) || value < 0.0) {
                  throw new IllegalArgumentException("UU value must be finite and non-negative");
               }

               String key = this.parent.path + this.item.getValue();
               this.parent.pending.put(key, this.amount.getValue());
               this.parent.entries.add(new Config.Value(this.item.getValue(), "Predefined UU value", this.amount.getValue()));
               this.onClose();
            } catch (Exception var5) {
               this.error = var5.getMessage();
            }
         }).bounds(this.width / 2 - 85, 145, 80, 20).build());
         this.addRenderableWidget(
            net.minecraft.client.gui.components.Button.builder(Component.literal("Cancel"), b -> this.onClose())
               .bounds(this.width / 2 + 5, 145, 80, 20)
               .build()
         );
      }

      public void onClose() {
         this.minecraft.gui.setScreen(this.parent);
      }

      public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float partialTick) {
         super.extractRenderState(graphics, x, y, partialTick);
         graphics.centeredText(this.font, this.title, this.width / 2, 20, -1);
         graphics.text(this.font, "Item identifier", this.item.getX(), 46, -1, false);
         graphics.text(this.font, "UU value", this.amount.getX(), 86, -1, false);
         graphics.text(this.font, this.error, 10, this.height - 25, -13210, false);
      }
   }

   public static class IC2ConfigGuiScreen extends Screen {
      private final Screen parent;
      private final Config category;
      private final String path;
      private final Map<String, String> pending;
      private final List<Object> entries = new ArrayList<>();
      private final List<Ic2GuiFactory.IC2ConfigGuiScreen.Preview> previews = new ArrayList<>();
      private int page;
      private String error = "";

      public IC2ConfigGuiScreen(Screen parent) {
         this(parent, MainConfig.get(), "", new LinkedHashMap<>());
      }

      private IC2ConfigGuiScreen(Screen parent, Config category, String path, Map<String, String> pending) {
         super(Component.literal(path.isEmpty() ? "IC2 Configuration" : "IC2: " + path));
         this.parent = parent;
         this.category = category;
         this.path = path;
         this.pending = pending;
         category.sectionIterator().forEachRemaining(this.entries::add);
         category.valueIterator().forEachRemaining(value -> {
            if (MainConfig.getDefault(path + value.name) != null || path.equals("balance/uu-values/predefined/")) {
               this.entries.add(value);
            }
         });
      }

      protected void init() {
         this.previews.clear();
         int rows = Math.max(1, (this.height - 110) / 42);
         int left = Math.max(10, (this.width - 420) / 2);
         int editorWidth = Math.max(80, Math.min(280, this.width - left - 145));
         int first = this.page * rows;

         for (int i = first; i < Math.min(this.entries.size(), first + rows); i++) {
            Object entry = this.entries.get(i);
            int y = 38 + (i - first) * 42;
            if (entry instanceof Config section) {
               this.addRenderableWidget(
                  net.minecraft.client.gui.components.Button.builder(
                        Component.literal(section.name + " >"),
                        b -> this.minecraft.gui.setScreen(new Ic2GuiFactory.IC2ConfigGuiScreen(this, section, this.path + section.name + "/", this.pending))
                     )
                     .bounds(left, y + 12, editorWidth, 20)
                     .build()
               );
            } else {
               Config.Value value = (Config.Value)entry;
               String key = this.path + value.name;
               EditBox editor = new EditBox(this.font, left, y + 12, editorWidth, 20, Component.literal(value.name));
               editor.setMaxLength(32767);
               editor.setValue(this.pending.getOrDefault(key, value.value));
               editor.setResponder(text -> this.pending.put(key, text));
               this.addRenderableWidget(editor);
               Config.Value defaults = MainConfig.getDefault(key);
               if (defaults != null) {
                  this.addRenderableWidget(
                     net.minecraft.client.gui.components.Button.builder(Component.literal("Default"), b -> editor.setValue(defaults.value))
                        .bounds(left + editorWidth + 8, y + 12, 64, 20)
                        .build()
                  );
               } else {
                  this.addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("Delete"), b -> {
                     this.pending.put(key, null);
                     editor.setEditable(false);
                  }).bounds(left + editorWidth + 8, y + 12, 64, 20).build());
               }

               editor.setTooltip(Tooltip.create(Component.literal(value.comment)));
               this.previews.add(new Ic2GuiFactory.IC2ConfigGuiScreen.Preview(value.name, editor, y));
            }
         }

         int bottom = this.height - 55;
         ((net.minecraft.client.gui.components.Button)this.addRenderableWidget(
               net.minecraft.client.gui.components.Button.builder(Component.literal("<"), b -> {
                  this.page--;
                  this.rebuildWidgets();
               }).bounds(left, bottom, 30, 20).build()
            ))
            .active = this.page > 0;
         ((net.minecraft.client.gui.components.Button)this.addRenderableWidget(
               net.minecraft.client.gui.components.Button.builder(Component.literal(">"), b -> {
                  this.page++;
                  this.rebuildWidgets();
               }).bounds(left + 35, bottom, 30, 20).build()
            ))
            .active = first + rows < this.entries.size();
         this.addRenderableWidget(
            net.minecraft.client.gui.components.Button.builder(Component.literal("Save"), b -> this.save()).bounds(this.width / 2 - 40, bottom, 80, 20).build()
         );
         this.addRenderableWidget(
            net.minecraft.client.gui.components.Button.builder(Component.literal("Back"), b -> this.onClose()).bounds(this.width - 90, bottom, 80, 20).build()
         );
         if (this.path.equals("balance/uu-values/predefined/")) {
            this.addRenderableWidget(
               net.minecraft.client.gui.components.Button.builder(
                     Component.literal("Add UU value"), b -> this.minecraft.gui.setScreen(new Ic2GuiFactory.AddUuScreen(this))
                  )
                  .bounds(left + 75, bottom, 105, 20)
                  .build()
            );
         }
      }

      private void save() {
         try {
            for (Entry<String, String> entry : this.pending.entrySet()) {
               String key = entry.getKey();
               String value = entry.getValue();
               if (value != null) {
                  Config.Value defaults = MainConfig.getDefault(key);
                  if (key.startsWith("balance/uu-values/predefined/")) {
                     if (!Double.isFinite(Double.parseDouble(value)) || Double.parseDouble(value) < 0.0) {
                        throw new IllegalArgumentException("Invalid UU value: " + key);
                     }

                     ConfigUtil.asStack(key.substring("balance/uu-values/predefined/".length()));
                  } else if (defaults != null) {
                     String initial = defaults.value.trim();
                     if (!initial.equalsIgnoreCase("true") && !initial.equalsIgnoreCase("false")) {
                        if (initial.matches("[+-]?\\d+")) {
                           Long.parseLong(value.trim());
                        } else if (initial.matches("[+-]?\\d+\\.\\d+") && !Double.isFinite(Double.parseDouble(value.trim()))) {
                           throw new IllegalArgumentException("Expected finite number: " + key);
                        }
                     } else if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                        throw new IllegalArgumentException("Expected true or false: " + key);
                     }
                  }
               }
            }

            for (Entry<String, String> entryx : this.pending.entrySet()) {
               if (entryx.getValue() == null) {
                  MainConfig.get().remove(entryx.getKey());
               } else {
                  MainConfig.get().set(entryx.getKey(), entryx.getValue());
               }
            }

            MainConfig.save();
            this.pending.clear();
            this.error = "Saved. Restart to apply settings that require reinitialization.";
         } catch (Exception var7) {
            this.error = var7.getMessage();
         }
      }

      public void onClose() {
         this.minecraft.gui.setScreen(this.parent);
      }

      public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
         super.extractRenderState(graphics, mouseX, mouseY, partialTick);
         graphics.centeredText(this.font, this.title, this.width / 2, 14, -1);

         for (Ic2GuiFactory.IC2ConfigGuiScreen.Preview preview : this.previews) {
            graphics.text(this.font, preview.name, preview.editor.getX(), preview.y, -1, false);
            String item = this.path.equals("balance/uu-values/predefined/") ? preview.name : preview.editor.getValue().split(",", 2)[0].trim();
            if (item.contains(":")) {
               try {
                  ItemStack stack = ConfigUtil.asStack(item);
                  if (!stack.isEmpty()) {
                     graphics.item(stack, preview.editor.getX() + preview.editor.getWidth() + 82, preview.y + 14);
                  }
               } catch (Exception var9) {
               }
            }
         }

         if (!this.error.isEmpty()) {
            graphics.text(this.font, this.error, 10, this.height - 25, -13210, false);
         }
      }

      private record Preview(String name, EditBox editor, int y) {
      }
   }
}
