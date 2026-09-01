package com.eidcricketfest.tournament.service;

import com.eidcricketfest.common.exception.ConflictException;
import com.eidcricketfest.common.exception.ResourceNotFoundException;
import com.eidcricketfest.tournament.dto.CreateTournamentEditionRequest;
import com.eidcricketfest.tournament.dto.CreateTournamentRequest;
import com.eidcricketfest.tournament.dto.TournamentEditionResponse;
import com.eidcricketfest.tournament.dto.TournamentResponse;
import com.eidcricketfest.tournament.dto.UpdateTournamentEditionRequest;
import com.eidcricketfest.tournament.dto.UpdateTournamentEditionStatusRequest;
import com.eidcricketfest.tournament.entity.Tournament;
import com.eidcricketfest.tournament.entity.TournamentEdition;
import com.eidcricketfest.tournament.repository.TournamentEditionRepository;
import com.eidcricketfest.tournament.repository.TournamentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final TournamentEditionRepository editionRepository;

    public TournamentService(
            TournamentRepository tournamentRepository,
            TournamentEditionRepository editionRepository
    ) {
        this.tournamentRepository = tournamentRepository;
        this.editionRepository = editionRepository;
    }

    public TournamentResponse createTournament(
            CreateTournamentRequest request
    ) {

        String name = request.name().trim();

        if (tournamentRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException(
                    "Tournament already exists: " + name
            );
        }

        Tournament tournament = new Tournament(
                name,
                request.description(),
                request.logoUrl()
        );

        Tournament savedTournament =
                tournamentRepository.save(tournament);

        return toResponse(savedTournament);
    }

    @Transactional(readOnly = true)
    public TournamentResponse getTournament(Long id) {

        Tournament tournament = findTournament(id);

        return toResponse(tournament);
    }

    @Transactional(readOnly = true)
    public List<TournamentResponse> getTournaments() {

        return tournamentRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TournamentEditionResponse createEdition(
            Long tournamentId,
            CreateTournamentEditionRequest request
    ) {

        Tournament tournament = findTournament(tournamentId);

        String name = request.name().trim();

        if (editionRepository
                .existsByTournament_IdAndNameIgnoreCase(
                        tournamentId,
                        name
                )) {

            throw new ConflictException(
                    "Tournament edition already exists: " + name
            );
        }

        BigDecimal registrationFee =
                request.registrationFee() != null
                        ? request.registrationFee()
                        : BigDecimal.ZERO;

        String registrationCurrency =
                request.registrationCurrency() != null
                        ? request.registrationCurrency()
                        .trim()
                        .toUpperCase(Locale.ROOT)
                        : "BDT";

        TournamentEdition edition =
                new TournamentEdition(
                        tournament,
                        name,
                        request.startDate(),
                        request.endDate(),
                        request.registrationStartAt(),
                        request.registrationEndAt(),
                        request.oversPerInnings(),
                        request.squadSize(),
                        request.playingXiSize(),
                        registrationFee,
                        registrationCurrency,
                        request.winPoints(),
                        request.tiePoints(),
                        request.noResultPoints(),
                        request.lossPoints()
                );

        TournamentEdition savedEdition =
                editionRepository.save(edition);

        return toResponse(savedEdition);
    }

    public TournamentEditionResponse updateEdition(
            Long tournamentId,
            Long editionId,
            UpdateTournamentEditionRequest request
    ) {

        findTournament(tournamentId);

        TournamentEdition edition =
                findEditionForTournament(
                        tournamentId,
                        editionId
                );

        String name = request.name().trim();

        if (editionRepository
                .existsByTournament_IdAndNameIgnoreCaseAndIdNot(
                        tournamentId,
                        name,
                        editionId
                )) {

            throw new ConflictException(
                    "Tournament edition already exists: " + name
            );
        }

        try {
            edition.updateConfiguration(
                    name,
                    request.startDate(),
                    request.endDate(),
                    request.registrationStartAt(),
                    request.registrationEndAt(),
                    request.oversPerInnings(),
                    request.squadSize(),
                    request.playingXiSize(),
                    registrationFeeOrDefault(request.registrationFee()),
                    registrationCurrencyOrDefault(
                            request.registrationCurrency()
                    ),
                    request.winPoints(),
                    request.tiePoints(),
                    request.noResultPoints(),
                    request.lossPoints()
            );
        } catch (IllegalStateException ex) {
            throw new ConflictException(ex.getMessage());
        }

        return toResponse(edition);
    }

    public TournamentEditionResponse transitionEditionStatus(
            Long tournamentId,
            Long editionId,
            UpdateTournamentEditionStatusRequest request
    ) {

        findTournament(tournamentId);

        TournamentEdition edition =
                findEditionForTournament(
                        tournamentId,
                        editionId
                );

        try {
            edition.transitionTo(request.status());
        } catch (IllegalStateException ex) {
            throw new ConflictException(ex.getMessage());
        }

        return toResponse(edition);
    }

    @Transactional(readOnly = true)
    public List<TournamentEditionResponse> getEditions(
            Long tournamentId
    ) {

        // Ensures an invalid tournament ID gives 404
        // instead of silently returning an empty list.
        findTournament(tournamentId);

        return editionRepository
                .findByTournament_IdOrderByCreatedAtDesc(
                        tournamentId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Tournament findTournament(Long id) {

        return tournamentRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Tournament not found with id: " + id
                        )
                );
    }

    private TournamentEdition findEditionForTournament(
            Long tournamentId,
            Long editionId
    ) {

        TournamentEdition edition =
                editionRepository.findByIdForUpdate(editionId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Tournament edition not found with id: "
                                                + editionId
                                )
                        );

        if (!edition.getTournament()
                .getId()
                .equals(tournamentId)) {

            throw new ResourceNotFoundException(
                    "Tournament edition not found with id: " + editionId
            );
        }

        return edition;
    }

    private BigDecimal registrationFeeOrDefault(
            BigDecimal registrationFee
    ) {

        return registrationFee != null
                ? registrationFee
                : BigDecimal.ZERO;
    }

    private String registrationCurrencyOrDefault(
            String registrationCurrency
    ) {

        return registrationCurrency != null
                ? registrationCurrency
                .trim()
                .toUpperCase(Locale.ROOT)
                : "BDT";
    }

    private TournamentResponse toResponse(
            Tournament tournament
    ) {

        return new TournamentResponse(
                tournament.getId(),
                tournament.getName(),
                tournament.getDescription(),
                tournament.getLogoUrl(),
                tournament.getCreatedAt(),
                tournament.getUpdatedAt()
        );
    }

    private TournamentEditionResponse toResponse(
            TournamentEdition edition
    ) {

        return new TournamentEditionResponse(
                edition.getId(),
                edition.getTournament().getId(),
                edition.getName(),
                edition.getStartDate(),
                edition.getEndDate(),
                edition.getRegistrationStartAt(),
                edition.getRegistrationEndAt(),
                edition.getOversPerInnings(),
                edition.getSquadSize(),
                edition.getPlayingXiSize(),

                edition.getRegistrationFee(),
                edition.getRegistrationCurrency(),
                edition.getWinPoints(),
                edition.getTiePoints(),
                edition.getNoResultPoints(),
                edition.getLossPoints(),

                edition.getStatus(),
                edition.getCreatedAt(),
                edition.getUpdatedAt()
        );
    }
}
