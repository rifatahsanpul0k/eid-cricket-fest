package com.eidcricketfest.tournament.repository;

import com.eidcricketfest.tournament.entity.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    boolean existsByNameIgnoreCase(String name);
}