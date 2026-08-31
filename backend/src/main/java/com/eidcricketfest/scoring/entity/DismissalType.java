package com.eidcricketfest.scoring.entity;

public enum DismissalType {

    BOWLED(true),
    CAUGHT(true),
    LBW(true),
    RUN_OUT(false),

    STUMPED(true),
    HIT_WICKET(true),

    HIT_BALL_TWICE(false),
    OBSTRUCTING_FIELD(false);

    private final boolean bowlerCredited;

    DismissalType(boolean bowlerCredited) {
        this.bowlerCredited = bowlerCredited;
    }

    public boolean isBowlerCredited() {
        return bowlerCredited;
    }
}
