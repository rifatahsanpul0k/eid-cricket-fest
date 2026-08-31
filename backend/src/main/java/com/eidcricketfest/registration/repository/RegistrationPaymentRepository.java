package com.eidcricketfest.registration.repository;

import com.eidcricketfest.registration.entity.*;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface RegistrationPaymentRepository
        extends JpaRepository<RegistrationPayment, Long>,
        JpaSpecificationExecutor<RegistrationPayment> {

    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM RegistrationPayment p
        WHERE p.registration.id = :registrationId
          AND p.status = :status
    """)
    BigDecimal sumAmountByRegistrationAndStatus(
            @Param("registrationId") Long registrationId,
            @Param("status") PaymentStatus status
    );
}
