package kr.yubyeol.canvasgrid;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Lets Mod Menu open the settings screen from its mod list. Only loaded when Mod Menu is present. */
public final class ModMenuIntegration implements ModMenuApi {

  @Override
  public ConfigScreenFactory<?> getModConfigScreenFactory() {
    return SettingsScreen::new;
  }
}
