package com.devpro.user_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "profile")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Profile {


    @Id
    @Column
    private UUID userId;



    // BASIC STATS
    @Column(name = "total_submissions")
    private Long totalSubmissions = 0L;

    @Column(name = "total_solved")
    private Long totalSolved = 0L;

    @Column(name = "casual_solved")
    private Long casualSolved = 0L;

    @Column(name = "pro_solved")
    private Long proSolved = 0L;

    @Column(name = "engineering_solved")
    private Long engineeringSolved = 0L;

    @Column(name = "promax_solved")
    private Long pro_maxSolved = 0L;



    // TAG-WISE SOLVED COUNT
    // e.g. {"auth": 3, "mongodb": 18}
    @ElementCollection
    @CollectionTable(
            name = "profile_tag_stats",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @MapKeyColumn(name = "tag")
    @Column(name = "count")
    private Map<String, Long> tagStats;





    // FRAMEWORK USAGE
    // e.g. {"express": 20, "fastapi": 5}
    @ElementCollection
    @CollectionTable(
            name = "profile_framework_stats",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @MapKeyColumn(name = "framework")
    @Column(name = "count")
    private Map<String, Long> frameworkStats;

    @Column(name = "most_used_framework")
    private String mostUsedFramework;




    // YEARLY HEATMAP
    // e.g. {"2026-02-09": 3}
    @ElementCollection
    @CollectionTable(
            name = "profile_activity_heatmap",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @MapKeyColumn(name = "activity_date")
    @Column(name = "submission_count")
    private Map<String, Integer> yearlyActivity;



    // BADGES
    @ElementCollection
    @CollectionTable(
            name = "profile_badges",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "badge")
    private Set<String> badges;




    // PROFILE META
    @Column(name = "profile_views")
    private Long profileViews = 0L;

    @Column(name = "current_streak")
    private Integer currentStreak = 0;

    @Column(name = "max_streak")
    private Integer maxStreak = 0;

    @Column(name = "last_submission_at")
    private Instant lastSubmissionAt;

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

}
