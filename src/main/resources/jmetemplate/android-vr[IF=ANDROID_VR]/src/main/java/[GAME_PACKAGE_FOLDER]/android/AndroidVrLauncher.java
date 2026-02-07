package [GAME_PACKAGE].android;

import com.jme3.app.AndroidHarness;
import [GAME_PACKAGE].game.[GAME_NAME];

public class AndroidVrLauncher extends AndroidHarness {

    public AndroidVrLauncher() {
        appClass = [GAME_NAME]AndroidVr.class.getCanonicalName();
        eglSamples = 2;
    }
    @Override
    protected void onResume() {
        super.onResume();
        (([GAME_NAME]AndroidVr)getJmeApplication()).setAndroidActivity(this);
    }
}