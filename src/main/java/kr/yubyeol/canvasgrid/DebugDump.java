package kr.yubyeol.canvasgrid;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.map.MapState;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Writes the numbers behind the overlay to a text file, for bug reports about misalignment. */
final class DebugDump {

  private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");

  private DebugDump() {}

  /** Returns the path written, or {@code null} when there is nothing to dump or writing failed. */
  static Path write(MinecraftClient client, CanvasTracker tracker) {
    ClientPlayerEntity player = client.player;
    CanvasTracker.Canvas canvas = tracker.canvas();
    if (player == null || canvas == null) {
      return null;
    }
    StringBuilder text = new StringBuilder();
    line(text, "time", LocalDateTime.now().toString());
    line(text, "player.pos", vec(player.getPos()));
    line(text, "player.eye", vec(player.getEyePos()));
    line(text, "player.yaw", num(player.getYaw()));
    line(text, "player.pitch", num(player.getPitch()));
    line(text, "player.seated", String.valueOf(player.hasVehicle()));
    line(text, "camera.pos", vec(client.gameRenderer.getCamera().getPos()));

    ItemFrameEntity frame = canvas.frame();
    Box box = frame.getBoundingBox();
    line(text, "frame.pos", vec(frame.getPos()));
    line(text, "frame.box.min", vec(new Vec3d(box.minX, box.minY, box.minZ)));
    line(text, "frame.box.max", vec(new Vec3d(box.maxX, box.maxY, box.maxZ)));
    line(text, "frame.facing", canvas.facing().name());
    line(text, "frame.rotation", String.valueOf(frame.getRotation()));
    line(text, "frame.yaw", num(frame.getYaw()));
    line(text, "frame.invisible", String.valueOf(frame.isInvisible()));
    line(text, "cursor.yawOffset", String.valueOf(ArtMapCursor.yawOffset(canvas.facing())));
    int[] cursor = tracker.cursor();
    line(text, "cursor.pixel", cursor == null ? "off" : (cursor[0] + 1) + "," + (cursor[1] + 1));
    appendMapPixels(text, client, frame, cursor);

    Path dir = FabricLoader.getInstance().getConfigDir().resolve(CanvasGridMod.MOD_ID);
    Path file = dir.resolve("debug_" + LocalDateTime.now().format(STAMP) + ".txt");
    try {
      Files.createDirectories(dir);
      Files.writeString(file, text.toString(), StandardCharsets.UTF_8);
      return file;
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * Appends the map colour bytes around the cursor as a 9x9 canvas-pixel window, one row per
   * line, so a freshly painted pixel can be located relative to the highlighted one.
   */
  private static void appendMapPixels(StringBuilder text, MinecraftClient client,
      ItemFrameEntity frame, int[] cursor) {
    MapIdComponent mapId = frame.getHeldItemStack().get(DataComponentTypes.MAP_ID);
    MapState state = mapId == null || client.world == null ? null : client.world.getMapState(mapId);
    if (state == null) {
      line(text, "map", "no data");
      return;
    }
    line(text, "map.id", String.valueOf(mapId.id()));
    int cx = cursor == null ? 16 : cursor[0];
    int cy = cursor == null ? 16 : cursor[1];
    text.append("map.window (canvas pixels, rows top to bottom, * = cursor)\n");
    for (int y = cy - 4; y <= cy + 4; y++) {
      StringBuilder row = new StringBuilder();
      for (int x = cx - 4; x <= cx + 4; x++) {
        if (x < 0 || y < 0 || x >= 32 || y >= 32) {
          row.append("  --");
          continue;
        }
        int mapPixel = (x * 4 + 2) + (y * 4 + 2) * 128;
        int colour = state.colors[mapPixel] & 0xFF;
        row.append(x == cx && y == cy ? '*' : ' ').append(String.format(Locale.ROOT, "%3d", colour));
      }
      text.append(row).append('\n');
    }
  }

  private static void line(StringBuilder text, String key, String value) {
    text.append(key).append('=').append(value).append('\n');
  }

  private static String vec(Vec3d v) {
    return String.format(Locale.ROOT, "%.4f, %.4f, %.4f", v.x, v.y, v.z);
  }

  private static String num(float v) {
    return String.format(Locale.ROOT, "%.3f", v);
  }
}
