package com.eidcricketfest.player.repository;

import com.eidcricketfest.player.entity.PlayerCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerCategoryRepository
        extends JpaRepository<PlayerCategory, Short> {

    List<PlayerCategory> findByActiveTrueOrderByIdAsc();
}
