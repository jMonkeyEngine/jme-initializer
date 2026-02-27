package [GAME_PACKAGE].game.vr.manifest;

import [GAME_PACKAGE].game.vr.ActionConstants;
import com.onemillionworlds.tamarin.actions.ActionType;
import com.onemillionworlds.tamarin.actions.actionprofile.Action;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionManifest;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionSet;
public class ActionManifestBuilder {
    /**
     * Creates the application's action manifest. This maps physical controller attributes (like buttons)
     * to logical actions within the application.
     * <p>
     *     See https://github.com/oneMillionWorlds/Tamarin/wiki/Action-Based-Vr for details on creating a manifest
     * </p>
     */
    public static ActionManifest manifest() {

        Action gripPose = Action.builder()
                .actionHandle(ActionConstants.GRIP_POSE)
                .translatedName("Grip pose")
                .actionType(ActionType.POSE)
                .withSuggestAllKnownAimPoseBindings()
                .build();

        Action haptic = Action.builder()
                .actionHandle(ActionConstants.HAPTIC)
                .translatedName("Haptic")
                .actionType(ActionType.HAPTIC)
                .withSuggestAllKnownHapticBindings()
                .build();

        return ActionManifest.builder()
                .withActionSet(ActionSet.builder()
                        .name(ActionConstants.CORE_ACTION_SET)
                        .translatedName("Core Actions")
                        .priority(1)
                        .withAction(gripPose)
                        .withAction(haptic)
                        .build())
                .build();
    }
}