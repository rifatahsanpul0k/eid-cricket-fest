package com.eidcricketfest.player.service;

import com.eidcricketfest.auth.entity.User;
import com.eidcricketfest.auth.repository.UserRepository;
import com.eidcricketfest.common.dto.PageResponse;
import com.eidcricketfest.common.exception.ConflictException;
import com.eidcricketfest.common.exception.ResourceNotFoundException;
import com.eidcricketfest.common.web.PageableFactory;
import com.eidcricketfest.player.dto.*;
import com.eidcricketfest.player.entity.*;
import com.eidcricketfest.player.repository.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Transactional
public class PlayerService {

    private static final Map<String, String> PLAYER_SORTS =
            Map.of(
                    "name",
                    "fullName",

                    "createdAt",
                    "createdAt"
            );

    private final PlayerRepository playerRepository;
    private final PlayerCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final PageableFactory pageableFactory;

    public PlayerService(
            PlayerRepository playerRepository,
            PlayerCategoryRepository categoryRepository,
            UserRepository userRepository,
            PageableFactory pageableFactory
    ) {
        this.playerRepository = playerRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.pageableFactory = pageableFactory;
    }

    public PlayerResponse createMyProfile(
            Long userId,
            CreatePlayerRequest request
    ) {

        if (playerRepository.existsByUser_Id(userId)) {
            throw new ConflictException(
                    "Player profile already exists"
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );

        PlayerCategory category =
                findCategory(request.primaryCategoryId());

        Player player = new Player(
                user,
                request.fullName().trim(),
                request.photoUrl(),
                request.dateOfBirth(),
                category,
                request.battingStyle(),
                request.bowlingStyle()
        );

        return toResponse(
                playerRepository.save(player)
        );
    }

    public PlayerResponse createManualPlayer(
            CreatePlayerRequest request
    ) {

        PlayerCategory category =
                findCategory(request.primaryCategoryId());

        Player player = new Player(
                null,
                request.fullName().trim(),
                request.photoUrl(),
                request.dateOfBirth(),
                category,
                request.battingStyle(),
                request.bowlingStyle()
        );

        return toResponse(
                playerRepository.save(player)
        );
    }

    @Transactional(readOnly = true)
    public PlayerResponse getPlayer(Long id) {

        return toResponse(
                playerRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Player not found with id: " + id
                                )
                        )
        );
    }

    @Transactional(readOnly = true)
    public PlayerResponse getMyProfile(Long userId) {

        Player player = playerRepository
                .findByUser_Id(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Player profile does not exist"
                        )
                );

        return toResponse(player);
    }

    @Transactional(readOnly = true)
    public PageResponse<PlayerResponse> search(
            String search,
            String category,
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
                        PLAYER_SORTS,
                        "name"
                );

        Specification<Player> spec =
                Specification.allOf(
                        PlayerSpecifications
                                .nameContains(search),

                        PlayerSpecifications
                                .categoryCode(category)
                );

        var result =
                playerRepository
                        .findAll(
                                spec,
                                pageable
                        )
                        .map(this::toResponse);

        return PageResponse.from(result);
    }

    private PlayerCategory findCategory(Short id) {

        if (id == null) {
            return null;
        }

        PlayerCategory category =
                categoryRepository.findById(id)
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

        return category;
    }

    private PlayerResponse toResponse(Player player) {

        PlayerCategory category =
                player.getPrimaryCategory();

        return new PlayerResponse(
                player.getId(),

                player.getUser() != null
                        ? player.getUser().getId()
                        : null,

                player.getFullName(),
                player.getPhotoUrl(),
                player.getDateOfBirth(),

                category != null
                        ? new PlayerResponse.CategoryInfo(
                        category.getId(),
                        category.getCode(),
                        category.getName()
                )
                        : null,

                player.getBattingStyle(),
                player.getBowlingStyle(),
                player.getCreatedAt()
        );
    }
}
