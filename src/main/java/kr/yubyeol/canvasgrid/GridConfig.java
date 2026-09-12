package kr.yubyeol.canvasgrid;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;

/** User settings, stored as a plain properties file in the config directory. */
public final class GridConfig {

  /** Named colours offered in the settings screen. */
  public enum Palette {
    BLACK(0x000000),
    WHITE(0xFFFFFF),
    GRAY(0x808080),
    RED(0xFF2020),
    ORANGE(0xFF8C00),
    YELLOW(0xFFE000),
    GREEN(0x20D020),
    CYAN(0x20D0FF),
    BLUE(0x2060FF),
    MAGENTA(0xFF40FF);

    public final int rgb;

    Palette(int rgb) {
      this.rgb = rgb;
    }

    public Text label() {
      return Text.translatable("option." + CanvasGridMod.MOD_ID + ".color." + name().toLowerCase());
    }
  }

  /** Screen corner for the pixel coordinate text. */
  public enum HudCorner {
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    TOP_LEFT,
    TOP_RIGHT;

    public Text label() {
      return Text.translatable("option." + CanvasGridMod.MOD_ID + ".corner." + name().toLowerCase());
    }
  }

  public static final int MIN_LINE_WIDTH = 1;
  public static final int MAX_LINE_WIDTH = 5;
  public static final int[] BOLD_INTERVALS = {2, 4, 8, 16};

  private static final String FILE_NAME = CanvasGridMod.MOD_ID + ".properties";

  public boolean enabled = true;
  public boolean crispCanvas = true;
  public boolean showLabels = true;
  public boolean showCursor = true;
  public boolean showHud = true;
  public HudCorner hudCorner = HudCorner.BOTTOM_LEFT;
  /** Line thickness step, 1 (hairline) to 5 (thick). */
  public int lineWidth = 2;
  /** Opacity of the thin lines, 0-100 percent. */
  public int lineOpacity = 45;
  public Palette gridColor = Palette.BLACK;
  public boolean boldLines = true;
  /** Every n-th grid line is drawn bold and labelled. */
  public int boldEvery = 4;
  public Palette highlightColor = Palette.RED;
  /** Pointer outline thickness step, 1 (hairline) to 5 (thick). */
  public int pointerWidth = 3;
  /** How far (blocks) from the player to look for an easel canvas. */
  public double searchRadius = 6.0;
  /** Extra distance (blocks) to move the overlay out of the map plane, for odd frame setups. */
  public double surfaceOffset = 0.0;

