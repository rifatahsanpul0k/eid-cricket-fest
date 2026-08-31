package com.eidcricketfest.team.entity;

import com.eidcricketfest.common.entity.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "teams")
public class Team extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "short_name", length = 20)
    private String shortName;

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    protected Team() {}

    public Team(String name, String shortName, String logoUrl) {
        this.name = name;
        this.shortName = shortName;
        this.logoUrl = logoUrl;
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    public String getLogoUrl() {
        return logoUrl;
    }
}
