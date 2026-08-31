package com.eidcricketfest.match.repository;

import com.eidcricketfest.match.entity.CricketMatch;
import com.eidcricketfest.match.entity.MatchStage;
import com.eidcricketfest.match.entity.MatchStatus;
import org.springframework.data.jpa.domain.Specification;

public final class CricketMatchSpecifications {

    private CricketMatchSpecifications() {
    }

    public static Specification<CricketMatch> edition(
            Long editionId
    ) {

        return (root, query, cb) ->
                cb.equal(
                        root.get("tournamentEdition")
                                .get("id"),
                        editionId
                );
    }

    public static Specification<CricketMatch> status(
            MatchStatus status
    ) {

        if (status == null) {
            return Specification.unrestricted();
        }

        return (root, query, cb) ->
                cb.equal(
                        root.get("status"),
                        status
                );
    }

    public static Specification<CricketMatch> stage(
            MatchStage stage
    ) {

        if (stage == null) {
            return Specification.unrestricted();
        }

        return (root, query, cb) ->
                cb.equal(
                        root.get("stage"),
                        stage
                );
    }

    public static Specification<CricketMatch> involvesTeam(
            Long tournamentTeamId
    ) {

        if (tournamentTeamId == null) {
            return Specification.unrestricted();
        }

        return (root, query, cb) ->
                cb.or(
                        cb.equal(
                                root.get("teamA")
                                        .get("id"),
                                tournamentTeamId
                        ),

                        cb.equal(
                                root.get("teamB")
                                        .get("id"),
                                tournamentTeamId
                        )
                );
    }
}
