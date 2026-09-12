package kr.yubyeol.canvasgrid;

import net.minecraft.util.math.Direction;

/**
 * Maps the player's view angles to the canvas pixel that the ArtMap server plugin paints.
 *
 * <p>ArtMap ignores where the view ray actually hits the frame. It reads the seated player's yaw
 * and pitch, subtracts an offset that depends on the easel's facing, and looks both angles up in
 * a fixed 32x32 table. This class reproduces that lookup so the highlight matches the server.
 */
public final class ArtMapCursor {

  /** Canvas size in pixels along each axis. */
  public static final int SIZE = 32;

  private static final float EDGE_DEGREES = 45f;

  private ArtMapCursor() {}

  /** Yaw offset ArtMap applies for an easel whose item frame faces the given direction. */
  public static int yawOffset(Direction facing) {
    return switch (facing) {
      case SOUTH -> 180;
      case EAST, WEST -> 90;
      default -> 0;
    };
  }

  /**
   * Returns the pixel {@code {x, y}} the cursor sits on for the given raw view angles, or
   * {@code null} when ArtMap treats the view as off the canvas.
   */
  public static int[] pixelAt(float yaw, float pitch, int yawOffset) {
    float adjustedYaw = adjustYaw(yaw, yawOffset);
    if (Math.abs(adjustedYaw) > EDGE_DEGREES || Math.abs(pitch) > EDGE_DEGREES) {
      return null;
    }
    int x = columnFor(clampToTable(adjustedYaw));
    int y = rowFor(x, clampToTable(pitch));
    return new int[] {x, y};
  }

  private static float adjustYaw(float yaw, int yawOffset) {
    float wrapped = wrapDegrees(yaw);
    return wrapped > 0 ? wrapped - yawOffset : wrapped + yawOffset;
  }

  private static float wrapDegrees(float degrees) {
    float shifted = degrees + 180f;
    return (float) (shifted - Math.floor(shifted / 360f) * 360f) - 180f;
  }

  private static float clampToTable(float degrees) {
    return Math.max(-40f, Math.min(40f, degrees));
  }

  private static int columnFor(float adjustedYaw) {
    int x = 0;
    while (x < SIZE - 1 && adjustedYaw > CursorTable.YAW[x + 1]) {
      x++;
    }
    return x;
  }

  private static int rowFor(int column, float pitch) {
    float[] bounds = CursorTable.PITCH[column];
    int y = 0;
    while (y < SIZE - 1 && pitch > bounds[y + 1]) {
      y++;
    }
    return y;
  }
}
