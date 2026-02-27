package [GAME_PACKAGE].desktopmodule;

import [GAME_PACKAGE].game.[GAME_NAME];
import com.jme3.system.AppSettings;

/**
 * Used to launch a jme application in desktop environment
 *
 */
public class DesktopLauncher {
    public static void main(String[] args) {
        final [GAME_NAME] game = new [GAME_NAME]();

        final AppSettings appSettings = new AppSettings(true);

        game.setSettings(appSettings);
        [IF=MACOS]game.setShowSettings(false); //Settings dialog not supported on mac[/IF=MACOS]
        game.start();
    }
}