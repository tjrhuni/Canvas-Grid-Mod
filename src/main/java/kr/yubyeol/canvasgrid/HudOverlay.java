package kr.yubyeol.canvasgrid;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

/** Small text in a screen corner with the pixel the view points at. */
public final class HudOverlay {

  private static final int MARGIN = 4;
  private static final int TEXT_COLOR = 0xFFFFFFFF;

  private final GridConfig config;
  private final CanvasTracker tracker;

  HudOverlay(GridConfig config, CanvasTracker tracker) {
    this.config = config;
    this.tracker = tracker;
  }

  void render(DrawContext context, RenderTickCounter tickCounter) {
    if (!config.enabled || !config.showHud || tracker.canvas() == null) {
      return;
    }
    MinecraftClient client = MinecraftClient.getInstance();
    if (client.options.hudHidden) {
      return;
    }
    int[] cursor = tracker.cursor();
    Text text = cursor == null
        ? Text.translatable("hud." + CanvasGridMod.MOD_ID + ".sit")
        : Text.translatable("hud." + CanvasGridMod.MOD_ID + ".pixel", cursor[0] + 1, cursor[1] + 1);
    int textWidth = client.textRenderer.getWidth(text);
    int textHeight = client.textRenderer.fontHeight;
    int x = switch (config.hudCorner) {
      case BOTTOM_LEFT, TOP_LEFT -> MARGIN;
      case BOTTOM_RIGHT, TOP_RIGHT -> context.getScaledWindowWidth() - textWidth - MARGIN;
    };
    int y = switch (config.hudCorner) {
      case TOP_LEFT, TOP_RIGHT -> MARGIN;
      case BOTTOM_LEFT, BOTTOM_RIGHT -> context.getScaledWindowHeight() - textHeight - MARGIN;
    };
    context.drawTextWithShadow(client.textRenderer, text, x, y, TEXT_COLOR);
  }
}
