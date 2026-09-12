package kr.yubyeol.canvasgrid;

import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/** Entry point: key bindings, per-tick canvas lookup, and the two render hooks. */
public final class CanvasGridMod implements ClientModInitializer {

  public static final String MOD_ID = "yubyeol_canvas_grid";

  private static final String KEY_CATEGORY = "key." + MOD_ID + ".category";
  private static GridConfig config;

  private final KeyBinding toggleKey = key("toggle", GLFW.GLFW_KEY_G);
  private final KeyBinding settingsKey = key("settings", GLFW.GLFW_KEY_UNKNOWN);
  private final KeyBinding debugDumpKey = key("debug_dump", GLFW.GLFW_KEY_UNKNOWN);

  private CanvasTracker tracker;
  private GridRenderer gridRenderer;
  private HudOverlay hudOverlay;

  /** The live settings, loaded once at start-up. */
  public static GridConfig config() {
    return config;
  }

  @Override
  public void onInitializeClient() {
    config = GridConfig.load();
    tracker = new CanvasTracker(config);
    gridRenderer = new GridRenderer(config, tracker);
    hudOverlay = new HudOverlay(config, tracker);

    KeyBindingHelper.registerKeyBinding(toggleKey);
    KeyBindingHelper.registerKeyBinding(settingsKey);
    KeyBindingHelper.registerKeyBinding(debugDumpKey);

    ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    WorldRenderEvents.AFTER_TRANSLUCENT.register(gridRenderer::render);
    HudLayerRegistrationCallback.EVENT.register(layers -> layers.attachLayerAfter(
        IdentifiedLayer.CHAT, Identifier.of(MOD_ID, "cursor"), hudOverlay::render));
  }

  private void onTick(MinecraftClient client) {
    while (toggleKey.wasPressed()) {
      config.enabled = !config.enabled;
      config.save();
      showStatus(client, Text.translatable(
          "message." + MOD_ID + (config.enabled ? ".enabled" : ".disabled")));
    }
    while (settingsKey.wasPressed()) {
      client.setScreen(new SettingsScreen(client.currentScreen, config));
    }
    while (debugDumpKey.wasPressed()) {
      Path file = DebugDump.write(client, tracker);
      showStatus(client, file == null
          ? Text.translatable("message." + MOD_ID + ".debug_none")
          : Text.translatable("message." + MOD_ID + ".debug_saved", file.getFileName()));
    }
    tracker.update(client);
  }

  private static KeyBinding key(String name, int defaultKey) {
    return new KeyBinding("key." + MOD_ID + "." + name, InputUtil.Type.KEYSYM, defaultKey,
        KEY_CATEGORY);
  }

  private static void showStatus(MinecraftClient client, Text message) {
    if (client.player != null) {
      client.player.sendMessage(message, true);
    }
  }
}
