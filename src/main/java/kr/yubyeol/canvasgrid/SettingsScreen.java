package kr.yubyeol.canvasgrid;

import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

/** In-game settings, opened from Mod Menu or the settings key. Every change is saved at once. */
public final class SettingsScreen extends Screen {

  private static final int BUTTON_WIDTH = 150;
  private static final int BUTTON_HEIGHT = 20;
  private static final int COLUMN_GAP = 10;
  private static final int ROW_HEIGHT = 24;
  private static final int TOP = 36;

  private final Screen parent;
  private final GridConfig config;

  public SettingsScreen(Screen parent) {
    this(parent, CanvasGridMod.config());
  }

  SettingsScreen(Screen parent, GridConfig config) {
    super(Text.translatable("screen." + CanvasGridMod.MOD_ID + ".title"));
    this.parent = parent;
    this.config = config;
  }

  @Override
  protected void init() {
    int left = width / 2 - BUTTON_WIDTH - COLUMN_GAP / 2;
    int right = width / 2 + COLUMN_GAP / 2;
    int row = 0;

    addToggle(left, row, "enabled", config.enabled, v -> config.enabled = v);
    addToggle(right, row++, "crisp_canvas", config.crispCanvas, v -> config.crispCanvas = v);

    addSlider(left, row, new LineWidthSlider());
    addSlider(right, row++, new OpacitySlider());

    addCycle(left, row, "grid_color", GridConfig.Palette.values(), config.gridColor,
        GridConfig.Palette::label, v -> config.gridColor = v);
    addCycle(right, row++, "highlight_color", GridConfig.Palette.values(), config.highlightColor,
        GridConfig.Palette::label, v -> config.highlightColor = v);

    addToggle(left, row, "bold_lines", config.boldLines, v -> config.boldLines = v);
    addDrawableChild(ButtonWidget.builder(boldEveryText(), button -> {
      config.boldEvery = config.nextBoldInterval();
      config.save();
      button.setMessage(boldEveryText());
    }).dimensions(right, rowY(row++), BUTTON_WIDTH, BUTTON_HEIGHT)
        .tooltip(tooltip("bold_every")).build());

    addToggle(left, row, "show_labels", config.showLabels, v -> config.showLabels = v);
    addToggle(right, row++, "show_cursor", config.showCursor, v -> config.showCursor = v);

    addToggle(left, row, "show_hud", config.showHud, v -> config.showHud = v);
    addCycle(right, row++, "hud_corner", GridConfig.HudCorner.values(), config.hudCorner,
        GridConfig.HudCorner::label, v -> config.hudCorner = v);

    addSlider(left, row, new PointerWidthSlider());
    addSlider(right, row++, new RadiusSlider());

    row++;
    addDrawableChild(ButtonWidget.builder(option("reset"), button -> {
      config.resetToDefaults();
      config.save();
      clearAndInit();
    }).dimensions(left, rowY(row), BUTTON_WIDTH, BUTTON_HEIGHT)
        .tooltip(tooltip("reset")).build());
    addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> close())
        .dimensions(right, rowY(row), BUTTON_WIDTH, BUTTON_HEIGHT).build());
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    super.render(context, mouseX, mouseY, delta);
    context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 14, 0xFFFFFF);
  }

  @Override
  public void close() {
    config.save();
    client.setScreen(parent);
  }

  private void addToggle(int x, int row, String key, boolean initial,
      Consumer<Boolean> apply) {
    addDrawableChild(CyclingButtonWidget.onOffBuilder(initial)
        .tooltip(value -> tooltip(key))
        .build(x, rowY(row), BUTTON_WIDTH, BUTTON_HEIGHT, option(key), (button, value) -> {
          apply.accept(value);
          config.save();
        }));
  }

  private <T> void addCycle(int x, int row, String key, T[] values, T initial,
      Function<T, Text> toText, Consumer<T> apply) {
    addDrawableChild(CyclingButtonWidget.<T>builder(toText::apply)
        .values(values)
        .initially(initial)
        .tooltip(value -> tooltip(key))
        .build(x, rowY(row), BUTTON_WIDTH, BUTTON_HEIGHT, option(key), (button, value) -> {
          apply.accept(value);
          config.save();
        }));
  }

  private void addSlider(int x, int row, IntSlider slider) {
    slider.setX(x);
    slider.setY(rowY(row));
    slider.setTooltip(tooltip(slider.key));
    addDrawableChild(slider);
  }

  private Text boldEveryText() {
    return Text.translatable("option." + CanvasGridMod.MOD_ID + ".bold_every", config.boldEvery);
  }

  private static Text option(String key) {
    return Text.translatable("option." + CanvasGridMod.MOD_ID + "." + key);
  }

  private static Tooltip tooltip(String key) {
    return Tooltip.of(Text.translatable("option." + CanvasGridMod.MOD_ID + "." + key + ".tooltip"));
  }

  private static int rowY(int row) {
    return TOP + row * ROW_HEIGHT;
  }

  /** Slider over an integer range that writes straight into the config. */
  private abstract class IntSlider extends SliderWidget {
    final String key;
    private final int min;
    private final int max;

    IntSlider(String key, int min, int max, int initial) {
      super(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Text.empty(),
          (double) (initial - min) / (max - min));
      this.key = key;
      this.min = min;
      this.max = max;
      updateMessage();
    }

    int intValue() {
      return (int) Math.round(min + value * (max - min));
    }

    @Override
    protected void updateMessage() {
      setMessage(Text.translatable("option." + CanvasGridMod.MOD_ID + "." + key, intValue()));
    }

    @Override
    protected void applyValue() {
      apply(intValue());
      config.save();
    }

    abstract void apply(int value);
  }

  private final class LineWidthSlider extends IntSlider {
    LineWidthSlider() {
      super("line_width", GridConfig.MIN_LINE_WIDTH, GridConfig.MAX_LINE_WIDTH, config.lineWidth);
    }

    @Override
    void apply(int value) {
      config.lineWidth = value;
    }
  }

  private final class OpacitySlider extends IntSlider {
    OpacitySlider() {
      super("line_opacity", 0, 100, config.lineOpacity);
    }

    @Override
    void apply(int value) {
      config.lineOpacity = value;
    }
  }

  private final class PointerWidthSlider extends IntSlider {
    PointerWidthSlider() {
      super("pointer_width", GridConfig.MIN_LINE_WIDTH, GridConfig.MAX_LINE_WIDTH,
          config.pointerWidth);
    }

    @Override
    void apply(int value) {
      config.pointerWidth = value;
    }
  }

  private final class RadiusSlider extends IntSlider {
    RadiusSlider() {
      super("search_radius", 2, 16, (int) Math.round(config.searchRadius));
    }

    @Override
    void apply(int value) {
      config.searchRadius = value;
    }
  }
}
