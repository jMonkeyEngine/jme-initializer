package [GAME_PACKAGE].android;

import com.jme3.app.AndroidHarness;
import [GAME_PACKAGE].game.[GAME_NAME];


public class AndroidLauncher extends AndroidHarness {

    public AndroidLauncher() {
        appClass = [GAME_NAME].class.getCanonicalName();
    }
}