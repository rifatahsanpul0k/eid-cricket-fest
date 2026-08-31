package com.eidcricketfest.registration.entity;

import com.eidcricketfest.auth.entity.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "registration_payments")
public class RegistrationPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registration_id", nullable = false)
    private PlayerRegistration registration;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "transaction_reference", length = 150)
    private String transactionReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_user_id")
    private User verifiedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by_user_id")
    private User submittedBy;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected RegistrationPayment() {
    }

    public RegistrationPayment(
            PlayerRegistration registration,
            BigDecimal amount,
            PaymentMethod paymentMethod,
            String transactionReference,
            Instant paidAt,
            User submittedBy
    ) {
        this.registration = registration;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.transactionReference = transactionReference;
        this.paidAt = paidAt;
        this.submittedBy = submittedBy;
        this.status = PaymentStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void verify(User verifier) {

        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending payments can be verified"
            );
        }

        status = PaymentStatus.VERIFIED;
        verifiedBy = verifier;
        verifiedAt = Instant.now();
        rejectionReason = null;
    }

    public void reject(User reviewer, String reason) {

        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending payments can be rejected"
            );
        }

        status = PaymentStatus.REJECTED;
        verifiedBy = reviewer;
        verifiedAt = Instant.now();
        rejectionReason = reason;
    }

    public Long getId() {
        return id;
    }

    public PlayerRegistration getRegistration() {
        return registration;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}