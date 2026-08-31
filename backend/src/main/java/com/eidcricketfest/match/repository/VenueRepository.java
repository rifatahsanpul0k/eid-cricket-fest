package com.eidcricketfest.match.repository;

import com.eidcricketfest.match.entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueRepository
        extends JpaRepository<Venue, Long> {

    boolean existsByNameIgnoreCase(String name);
}
