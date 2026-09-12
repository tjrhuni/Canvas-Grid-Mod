package kr.yubyeol.canvasgrid;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Mod Menu 의 모드 목록에서 설정 화면을 열 수 있게 한다. Mod Menu 가 있을 때만 로드된다. */
public final class ModMenuIntegration implements ModMenuApi {

  @Override
  public ConfigScreenFactory<?> getModConfigScreenFactory() {
    return SettingsScreen::new;
  }
}
