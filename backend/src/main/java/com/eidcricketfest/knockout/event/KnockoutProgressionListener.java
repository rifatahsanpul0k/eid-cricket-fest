package com.eidcricketfest.knockout.event;

import com.eidcricketfest.knockout.service.KnockoutService;
import com.eidcricketfest.match.entity.MatchStage;
import com.eidcricketfest.match.event.MatchCompletedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;

@Component
public class KnockoutProgressionListener {

    private final KnockoutService knockoutService;

    public KnockoutProgressionListener(
            KnockoutService knockoutService
    ) {
        this.knockoutService = knockoutService;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void onMatchCompleted(
            MatchCompletedEvent event
    ) {

        if (event.stage()
                == MatchStage.SEMI_FINAL) {

            knockoutService
                    .generateFinalIfReady(
                            event.tournamentEditionId()
                    );

            return;
        }

        if (event.stage()
                == MatchStage.FINAL) {

            knockoutService
                    .completeEditionFromFinal(
                            event.matchId()
                    );
        }
    }
}
