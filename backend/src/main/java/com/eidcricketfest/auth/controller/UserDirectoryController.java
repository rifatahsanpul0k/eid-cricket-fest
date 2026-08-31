package com.eidcricketfest.auth.controller;

import com.eidcricketfest.auth.dto.UserOptionResponse;
import com.eidcricketfest.auth.entity.*;
import com.eidcricketfest.auth.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Users")
@RestController
@RequestMapping("/api/v1/users")
@SecurityRequirement(name = "bearerAuth")
public class UserDirectoryController {

    private final UserRepository userRepository;

    public UserDirectoryController(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    @Operation(summary = "List scorer users")
    @GetMapping("/scorers")
    public List<UserOptionResponse> getScorers() {

        return userRepository
                .findEnabledByRole(RoleCode.SCORER)
                .stream()
                .map(user ->
                        new UserOptionResponse(
                                user.getId(),
                                user.getDisplayName(),
                                user.getEmail(),
                                user.getPhone()
                        )
                )
                .toList();
    }
}
