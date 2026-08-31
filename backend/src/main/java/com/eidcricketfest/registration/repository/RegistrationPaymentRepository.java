package com.eidcricketfest.registration.repository;

import com.eidcricketfest.registration.entity.*;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

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

    @Query("""
        SELECT p
        FROM RegistrationPayment p
        JOIN FETCH p.registration r
        JOIN FETCH r.player player
        LEFT JOIN FETCH player.user
        WHERE r.id = :registrationId
          AND player.user.id = :userId
        ORDER BY p.createdAt DESC
    """)
    List<RegistrationPayment> findMineByRegistrationIdAndUserId(
            @Param("registrationId") Long registrationId,
            @Param("userId") Long userId
    );
}
