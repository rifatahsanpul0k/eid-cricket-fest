package com.eidcricketfest.match.entity;

import java.io.Serializable;
import java.util.Objects;

public class MatchScorerId implements Serializable {

    private Long match;
    private Long user;

    public MatchScorerId() {}

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;

        if (!(o instanceof MatchScorerId that))
            return false;

        return Objects.equals(match, that.match)
                && Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(match, user);
    }
}
