package com.eidcricketfest.registration.service;

import com.eidcricketfest.auth.entity.User;
import com.eidcricketfest.auth.repository.UserRepository;
import com.eidcricketfest.common.dto.PageResponse;
import com.eidcricketfest.common.exception.*;
import com.eidcricketfest.common.web.PageableFactory;
import com.eidcricketfest.registration.dto.*;
import com.eidcricketfest.registration.entity.*;
import com.eidcricketfest.registration.repository.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class PaymentService {

    private static final Map<String, String> PAYMENT_SORTS =
            Map.of(
                    "paidAt",
                    "paidAt",

                    "createdAt",
                    "createdAt",

                    "status",
                    "status"
            );

    private final RegistrationPaymentRepository paymentRepository;
    private final PlayerRegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final PageableFactory pageableFactory;

    public PaymentService(
            RegistrationPaymentRepository paymentRepository,
            PlayerRegistrationRepository registrationRepository,
            UserRepository userRepository,
            PageableFactory pageableFactory
    ) {
        this.paymentRepository = paymentRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.pageableFactory = pageableFactory;
    }

    public PaymentResponse submitMyPayment(
            Long userId,
            Long registrationId,
            SubmitPaymentRequest request
    ) {

        PlayerRegistration registration =
                findRegistration(registrationId);

        if (registration.getPlayer().getUser() == null
                || !registration.getPlayer()
                .getUser()
                .getId()
                .equals(userId)) {

            throw new ForbiddenException(
                    "You cannot submit payment for this registration"
            );
        }

        User submitter = findUser(userId);

        return createPayment(
                registration,
                submitter,
                request
        );
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getMyPayments(
            Long userId,
            Long registrationId
    ) {

        PlayerRegistration registration =
                findRegistration(registrationId);

        if (registration.getPlayer().getUser() == null
                || !registration.getPlayer()
                .getUser()
                .getId()
                .equals(userId)) {

            throw new ForbiddenException(
                    "You cannot view payments for this registration"
            );
        }

        return paymentRepository
                .findMineByRegistrationIdAndUserId(
                        registrationId,
                        userId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public PaymentResponse submitPaymentByOrganizer(
            Long organizerUserId,
            Long registrationId,
            SubmitPaymentRequest request
    ) {

        PlayerRegistration registration =
                findRegistration(registrationId);

        return createPayment(
                registration,
                findUser(organizerUserId),
                request
        );
    }

    public PaymentResponse verifyPayment(
            Long reviewerUserId,
            Long paymentId
    ) {

        RegistrationPayment payment =
                paymentRepository.findById(paymentId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Payment not found"
                                )
                        );

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new ConflictException(
                    "Only pending payments can be verified"
            );
        }

        Long registrationId =
                payment.getRegistration().getId();

        BigDecimal currentlyVerified =
                verifiedAmount(registrationId);

        BigDecimal fee =
                payment.getRegistration().getFeeAmount();

        if (currentlyVerified
                .add(payment.getAmount())
                .compareTo(fee) > 0) {

            throw new ConflictException(
                    "Verifying this payment would exceed the registration fee"
            );
        }

        payment.verify(findUser(reviewerUserId));

        return toResponse(payment);
    }

    public PaymentResponse rejectPayment(
            Long reviewerUserId,
            Long paymentId,
            RejectRequest request
    ) {

        RegistrationPayment payment =
                paymentRepository.findById(paymentId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Payment not found"
                                )
                        );

        try {
            payment.reject(
                    findUser(reviewerUserId),
                    request.reason().trim()
            );
        } catch (IllegalStateException ex) {
            throw new ConflictException(ex.getMessage());
        }

        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> searchPayments(
            Long editionId,
            PaymentStatus status,
            PaymentMethod method,
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
                        PAYMENT_SORTS,
                        "createdAt"
                );

        Specification<RegistrationPayment> spec =
                Specification.allOf(
                        RegistrationPaymentSpecifications
                                .edition(editionId),

                        RegistrationPaymentSpecifications
                                .status(status),

                        RegistrationPaymentSpecifications
                                .method(method),

                        RegistrationPaymentSpecifications
                                .playerNameContains(search)
                );

        var result =
                paymentRepository
                        .findAll(
                                spec,
                                pageable
                        )
                        .map(this::toResponse);

        return PageResponse.from(result);
    }

    private PaymentResponse createPayment(
            PlayerRegistration registration,
            User submitter,
            SubmitPaymentRequest request
    ) {

        if (registration.getStatus()
                != RegistrationStatus.PENDING) {

            throw new ConflictException(
                    "Payments can only be submitted for pending registrations"
            );
        }

        BigDecimal verified =
                verifiedAmount(registration.getId());

        BigDecimal remaining =
                registration.getFeeAmount()
                        .subtract(verified);

        if (request.amount().compareTo(remaining) > 0) {
            throw new ConflictException(
                    "Payment amount exceeds remaining registration fee"
            );
        }

        String reference =
                request.transactionReference() == null
                        ? null
                        : request.transactionReference().trim();

        RegistrationPayment payment =
                new RegistrationPayment(
                        registration,
                        request.amount(),
                        request.paymentMethod(),
                        reference,
                        request.paidAt() != null
                                ? request.paidAt()
                                : Instant.now(),
                        submitter
                );

        return toResponse(
                paymentRepository.save(payment)
        );
    }

    private BigDecimal verifiedAmount(Long registrationId) {

        return paymentRepository
                .sumAmountByRegistrationAndStatus(
                        registrationId,
                        PaymentStatus.VERIFIED
                );
    }

    private PlayerRegistration findRegistration(Long id) {

        return registrationRepository
                .findDetailedById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Registration not found"
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

    private PaymentResponse toResponse(
            RegistrationPayment payment
    ) {

        return new PaymentResponse(
                payment.getId(),
                payment.getRegistration().getId(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getTransactionReference(),
                payment.getStatus(),
                payment.getPaidAt(),
                payment.getVerifiedAt(),
                payment.getRejectionReason(),
                payment.getCreatedAt()
        );
    }
}
