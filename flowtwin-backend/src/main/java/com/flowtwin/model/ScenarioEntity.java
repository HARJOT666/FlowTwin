package com.flowtwin.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "scenario")
public class ScenarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private double baselineP90;
    private double scenarioP90;

    @Column(columnDefinition = "text")
    private String narrationSummary;

    @Column(columnDefinition = "text")
    private String recommendations;

    private Instant createdAt = Instant.now();

    protected ScenarioEntity() { }

    public ScenarioEntity(String name, double baselineP90, double scenarioP90,
                          String narrationSummary, String recommendations) {
        this.name = name;
        this.baselineP90 = baselineP90;
        this.scenarioP90 = scenarioP90;
        this.narrationSummary = narrationSummary;
        this.recommendations = recommendations;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public double getBaselineP90() { return baselineP90; }
    public double getScenarioP90() { return scenarioP90; }
    public String getNarrationSummary() { return narrationSummary; }
    public String getRecommendations() { return recommendations; }
    public Instant getCreatedAt() { return createdAt; }
}
