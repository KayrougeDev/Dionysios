package fr.kayrouge.dionysios;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class GameSettings {

    private int minPlayerCount = 1;
    private int minPlayerToStopGame = 0;
    private int maxPlayerCount = 15;
    private long timeToTerminate = 30L;
    private int id;
    private boolean isIdDefine = false;
    private boolean tpAfterGameTerminated = true;

    protected void setId(int id) {
        if (isIdDefine) {
            throw new IllegalStateException("Game ID can't be edited");
        }
        this.id = id;
        this.isIdDefine = true;
    }

}
