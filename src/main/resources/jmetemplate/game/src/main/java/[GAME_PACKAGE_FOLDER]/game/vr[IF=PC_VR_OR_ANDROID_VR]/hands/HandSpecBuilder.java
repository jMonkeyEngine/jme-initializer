package [GAME_PACKAGE].game.vr.hands;

import [GAME_PACKAGE].game.vr.ActionConstants;
import com.onemillionworlds.tamarin.vrhands.HandSpec;
public class HandSpecBuilder {

    /**
     * The hand spec describes the openXR actions that are bound to the hand graphics
     * The hand model could also be changed here but the tamarin default is being used here
     */
    public static HandSpec handSpec(){
        return HandSpec.builder(
                ActionConstants.GRIP_POSE,
                ActionConstants.GRIP_POSE
        ).build();
    }
}