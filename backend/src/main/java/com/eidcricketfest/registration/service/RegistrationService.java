package com.eidcricketfest.registration.service;

import com.eidcricketfest.auth.entity.User;
import com.eidcricketfest.auth.repository.UserRepository;
import com.eidcricketfest.common.dto.PageResponse;
import com.eidcricketfest.common.exception.*;
import com.eidcricketfest.common.web.PageableFactory;
import com.eidcricketfest.player.entity.*;
import com.eidcricketfest.player.repository.*;
import com.eidcricketfest.registration.dto.*;
import com.eidcricketfest.registration.entity.*;
import com.eidcricketfest.registration.repository.*;
import com.eidcricketfest.tournament.entity.*;
import com.eidcricketfest.tournament.repository.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Service
@Transactional
public class RegistrationService {
    private static final Map<String, String> REGISTRATION_SORTS =
            Map.of(
                    "registeredAt",
                    "registeredAt",

                    "status",
                    "status",

                    "createdAt",
                    "createdAt"
            );

    private final UserRepository userRepository;
    private final RegistrationPaymentRepository paymentRepository;
    private final PlayerRegistrationRepository registrationRepository;
    private final PlayerRepository playerRepository;
    private final PlayerCategoryRepository categoryRepository;
    private final TournamentEditionRepository editionRepository;
    private final PageableFactory pageableFactory;

    public RegistrationService(
            PlayerRegistrationRepository registrationRepository,
            PlayerRepository playerRepository,
            PlayerCategoryRepository categoryRepository,
            TournamentEditionRepository editionRepository,
            UserRepository userRepository,
            RegistrationPaymentRepository paymentRepository,
            PageableFactory pageableFactory
    ) {
        this.registrationRepository = registrationRepository;
        this.playerRepository = playerRepository;
        this.categoryRepository = categoryRepository;
        this.editionRepository = editionRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.pageableFactory = pageableFactory;
    }

    public RegistrationResponse registerMyself(
            Long userId,
            Long editionId,
            CreateRegistrationRequest request
    ) {

        Player player = playerRepository
                .findByUser_Id(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Create a player profile before registering"
                        )
                );

        return register(
                player,
                editionId,
                request
        );
    }

    @Transactional(readOnly = true)
    public RegistrationResponse getMyRegistration(
            Long userId,
            Long editionId
    ) {

        return toResponse(
                registrationRepository
                        .findMineByEditionIdAndUserId(
                                editionId,
                                userId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Registration not found"
                                )
                        )
        );
    }

    private RegistrationResponse register(
            Player player,
            Long editionId,
            CreateRegistrationRequest request
    ) {

        TournamentEdition edition =
                editionRepository.findById(editionId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Tournament edition not found"
                                )
                        );

        validateRegistrationWindow(edition);

        if (registrationRepository
                .existsByTournamentEdition_IdAndPlayer_Id(
                        editionId,
                        player.getId()
                )) {

            throw new ConflictException(
                    "Player is already registered for this tournament"
            );
        }

        PlayerCategory category =
                categoryRepository
                        .findById(request.categoryId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Player category not found"
                                )
                        );

        if (!category.isActive()) {
            throw new IllegalArgumentException(
                    "Player category is inactive"
            );
        }

        PlayerRegistration registration =
                new PlayerRegistration(
                        edition,
                        player,
                        category,
                        edition.getRegistrationFee()
                );

        return toResponse(
                registrationRepository.save(registration)
        );
    }

    private void validateRegistrationWindow(
            TournamentEdition edition
    ) {

        if (edition.getStatus()
                != TournamentEditionStatus.REGISTRATION_OPEN) {

            throw new ConflictException(
                    "Tournament registration is not open"
            );
        }

        Instant now = Instant.now();

        if (edition.getRegistrationStartAt() != null
                && now.isBefore(
                edition.getRegistrationStartAt()
        )) {

            throw new ConflictException(
                    "Registration has not started yet"
            );
        }

        if (edition.getRegistrationEndAt() != null
                && now.isAfter(
                edition.getRegistrationEndAt()
        )) {

            throw new ConflictException(
                    "Registration has already closed"
            );
        }
    }

    private RegistrationResponse toResponse(
            PlayerRegistration registration
    ) {

        return new RegistrationResponse(
                registration.getId(),
                registration.getTournamentEdition().getId(),
                registration.getPlayer().getId(),
                registration.getCategory().getId(),
                registration.getCategory().getName(),
                registration.getFeeAmount(),
                registration.getTournamentEdition()
                        .getRegistrationCurrency(),
                registration.getStatus(),
                registration.getRegisteredAt()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<RegistrationResponse> searchRegistrations(
            Long editionId,
            RegistrationStatus status,
            String category,
            String search,
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
                        REGISTRATION_SORTS,
                        "registeredAt"
                );

        Specification<PlayerRegistration> spec =
                Specification.allOf(
                        PlayerRegistrationSpecifications
                                .edition(editionId),

                        PlayerRegistrationSpecifications
                                .status(status),

                        PlayerRegistrationSpecifications
                                .categoryCode(category),

                        PlayerRegistrationSpecifications
                                .playerNameContains(search)
                );

        var result =
                registrationRepository
                        .findAll(
                                spec,
                                pageable
                        )
                        .map(this::toResponse);

        return PageResponse.from(result);
    }

    public RegistrationReviewResponse approve(
            Long reviewerUserId,
            Long registrationId
    ) {

        PlayerRegistration registration =
                registrationRepository.findDetailedById(registrationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Registration not found"
                                )
                        );

        if (registration.getStatus()
                != RegistrationStatus.PENDING) {

            throw new ConflictException(
                    "Only pending registrations can be approved"
            );
        }

        BigDecimal verified =
                paymentRepository
                        .sumAmountByRegistrationAndStatus(
                                registrationId,
                                PaymentStatus.VERIFIED
                        );

        BigDecimal required =
                registration.getFeeAmount();

        if (verified.compareTo(required) < 0) {

            throw new ConflictException(
                    "Registration fee has not been fully verified"
            );
        }

        User reviewer =
                userRepository.findById(reviewerUserId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Reviewer not found"
                                )
                        );

        registration.approve(reviewer);

        return reviewResponse(
                registration,
                verified
        );
    }
    public RegistrationReviewResponse reject(
            Long reviewerUserId,
            Long registrationId,
            RejectRequest request
    ) {

        PlayerRegistration registration =
                registrationRepository.findDetailedById(registrationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Registration not found"
                                )
                        );

        User reviewer =
                userRepository.findById(reviewerUserId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Reviewer not found"
                                )
                        );

        try {
            registration.reject(
                    reviewer,
                    request.reason().trim()
            );
        } catch (IllegalStateException ex) {
            throw new ConflictException(ex.getMessage());
        }

        BigDecimal verified =
                paymentRepository
                        .sumAmountByRegistrationAndStatus(
                                registrationId,
                                PaymentStatus.VERIFIED
                        );

        return reviewResponse(
                registration,
                verified
        );
    }
    private RegistrationReviewResponse reviewResponse(
            PlayerRegistration registration,
            BigDecimal verified
    ) {

        BigDecimal remaining =
                registration.getFeeAmount()
                        .subtract(verified)
                        .max(BigDecimal.ZERO);

        return new RegistrationReviewResponse(
                registration.getId(),
                registration.getStatus(),
                registration.getFeeAmount(),
                verified,
                remaining,
                registration.getRejectionReason()
        );
    }
}
