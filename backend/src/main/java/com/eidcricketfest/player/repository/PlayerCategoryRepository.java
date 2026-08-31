package com.eidcricketfest.player.repository;

import com.eidcricketfest.player.entity.PlayerCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerCategoryRepository
        extends JpaRepository<PlayerCategory, Short> {
}