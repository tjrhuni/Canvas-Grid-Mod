package kr.yubyeol.canvasgrid;

import net.minecraft.util.math.Direction;

/**
 * 플레이어의 시선 각도를 ArtMap 서버 플러그인이 칠하는 캔버스 픽셀로 바꾼다.
 *
 * <p>ArtMap 은 시선이 액자에 실제로 닿는 지점을 보지 않는다. 앉은 플레이어의 yaw 와 pitch 를 읽어
 * 이젤 방향에 따른 오프셋을 뺀 뒤, 두 각도를 고정된 32x32 표에서 찾는다. 이 클래스는 그 조회를
 * 그대로 재현해서 테두리로 표시하는 픽셀이 서버가 칠하는 픽셀과 일치하게 한다.
 */
public final class ArtMapCursor {

  /** 캔버스 한 변의 픽셀 수. */
  public static final int SIZE = 32;

  private static final float EDGE_DEGREES = 45f;

  private ArtMapCursor() {}

  /** 액자가 바라보는 방향에 따라 ArtMap 이 yaw 에 적용하는 오프셋. */
  public static int yawOffset(Direction facing) {
    return switch (facing) {
      case SOUTH -> 180;
      case EAST, WEST -> 90;
      default -> 0;
    };
  }

  /**
   * 주어진 시선 각도에서 커서가 놓이는 픽셀 {@code {x, y}} 를 돌려준다. ArtMap 이 캔버스 밖으로
   * 판정하는 각도면 {@code null}.
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
