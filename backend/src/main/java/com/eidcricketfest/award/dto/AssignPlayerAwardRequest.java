package com.eidcricketfest.award.dto;

import com.eidcricketfest.award.entity.AwardType;
import jakarta.validation.constraints.*;

public record AssignPlayerAwardRequest(

        @NotNull
        Long registrationId,

        @NotNull
        AwardType awardType,

        @Size(max = 150)
        String title,

        @Size(max = 1000)
        String notes

) {

    @AssertTrue(
            message = "A title is required for a custom award"
    )
    public boolean isCustomTitleValid() {

        return awardType != AwardType.CUSTOM
                || (
                    title != null
                    && !title.isBlank()
                );
    }
}
