package com.eidcricketfest.match.service;

import com.eidcricketfest.common.dto.PageResponse;
import com.eidcricketfest.common.exception.*;
import com.eidcricketfest.common.web.PageableFactory;
import com.eidcricketfest.match.dto.*;
import com.eidcricketfest.match.entity.*;
import com.eidcricketfest.match.repository.*;
import com.eidcricketfest.player.entity.Player;
import com.eidcricketfest.player.repository.PlayerRepository;
import com.eidcricketfest.team.entity.TournamentTeam;
import com.eidcricketfest.team.repository.TournamentTeamRepository;
import com.eidcricketfest.tournament.entity.TournamentEdition;
import com.eidcricketfest.tournament.repository.TournamentEditionRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class FixtureService {

    private static final Map<String, String> MATCH_SORTS =
            Map.of(
                    "matchNumber",
                    "matchNumber",

                    "scheduledAt",
                    "scheduledAt",

                    "stage",
                    "stage",

                    "createdAt",
                    "createdAt"
            );

    private final CricketMatchRepository matchRepository;
    private final VenueRepository venueRepository;
    private final TournamentEditionRepository editionRepository;
    private final TournamentTeamRepository tournamentTeamRepository;
    private final MatchScorerRepository scorerRepository;
    private final PlayingXiEntryRepository playingXiRepository;
    private final MatchSideRepository matchSideRepository;
    private final MatchOperationAuditRepository auditRepository;
    private final PlayerRepository playerRepository;
    private final PageableFactory pageableFactory;

    public FixtureService(
            CricketMatchRepository matchRepository,
            VenueRepository venueRepository,
            TournamentEditionRepository editionRepository,
            TournamentTeamRepository tournamentTeamRepository,
            MatchScorerRepository scorerRepository,
            PlayingXiEntryRepository playingXiRepository,
            MatchSideRepository matchSideRepository,
            MatchOperationAuditRepository auditRepository,
            PlayerRepository playerRepository,
            PageableFactory pageableFactory
    ) {
        this.matchRepository = matchRepository;
        this.venueRepository = venueRepository;
        this.editionRepository = editionRepository;
        this.tournamentTeamRepository = tournamentTeamRepository;
        this.scorerRepository = scorerRepository;
        this.playingXiRepository = playingXiRepository;
        this.matchSideRepository = matchSideRepository;
        this.auditRepository = auditRepository;
        this.playerRepository = playerRepository;
        this.pageableFactory = pageableFactory;
    }

    public MatchResponse createFriendlyMatch(
            CreateFriendlyMatchRequest request
    ) {

        String teamAName =
                request.teamAName().trim();

        String teamBName =
                request.teamBName().trim();

        if (teamAName.equalsIgnoreCase(teamBName)) {
            throw new ConflictException(
                    "Friendly match sides must have different names"
            );
        }

        Set<Long> teamAPlayerIds =
                uniquePlayers(
                        request.teamAPlayerIds(),
                        "Team A"
                );

        Set<Long> teamBPlayerIds =
                uniquePlayers(
                        request.teamBPlayerIds(),
                        "Team B"
                );

        if (teamAPlayerIds.size() < 2
                || teamBPlayerIds.size() < 2) {
            throw new ConflictException(
                    "Each side needs at least two players"
            );
        }

        Set<Long> overlap =
                new HashSet<>(teamAPlayerIds);

        overlap.retainAll(teamBPlayerIds);

        if (!overlap.isEmpty()) {
            throw new ConflictException(
                    "A player cannot be on both sides"
            );
        }

        Venue venue =
                venueRepository
                        .findById(request.venueId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Venue not found"
                                )
                        );

        Map<Long, Player> playersById =
                playersById(
                        teamAPlayerIds,
                        teamBPlayerIds
                );

        CricketMatch match =
                matchRepository.save(
                        CricketMatch.friendly(
                                request.oversPerInnings(),
                                venue,
                                request.scheduledAt()
                        )
                );

        MatchSide sideA =
                matchSideRepository.save(
                        new MatchSide(
                                match,
                                MatchSideKey.A,
                                teamAName,
                                null
                        )
                );

        MatchSide sideB =
                matchSideRepository.save(
                        new MatchSide(
                                match,
                                MatchSideKey.B,
                                teamBName,
                                null
                        )
                );

        match.attachSides(sideA, sideB);

        seedFriendlyPlayingXi(
                match,
                sideA,
                teamAPlayerIds,
                playersById
        );

        seedFriendlyPlayingXi(
                match,
                sideB,
                teamBPlayerIds,
                playersById
        );

        return toResponse(match);
    }

    @Transactional(readOnly = true)
    public List<FriendlyPlayerOptionResponse> friendlyPlayerOptions() {

        return playerRepository
                .findAllDetailedForFriendlyOptions()
                .stream()
                .map(player -> new FriendlyPlayerOptionResponse(
                        player.getId(),
                        player.getFullName(),
                        player.getPhotoUrl(),
                        player.getPrimaryCategory() != null
                                ? player.getPrimaryCategory().getName()
                                : null,
                        player.getBattingStyle(),
                        player.getBowlingStyle()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> getFriendlyMatches() {
        return toResponses(
                matchRepository.findDetailedFriendlyMatches()
        );
    }

    public List<MatchResponse> generateRoundRobin(
            Long editionId,
            GenerateRoundRobinRequest request
    ) {

        TournamentEdition edition =
                editionRepository.findById(editionId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Tournament edition not found"
                                )
                        );

        if (matchRepository
                .existsByTournamentEdition_IdAndStage(
                        editionId,
                        MatchStage.LEAGUE
                )) {

            throw new ConflictException(
                    "League fixtures already exist"
            );
        }

        List<TournamentTeam> teams =
                new ArrayList<>(
                        tournamentTeamRepository
                                .findDetailedByEditionId(editionId)
                );

        if (teams.size() < 2) {
            throw new ConflictException(
                    "At least two teams are required"
            );
        }

        Venue venue = null;

        if (request.venueId() != null) {
            venue = venueRepository
                    .findById(request.venueId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Venue not found"
                            )
                    );
        }

        /*
         * Odd number of teams:
         * add null as a BYE slot.
         */
        if (teams.size() % 2 != 0) {
            teams.add(null);
        }

        int size = teams.size();
        int rounds = size - 1;

        int matchNumber =
                matchRepository.findMaxMatchNumber(editionId) + 1;

        List<CricketMatch> generated =
                new ArrayList<>();

        for (int round = 1; round <= rounds; round++) {

            for (int i = 0; i < size / 2; i++) {

                TournamentTeam teamA = teams.get(i);
                TournamentTeam teamB =
                        teams.get(size - 1 - i);

                /*
                 * BYE
                 */
                if (teamA == null || teamB == null) {
                    continue;
                }

                CricketMatch match =
                        new CricketMatch(
                                edition,
                                teamA,
                                teamB,
                                MatchStage.LEAGUE,
                                round,
                                matchNumber++,
                                edition.getOversPerInnings(),
                                venue
                        );

                generated.add(match);
            }

            /*
             * Circle method:
             * first team fixed,
             * rotate the remaining teams.
             */
            TournamentTeam last =
                    teams.remove(size - 1);

            teams.add(1, last);
        }

        List<CricketMatch> saved =
                matchRepository.saveAll(generated);

        for (CricketMatch match : saved) {
            attachTournamentSides(match);
        }

        return toResponses(saved);
    }

    private Set<Long> uniquePlayers(
            List<Long> playerIds,
            String sideName
    ) {

        Set<Long> unique =
                new LinkedHashSet<>(playerIds);

        if (unique.size() != playerIds.size()) {
            throw new ConflictException(
                    sideName + " contains duplicate players"
            );
        }

        return unique;
    }

    @SafeVarargs
    private Map<Long, Player> playersById(
            Set<Long>... playerIdSets
    ) {

        Set<Long> allIds =
                new LinkedHashSet<>();

        for (Set<Long> playerIds : playerIdSets) {
            allIds.addAll(playerIds);
        }

        Map<Long, Player> players =
                new HashMap<>();

        for (Player player : playerRepository.findAllById(allIds)) {
            players.put(player.getId(), player);
        }

        if (players.size() != allIds.size()) {
            throw new ResourceNotFoundException(
                    "One or more players were not found"
            );
        }

        return players;
    }

    private void seedFriendlyPlayingXi(
            CricketMatch match,
            MatchSide side,
            Set<Long> playerIds,
            Map<Long, Player> playersById
    ) {

        boolean captain = true;

        for (Long playerId : playerIds) {
            playingXiRepository.save(
                    new PlayingXiEntry(
                            match,
                            side,
                            playersById.get(playerId),
                            captain,
                            false
                    )
            );

            captain = false;
        }
    }

    public MatchResponse scheduleMatch(
            Long matchId,
            ScheduleMatchRequest request
    ) {

        CricketMatch match =
                findMatch(matchId);

        Venue venue =
                venueRepository.findById(
                                request.venueId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Venue not found"
                                )
                        );

        match.schedule(
                request.scheduledAt(),
                venue
        );

        return toResponse(match);
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> getEditionMatches(
            Long editionId
    ) {

        return toResponses(
                matchRepository.findDetailedByEditionId(editionId)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<MatchResponse> searchMatches(
            Long editionId,
            MatchStatus status,
            MatchStage stage,
            Long teamId,
            Integer page,
            Integer size,
            String sortBy,
            String direction
    ) {

        Pageable pageable =
                pageableFactory.create(
                        page,
                        size,
                        sortBy,
                        direction,
                        MATCH_SORTS,
                        "matchNumber"
                );

        Specification<CricketMatch> spec =
                Specification.allOf(
                        CricketMatchSpecifications
                                .edition(editionId),

                        CricketMatchSpecifications
                                .status(status),

                        CricketMatchSpecifications
                                .stage(stage),

                        CricketMatchSpecifications
                                .involvesTeam(teamId)
                );

        var pageResult =
                matchRepository.findAll(
                        spec,
                        pageable
                );

        MatchReadiness readiness =
                readinessFor(pageResult.getContent());

        var matches =
                pageResult.map(match ->
                        toResponse(match, readiness)
                );

        return PageResponse.from(matches);
    }

    public MatchResponse getMatch(
            Long id
    ) {

        return toResponse(
                findMatch(id),
                true
        );
    }

    private CricketMatch findMatch(Long id) {

        return matchRepository
                .findDetailedById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Match not found"
                        )
                );
    }

    private List<MatchResponse> toResponses(
            List<CricketMatch> matches
    ) {

        MatchReadiness readiness =
                readinessFor(matches);

        return matches
                .stream()
                .map(match ->
                        toResponse(match, readiness)
                )
                .toList();
    }

    private MatchResponse toResponse(CricketMatch match) {
        return toResponse(
                match,
                false
        );
    }

    private MatchResponse toResponse(
            CricketMatch match,
            boolean includeHistory
    ) {
        return toResponse(
                match,
                readinessFor(List.of(match)),
                includeHistory
        );
    }

    private MatchResponse toResponse(
            CricketMatch match,
            MatchReadiness readiness
    ) {
        return toResponse(
                match,
                readiness,
                false
        );
    }

    private MatchResponse toResponse(
            CricketMatch match,
            MatchReadiness readiness,
            boolean includeHistory
    ) {

        MatchResponse.VenueInfo venue = null;

        if (match.getVenue() != null) {
            venue = new MatchResponse.VenueInfo(
                    match.getVenue().getId(),
                    match.getVenue().getName()
            );
        }

        return new MatchResponse(
                match.getId(),
                match.getMatchType(),
                match.getMatchNumber(),
                match.getRoundNumber(),
                match.getStage(),
                match.getStatus(),
                match.getResultStatus(),
                match.getRematchOfMatch() != null
                        ? match.getRematchOfMatch().getId()
                        : null,
                match.getSupersededByMatch() != null
                        ? match.getSupersededByMatch().getId()
                        : null,

                new MatchResponse.TeamInfo(
                        match.getTeamASide() != null
                                ? match.getTeamASide().getId()
                                : null,
                        match.getTeamA() != null
                                ? match.getTeamA().getId()
                                : null,
                        sideName(match.getTeamASide())
                ),

                new MatchResponse.TeamInfo(
                        match.getTeamBSide() != null
                                ? match.getTeamBSide().getId()
                                : null,
                        match.getTeamB() != null
                                ? match.getTeamB().getId()
                                : null,
                        sideName(match.getTeamBSide())
                ),

                match.getOversPerInnings(),
                venue,
                match.getScheduledAt(),
                readiness.scorerAssigned(match),
                readiness.playingXiSubmitted(
                        match,
                        match.getTeamASide().getId()
                ),
                readiness.playingXiSubmitted(
                        match,
                        match.getTeamBSide().getId()
                ),
                readiness.tossCompleted(match),
                availableOperations(match),
                includeHistory
                        ? operationHistory(match.getId())
                        : List.of()
        );
    }

    private List<MatchOperationHistoryResponse> operationHistory(
            Long matchId
    ) {
        return auditRepository
                .findDetailedByMatchId(matchId)
                .stream()
                .map(audit ->
                        new MatchOperationHistoryResponse(
                                audit.getId(),
                                audit.getOperationType(),
                                audit.getActor().getId(),
                                audit.getActor().getDisplayName(),
                                audit.getReason(),
                                audit.getOldStatus(),
                                audit.getNewStatus(),
                                audit.getOldResultStatus(),
                                audit.getNewResultStatus(),
                                audit.getMetadata(),
                                audit.getRelatedMatch() != null
                                        ? audit.getRelatedMatch().getId()
                                        : null,
                                audit.getCreatedAt()
                        )
                )
                .toList();
    }

    private List<MatchOperationType> availableOperations(
            CricketMatch match
    ) {
        MatchStatus status = match.getStatus();
        MatchResultStatus resultStatus = match.getResultStatus();
        List<MatchOperationType> operations = new ArrayList<>();

        if (status == MatchStatus.PLANNED
                || status == MatchStatus.SCHEDULED
                || status == MatchStatus.READY
                || status == MatchStatus.POSTPONED) {
            operations.add(MatchOperationType.RESCHEDULE);
        }

        if (status == MatchStatus.PLANNED
                || status == MatchStatus.SCHEDULED
                || status == MatchStatus.READY
                || status == MatchStatus.TOSS_COMPLETED) {
            operations.add(MatchOperationType.POSTPONE);
            operations.add(MatchOperationType.CANCEL);
        }

        if (status == MatchStatus.TOSS_COMPLETED) {
            operations.add(MatchOperationType.RESET_TOSS);
        }

        if (status == MatchStatus.TOSS_COMPLETED
                || status == MatchStatus.LIVE
                || status == MatchStatus.INNINGS_BREAK) {
            operations.add(MatchOperationType.SUSPEND);
            operations.add(MatchOperationType.ABANDON);
        }

        if (status == MatchStatus.SUSPENDED) {
            operations.add(MatchOperationType.RESUME);
            operations.add(MatchOperationType.ABANDON);
        }

        if (status == MatchStatus.COMPLETED
                && resultStatus == MatchResultStatus.OFFICIAL) {
            operations.add(MatchOperationType.MARK_UNDER_REVIEW);
            operations.add(MatchOperationType.ORDER_REMATCH);
        }

        if (status == MatchStatus.COMPLETED
                && resultStatus == MatchResultStatus.UNDER_REVIEW) {
            operations.add(MatchOperationType.RESTORE_OFFICIAL);
            operations.add(MatchOperationType.VOID_RESULT);
            operations.add(MatchOperationType.ORDER_REMATCH);
        }

        return operations;
    }

    private void attachTournamentSides(CricketMatch match) {

        MatchSide sideA =
                matchSideRepository.save(
                        new MatchSide(
                                match,
                                MatchSideKey.A,
                                match.getTeamA().getTeam().getName(),
                                match.getTeamA()
                        )
                );

        MatchSide sideB =
                matchSideRepository.save(
                        new MatchSide(
                                match,
                                MatchSideKey.B,
                                match.getTeamB().getTeam().getName(),
                                match.getTeamB()
                        )
                );

        match.attachSides(sideA, sideB);
    }

    private String sideName(MatchSide side) {
        return side != null
                ? side.getDisplayName()
                : "TBD";
    }

    private MatchReadiness readinessFor(
            List<CricketMatch> matches
    ) {

        if (matches.isEmpty()) {
            return MatchReadiness.empty();
        }

        Set<Long> matchIds =
                new HashSet<>();

        for (CricketMatch match : matches) {
            matchIds.add(match.getId());
        }

        Set<Long> matchesWithScorer =
                new HashSet<>(
                        scorerRepository
                                .findMatchIdsWithScorer(matchIds)
                );

        Map<TeamSubmissionKey, Long> playingXiCounts =
                new HashMap<>();

        for (Object[] row :
                playingXiRepository
                        .countSubmittedByMatchAndTeam(matchIds)) {

            playingXiCounts.put(
                    new TeamSubmissionKey(
                            (Long) row[0],
                            (Long) row[1]
                    ),
                    (Long) row[2]
            );
        }

        return new MatchReadiness(
                matchesWithScorer,
                playingXiCounts
        );
    }

    private record MatchReadiness(
            Set<Long> matchesWithScorer,
            Map<TeamSubmissionKey, Long> playingXiCounts
    ) {

        static MatchReadiness empty() {
            return new MatchReadiness(
                    Set.of(),
                    Map.of()
            );
        }

        boolean scorerAssigned(CricketMatch match) {
            return matchesWithScorer.contains(match.getId());
        }

        boolean tossCompleted(CricketMatch match) {
            MatchStatus status =
                    match.getStatus();

            return status == MatchStatus.TOSS_COMPLETED
                    || status == MatchStatus.LIVE
                    || status == MatchStatus.INNINGS_BREAK
                    || status == MatchStatus.COMPLETED;
        }

        boolean playingXiSubmitted(
                CricketMatch match,
                Long matchSideId
        ) {

            long submitted =
                    playingXiCounts.getOrDefault(
                            new TeamSubmissionKey(
                                    match.getId(),
                                    matchSideId
                            ),
                            0L
                    );

            Integer required =
                    match.isTournament()
                            ? match.requireTournamentEdition()
                                    .getPlayingXiSize()
                            : 2;

            return required != null
                    && required > 0
                    && submitted >= required;
        }
    }

    private record TeamSubmissionKey(
            Long matchId,
            Long matchSideId
    ) {}
}
