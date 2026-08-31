package com.eidcricketfest.auth.repository;

import com.eidcricketfest.auth.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    @EntityGraph(attributePaths = "roles")
    @Query("""
        SELECT u
        FROM User u
        WHERE LOWER(u.email) = LOWER(:identifier)
           OR u.phone = :identifier
    """)
    Optional<User> findByIdentifier(
            @Param("identifier") String identifier
    );
}