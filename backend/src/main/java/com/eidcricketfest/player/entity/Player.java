package com.eidcricketfest.player.entity;

import com.eidcricketfest.auth.entity.User;
import com.eidcricketfest.common.entity.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "players")
public class Player extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "photo_url", columnDefinition = "TEXT")
    private String photoUrl;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_category_id")
    private PlayerCategory primaryCategory;

    @Column(name = "batting_style", length = 50)
    private String battingStyle;

    @Column(name = "bowling_style", length = 80)
    private String bowlingStyle;

    protected Player() {
    }

    public Player(
            User user,
            String fullName,
            String photoUrl,
            LocalDate dateOfBirth,
            PlayerCategory primaryCategory,
            String battingStyle,
            String bowlingStyle
    ) {
        this.user = user;
        this.fullName = fullName;
        this.photoUrl = photoUrl;
        this.dateOfBirth = dateOfBirth;
        this.primaryCategory = primaryCategory;
        this.battingStyle = battingStyle;
        this.bowlingStyle = bowlingStyle;
    }

    public User getUser() {
        return user;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public PlayerCategory getPrimaryCategory() {
        return primaryCategory;
    }

    public String getBattingStyle() {
        return battingStyle;
    }

    public String getBowlingStyle() {
        return bowlingStyle;
    }
}