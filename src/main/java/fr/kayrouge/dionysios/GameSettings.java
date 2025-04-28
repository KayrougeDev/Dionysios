package fr.kayrouge.dionysios;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class GameSettings {

    private int minPlayerCount = 1;
    private int minPlayerToStopGame = 1;
    private int maxPlayerCount = 15;
    private long timeToTerminate = 30L;

    private int id;

    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.NONE)
    private boolean isIdDefine = false;

    /**

    Use $TIMELEFT to use time left before terminated

     */
    private String gameFinishedMessage = "Game finished, automatic quitting in $TIMELEFT seconds.";

    protected void setId(int id) {
        if (isIdDefine) {
            throw new IllegalStateException("Game ID can't be edited");
        }
        this.id = id;
        this.isIdDefine = true;
    }

}
