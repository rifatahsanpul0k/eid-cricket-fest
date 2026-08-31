package com.eidcricketfest.registration.controller;

import com.eidcricketfest.common.dto.PageResponse;
import com.eidcricketfest.registration.dto.*;
import com.eidcricketfest.registration.entity.PaymentMethod;
import com.eidcricketfest.registration.entity.PaymentStatus;
import com.eidcricketfest.registration.service.PaymentService;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Registration Payments")
@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(summary = "Submit my registration payment")
    @PostMapping("/registrations/{registrationId}/payments/me")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse submitMyPayment(
            @PathVariable Long registrationId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SubmitPaymentRequest request
    ) {
        return paymentService.submitMyPayment(
                Long.valueOf(jwt.getSubject()),
                registrationId,
                request
        );
    }

    @Operation(summary = "List my registration payments")
    @GetMapping("/registrations/{registrationId}/payments/me")
    public List<PaymentResponse> getMyPayments(
            @PathVariable Long registrationId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return paymentService.getMyPayments(
                Long.valueOf(jwt.getSubject()),
                registrationId
        );
    }

    @Operation(summary = "Record payment on behalf of a player")
    @PostMapping("/registrations/{registrationId}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse recordPayment(
            @PathVariable Long registrationId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SubmitPaymentRequest request
    ) {
        return paymentService.submitPaymentByOrganizer(
                Long.valueOf(jwt.getSubject()),
                registrationId,
                request
        );
    }

    @Operation(summary = "Search registration payments")
    @GetMapping("/tournament-editions/{editionId}/payments")
    public PageResponse<PaymentResponse> getPayments(
            @PathVariable Long editionId,
            @RequestParam(required = false)
            PaymentStatus status,
            @RequestParam(required = false)
            PaymentMethod method,
            @RequestParam(required = false)
            String q,
            @RequestParam(defaultValue = "0")
            Integer page,
            @RequestParam(defaultValue = "20")
            Integer size,
            @RequestParam(defaultValue = "createdAt")
            String sortBy,
            @RequestParam(defaultValue = "desc")
            String direction
    ) {
        return paymentService.searchPayments(
                editionId,
                status,
                method,
                q,
                page,
                size,
                sortBy,
                direction
        );
    }

    @Operation(summary = "Verify registration payment")
    @PatchMapping("/registration-payments/{paymentId}/verify")
    public PaymentResponse verify(
            @PathVariable Long paymentId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return paymentService.verifyPayment(
                Long.valueOf(jwt.getSubject()),
                paymentId
        );
    }

    @Operation(summary = "Reject registration payment")
    @PatchMapping("/registration-payments/{paymentId}/reject")
    public PaymentResponse reject(
            @PathVariable Long paymentId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RejectRequest request
    ) {
        return paymentService.rejectPayment(
                Long.valueOf(jwt.getSubject()),
                paymentId,
                request
        );
    }
}
