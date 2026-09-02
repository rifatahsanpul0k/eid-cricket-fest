package com.eidcricketfest.match.service;

import com.eidcricketfest.auth.entity.*;
import com.eidcricketfest.auth.repository.UserRepository;
import com.eidcricketfest.common.exception.*;
import com.eidcricketfest.match.dto.*;
import com.eidcricketfest.match.entity.*;
import com.eidcricketfest.match.repository.*;
import com.eidcricketfest.registration.entity.PlayerRegistration;
import com.eidcricketfest.registration.repository.PlayerRegistrationRepository;
import com.eidcricketfest.team.entity.*;
import com.eidcricketfest.team.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class MatchSetupService {

    private final CricketMatchRepository matchRepository;
    private final MatchScorerRepository scorerRepository;
    private final MatchTossRepository tossRepository;
    private final PlayingXiEntryRepository playingXiRepository;
    private final MatchSideRepository matchSideRepository;

    private final TournamentTeamRepository tournamentTeamRepository;
    private final TeamRosterEntryRepository rosterRepository;

    private final PlayerRegistrationRepository registrationRepository;
    private final UserRepository userRepository;

    public MatchSetupService(
            CricketMatchRepository matchRepository,
            MatchScorerRepository scorerRepository,
            MatchTossRepository tossRepository,
            PlayingXiEntryRepository playingXiRepository,
            MatchSideRepository matchSideRepository,
            TournamentTeamRepository tournamentTeamRepository,
            TeamRosterEntryRepository rosterRepository,
            PlayerRegistrationRepository registrationRepository,
            UserRepository userRepository
    ) {
        this.matchRepository = matchRepository;
        this.scorerRepository = scorerRepository;
        this.tossRepository = tossRepository;
        this.playingXiRepository = playingXiRepository;
        this.matchSideRepository = matchSideRepository;
        this.tournamentTeamRepository = tournamentTeamRepository;
        this.rosterRepository = rosterRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
    }

    public void assignScorer(
            Long matchId,
            Long actorUserId,
            AssignScorerRequest request
    ) {

        CricketMatch match = findMatch(matchId);

        User scorer = findUser(request.scorerUserId());

        boolean scorerRole =
                scorer.getRoles()
                        .stream()
                        .anyMatch(role ->
                                role.getCode()
                                        == RoleCode.SCORER
                        );

        if (!scorerRole) {
            throw new ConflictException(
                    "Selected user does not have SCORER role"
            );
        }

        User actor = findUser(actorUserId);

        scorerRepository.save(
                new MatchScorer(
                        match,
                        scorer,
                        request.primary(),
                        actor
                )
        );

        refreshReadyState(match);
    }

    @Transactional(readOnly = true)
    public MatchSetupDetailsResponse setupDetails(
            Long matchId
    ) {

        CricketMatch match = findMatch(matchId);

        List<MatchSetupDetailsResponse.ScorerAssignment> scorers =
                scorerRepository
                        .findDetailedByMatchId(matchId)
                        .stream()
                        .map(scorer ->
                                new MatchSetupDetailsResponse.ScorerAssignment(
                                        scorer.getUser().getId(),
                                        scorer.getUser().getDisplayName(),
                                        scorer.getUser().getEmail(),
                                        scorer.isPrimaryScorer()
                                )
                        )
                        .toList();

        Map<Long, List<PlayingXiEntry>> playingXiByTeam =
                new LinkedHashMap<>();

        for (PlayingXiEntry entry :
                playingXiRepository.findDetailedByMatchId(matchId)) {

            playingXiByTeam
                    .computeIfAbsent(
                            entry.getMatchSide().getId(),
                            ignored -> new ArrayList<>()
                    )
                    .add(entry);
        }

        return new MatchSetupDetailsResponse(
                scorers,
                teamPlayingXi(
                        match.getTeamASide().getId(),
                        playingXiByTeam
                ),
                teamPlayingXi(
                        match.getTeamBSide().getId(),
                        playingXiByTeam
                )
        );
    }

    public void submitPlayingXi(
            Long matchId,
            Long tournamentTeamId,
            Long actorUserId,
            boolean privileged,
            SubmitPlayingXiRequest request
    ) {

        CricketMatch match = findMatch(matchId);

        TournamentTeam team =
                tournamentTeamRepository
                        .findDetailedById(tournamentTeamId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Tournament team not found"
                                )
                        );

        assertTeamInMatch(match, team);

        if (!privileged) {
            assertCaptain(team, actorUserId);
        }

        int required =
                match.requireTournamentEdition()
                        .getPlayingXiSize();

        Set<Long> uniqueIds =
                new LinkedHashSet<>(
                        request.registrationIds()
                );

        if (uniqueIds.size() != required) {
            throw new ConflictException(
                    "Exactly " + required
                            + " unique players are required"
            );
        }

        PlayerRegistration captain =
                team.getCaptainRegistration();

        if (captain == null
                || !uniqueIds.contains(captain.getId())) {

            throw new ConflictException(
                    "Team captain must be included in Playing XI"
            );
        }

        if (request.wicketkeeperRegistrationId() != null
                && !uniqueIds.contains(
                        request.wicketkeeperRegistrationId()
                )) {

            throw new ConflictException(
                    "Wicketkeeper must be in Playing XI"
            );
        }

        List<PlayerRegistration> registrations =
                new ArrayList<>();

        for (Long registrationId : uniqueIds) {

            PlayerRegistration registration =
                    registrationRepository
                            .findDetailedById(registrationId)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Player registration not found: "
                                                    + registrationId
                                    )
                            );

            boolean activeRoster =
                    rosterRepository
                            .existsByTournamentTeam_IdAndPlayerRegistration_IdAndStatus(
                                    team.getId(),
                                    registrationId,
                                    RosterEntryStatus.ACTIVE
                            );

            if (!activeRoster) {
                throw new ConflictException(
                        registration.getPlayer()
                                .getFullName()
                                + " is not on this team's active roster"
                );
            }

            registrations.add(registration);
        }

        playingXiRepository
                .deleteByMatch_IdAndMatchSide_Id(
                        matchId,
                        match.sideForTournamentTeam(team).getId()
                );

        for (PlayerRegistration registration : registrations) {

            boolean isCaptain =
                    registration.getId()
                            .equals(captain.getId());

            boolean isWicketkeeper =
                    request.wicketkeeperRegistrationId() != null
                            && registration.getId()
                            .equals(
                                    request.wicketkeeperRegistrationId()
                            );

            playingXiRepository.save(
                    new PlayingXiEntry(
                            match,
                            team,
                            registration,
                            isCaptain,
                            isWicketkeeper
                    )
            );
        }

        refreshReadyState(match);
    }

    public void recordToss(
            Long matchId,
            Long actorUserId,
            RecordTossRequest request
    ) {

        CricketMatch match = findMatch(matchId);

        if (tossRepository.existsByMatch_Id(matchId)) {
            throw new ConflictException(
                    "Toss has already been recorded"
            );
        }

        if (match.getStatus() != MatchStatus.READY) {
            throw new ConflictException(
                    "Match is not ready for toss"
            );
        }

        MatchSide winner =
                tossWinnerSide(match, request);

        User actor = findUser(actorUserId);

        tossRepository.save(
                new MatchToss(
                        match,
                        winner,
                        request.decision(),
                        actor
                )
        );

        match.markTossCompleted();
    }

    private MatchSide tossWinnerSide(
            CricketMatch match,
            RecordTossRequest request
    ) {

        if (request.winnerMatchSideId() != null) {
            MatchSide side =
                    matchSideRepository.findById(
                                    request.winnerMatchSideId()
                            )
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Match side not found"
                                    )
                            );

            if (!side.getMatch().getId().equals(match.getId())) {
                throw new ConflictException(
                        "Side does not participate in this match"
                );
            }

            return side;
        }

        if (request.winnerTournamentTeamId() == null) {
            throw new ConflictException(
                    "Toss winner is required"
            );
        }

        TournamentTeam winner =
                tournamentTeamRepository
                        .findDetailedById(
                                request.winnerTournamentTeamId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Tournament team not found"
                                )
                        );

        assertTeamInMatch(match, winner);

        return match.sideForTournamentTeam(winner);
    }

    private void refreshReadyState(
            CricketMatch match
    ) {

        long teamACount =
                playingXiRepository
                        .countByMatch_IdAndMatchSide_Id(
                                match.getId(),
                                match.getTeamASide().getId()
                        );

        long teamBCount =
                playingXiRepository
                        .countByMatch_IdAndMatchSide_Id(
                                match.getId(),
                                match.getTeamBSide().getId()
                        );

        boolean scorerAssigned =
                scorerRepository
                        .existsByMatch_Id(match.getId());

        int required =
                match.isTournament()
                        ? match.requireTournamentEdition()
                                .getPlayingXiSize()
                        : 2;

        if (teamACount >= required
                && teamBCount >= required
                && scorerAssigned) {

            match.markReady();
        }
    }

    private void assertCaptain(
            TournamentTeam team,
            Long userId
    ) {

        if (team.getCaptainRegistration() == null
                || team.getCaptainRegistration()
                .getPlayer()
                .getUser() == null
                || !team.getCaptainRegistration()
                .getPlayer()
                .getUser()
                .getId()
                .equals(userId)) {

            throw new ForbiddenException(
                    "Only this team's captain can submit its Playing XI"
            );
        }
    }

    private void assertTeamInMatch(
            CricketMatch match,
            TournamentTeam team
    ) {

        boolean valid =
                match.getTeamA().getId().equals(team.getId())
                ||
                match.getTeamB().getId().equals(team.getId());

        if (!valid) {
            throw new ConflictException(
                    "Team does not participate in this match"
            );
        }
    }

    private CricketMatch findMatch(Long id) {

        return matchRepository.findDetailedById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Match not found"
                        )
                );
    }

    private User findUser(Long id) {

        return userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );
    }

    private MatchSetupDetailsResponse.TeamPlayingXi teamPlayingXi(
            Long matchSideId,
            Map<Long, List<PlayingXiEntry>> playingXiByTeam
    ) {

        List<PlayingXiEntry> entries =
                playingXiByTeam.getOrDefault(
                        matchSideId,
                        List.of()
                );

        List<Long> registrationIds =
                entries.stream()
                        .map(entry ->
                                entry.getRegistration() != null
                                        ? entry.getRegistration().getId()
                                        : null
                        )
                        .filter(Objects::nonNull)
                        .toList();

        List<Long> playerIds =
                entries.stream()
                        .map(entry -> entry.getPlayer().getId())
                        .toList();

        Long wicketkeeperRegistrationId =
                entries.stream()
                        .filter(PlayingXiEntry::isWicketkeeper)
                        .map(entry ->
                                entry.getRegistration() != null
                                        ? entry.getRegistration().getId()
                                        : null
                        )
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null);

        Long wicketkeeperPlayerId =
                entries.stream()
                        .filter(PlayingXiEntry::isWicketkeeper)
                        .map(entry -> entry.getPlayer().getId())
                        .findFirst()
                        .orElse(null);

        return new MatchSetupDetailsResponse.TeamPlayingXi(
                matchSideId,
                entries.stream()
                        .map(PlayingXiEntry::getTournamentTeam)
                        .filter(Objects::nonNull)
                        .map(TournamentTeam::getId)
                        .findFirst()
                        .orElse(null),
                registrationIds,
                playerIds,
                wicketkeeperRegistrationId,
                wicketkeeperPlayerId
        );
    }
}