  public static GridConfig load() {
    GridConfig config = new GridConfig();
    Path file = path();
    if (!Files.exists(file)) {
      config.save();
      return config;
    }
    Properties p = new Properties();
    try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      p.load(reader);
    } catch (IOException e) {
      return config;
    }
    config.enabled = bool(p, "enabled", config.enabled);
    config.crispCanvas = bool(p, "crispCanvas", config.crispCanvas);
    config.showLabels = bool(p, "showLabels", config.showLabels);
    config.showCursor = bool(p, "showCursor", config.showCursor);
    config.showHud = bool(p, "showHud", config.showHud);
    config.hudCorner = enumValue(p, "hudCorner", HudCorner.class, config.hudCorner);
    config.lineWidth = clamp(integer(p, "lineWidth", config.lineWidth), MIN_LINE_WIDTH,
        MAX_LINE_WIDTH);
    config.lineOpacity = clamp(integer(p, "lineOpacity", config.lineOpacity), 0, 100);
    config.gridColor = enumValue(p, "gridColor", Palette.class, config.gridColor);
    config.boldLines = bool(p, "boldLines", config.boldLines);
    config.boldEvery = nearestInterval(integer(p, "boldEvery", config.boldEvery));
    config.highlightColor = enumValue(p, "highlightColor", Palette.class, config.highlightColor);
    config.pointerWidth = clamp(integer(p, "pointerWidth", config.pointerWidth), MIN_LINE_WIDTH,
        MAX_LINE_WIDTH);
    config.searchRadius = clamp(decimal(p, "searchRadius", config.searchRadius), 2, 16);
    config.surfaceOffset = clamp(decimal(p, "surfaceOffset", config.surfaceOffset), -0.1, 0.1);
    return config;
  }

  public void save() {
    Properties p = new Properties();
    p.setProperty("enabled", String.valueOf(enabled));
    p.setProperty("crispCanvas", String.valueOf(crispCanvas));
    p.setProperty("showLabels", String.valueOf(showLabels));
    p.setProperty("showCursor", String.valueOf(showCursor));
    p.setProperty("showHud", String.valueOf(showHud));
    p.setProperty("hudCorner", hudCorner.name());
    p.setProperty("lineWidth", String.valueOf(lineWidth));
    p.setProperty("lineOpacity", String.valueOf(lineOpacity));
    p.setProperty("gridColor", gridColor.name());
    p.setProperty("boldLines", String.valueOf(boldLines));
    p.setProperty("boldEvery", String.valueOf(boldEvery));
    p.setProperty("highlightColor", highlightColor.name());
    p.setProperty("pointerWidth", String.valueOf(pointerWidth));
    p.setProperty("searchRadius", String.valueOf(searchRadius));
    p.setProperty("surfaceOffset", String.valueOf(surfaceOffset));
    try {
      Files.createDirectories(path().getParent());
      try (Writer writer = Files.newBufferedWriter(path(), StandardCharsets.UTF_8)) {
        p.store(writer, "YUBYEOL's Canvas Grid Mod");
      }
    } catch (IOException ignored) {
      // A missing config file only means the defaults are used next time.
    }
  }

  /** Restores every setting except the advanced surface offset. */
  public void resetToDefaults() {
    GridConfig defaults = new GridConfig();
    enabled = defaults.enabled;
    crispCanvas = defaults.crispCanvas;
    showLabels = defaults.showLabels;
    showCursor = defaults.showCursor;
    showHud = defaults.showHud;
    hudCorner = defaults.hudCorner;
    lineWidth = defaults.lineWidth;
    lineOpacity = defaults.lineOpacity;
    gridColor = defaults.gridColor;
    boldLines = defaults.boldLines;
    boldEvery = defaults.boldEvery;
    highlightColor = defaults.highlightColor;
    pointerWidth = defaults.pointerWidth;
    searchRadius = defaults.searchRadius;
  }

  /** The next bold interval in the fixed list, wrapping around. */
  public int nextBoldInterval() {
    for (int i = 0; i < BOLD_INTERVALS.length; i++) {
      if (BOLD_INTERVALS[i] == boldEvery) {
        return BOLD_INTERVALS[(i + 1) % BOLD_INTERVALS.length];
      }
    }
    return BOLD_INTERVALS[1];
  }

  private static int nearestInterval(int value) {
    int best = BOLD_INTERVALS[1];
    for (int interval : BOLD_INTERVALS) {
      if (Math.abs(interval - value) < Math.abs(best - value)) {
        best = interval;
      }
    }
    return best;
  }

  private static Path path() {
    return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
  }

  private static boolean bool(Properties p, String key, boolean fallback) {
    String value = p.getProperty(key);
    return value == null ? fallback : Boolean.parseBoolean(value.trim());
  }

  private static int integer(Properties p, String key, int fallback) {
    try {
      return Integer.parseInt(p.getProperty(key, String.valueOf(fallback)).trim());
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  private static double decimal(Properties p, String key, double fallback) {
    try {
      return Double.parseDouble(p.getProperty(key, String.valueOf(fallback)).trim());
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  private static <E extends Enum<E>> E enumValue(Properties p, String key, Class<E> type,
      E fallback) {
    String value = p.getProperty(key);
    if (value == null) {
      return fallback;
    }
    try {
      return Enum.valueOf(type, value.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      return fallback;
    }
  }

  private static int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }

  private static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }
}
