package com.eidcricketfest.auth.repository;

import com.eidcricketfest.auth.entity.Role;
import com.eidcricketfest.auth.entity.RoleCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Short> {

    Optional<Role> findByCode(RoleCode code);
}