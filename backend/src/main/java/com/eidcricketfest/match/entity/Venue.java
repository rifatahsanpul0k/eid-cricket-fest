package com.eidcricketfest.match.entity;

import com.eidcricketfest.common.entity.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "venues")
public class Venue extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String address;

    protected Venue() {}

    public Venue(String name, String address) {
        this.name = name;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }
}
