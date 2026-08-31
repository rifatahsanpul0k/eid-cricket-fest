package com.eidcricketfest.scoring.realtime;

import com.eidcricketfest.scoring.dto.LiveMatchResponse;
import com.eidcricketfest.scoring.event.MatchScoreChangedEvent;
import com.eidcricketfest.scoring.service.LiveScoreService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;

@Component
public class ScoreBroadcastListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final LiveScoreService liveScoreService;

    public ScoreBroadcastListener(
            SimpMessagingTemplate messagingTemplate,
            LiveScoreService liveScoreService
    ) {
        this.messagingTemplate = messagingTemplate;
        this.liveScoreService = liveScoreService;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void onScoreChanged(
            MatchScoreChangedEvent event
    ) {

        LiveMatchResponse score =
                liveScoreService.getLiveMatch(
                        event.matchId()
                );

        messagingTemplate.convertAndSend(
                "/topic/matches/"
                        + event.matchId(),
                score
        );
    }
}
