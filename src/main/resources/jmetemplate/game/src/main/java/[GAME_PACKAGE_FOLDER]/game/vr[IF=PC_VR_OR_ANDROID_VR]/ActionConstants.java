package [GAME_PACKAGE].game.vr;

import com.onemillionworlds.tamarin.actions.actionprofile.ActionHandle;
public class ActionConstants {

    // different action sets can be active at different times to allow easy remapping of buttons to actions for
    // different phases of the application

    public static String CORE_ACTION_SET = "core";

    public static ActionHandle HAPTIC = new ActionHandle(CORE_ACTION_SET, "haptic");

    public static ActionHandle GRIP_POSE = new ActionHandle(CORE_ACTION_SET, "grip_pose");
}