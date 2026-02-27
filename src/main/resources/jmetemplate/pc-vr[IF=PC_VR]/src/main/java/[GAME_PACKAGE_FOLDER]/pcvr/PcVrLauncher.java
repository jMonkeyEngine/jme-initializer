package [GAME_PACKAGE].pcvr;

import [GAME_PACKAGE].game.[GAME_NAME];
import [GAME_PACKAGE].game.vr.hands.HandSpecBuilder;
import [GAME_PACKAGE].game.vr.manifest.ActionManifestBuilder;
import [GAME_PACKAGE].game.vr.ActionConstants;
import com.jme3.math.Vector3f;
import com.jme3.app.LostFocusBehavior;
import com.jme3.system.AppSettings;
import com.onemillionworlds.tamarin.openxr.XrAppState;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import com.onemillionworlds.tamarin.actions.XrActionAppState;
import com.onemillionworlds.tamarin.deferredattachment.DeferredAttachmentService;

/**
 * Used to launch a jme application in desktop VR environment
 *
 */
public class PcVrLauncher {
    public static void main(String[] args) {
        AppSettings settings = new AppSettings(true);
        settings.put("Renderer", AppSettings.LWJGL_OPENGL45); // OpenXR only supports relatively modern OpenGL
        settings.setTitle("[GAME_NAME_FULL]");
        settings.setVSync(false); // don't want to VSync to the monitor refresh rate, we want to VSync to the headset refresh rate

        XrAppState xrState = new XrAppState();
        xrState.movePlayersFeetToPosition(new Vector3f(0,0,10));
        xrState.playerLookAtPosition(new Vector3f(0,0,0));

        [GAME_NAME] app = new [GAME_NAME](
                xrState,
                new XrActionAppState(ActionManifestBuilder.manifest(), ActionConstants.CORE_ACTION_SET),
                new VRHandsAppState(HandSpecBuilder.handSpec()),
                new DeferredAttachmentService(2)
        );
        app.setLostFocusBehavior(LostFocusBehavior.Disabled);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }

}