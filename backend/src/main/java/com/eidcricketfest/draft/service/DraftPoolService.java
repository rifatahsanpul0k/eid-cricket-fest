package com.eidcricketfest.draft.service;

import com.eidcricketfest.draft.dto.DraftPoolPlayerResponse;
import com.eidcricketfest.registration.entity.*;
import com.eidcricketfest.registration.repository.PlayerRegistrationRepository;
import com.eidcricketfest.team.entity.RosterEntryStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DraftPoolService {

    private final PlayerRegistrationRepository registrationRepository;

    public DraftPoolService(
            PlayerRegistrationRepository registrationRepository
    ) {
        this.registrationRepository = registrationRepository;
    }

    public List<DraftPoolPlayerResponse> getDraftPool(
            Long editionId
    ) {

        return registrationRepository
                .findDraftPool(
                        editionId,
                        RegistrationStatus.APPROVED,
                        RosterEntryStatus.ACTIVE
                )
                .stream()
                .map(registration ->
                        new DraftPoolPlayerResponse(
                                registration.getId(),
                                registration.getPlayer().getId(),
                                registration.getPlayer().getFullName(),

                                registration.getCategory().getId(),
                                registration.getCategory().getCode(),
                                registration.getCategory().getName()
                        )
                )
                .toList();
    }
}
