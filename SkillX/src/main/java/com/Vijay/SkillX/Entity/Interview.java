package com.Vijay.SkillX.Entity;

import java.time.Instant;

import com.Vijay.SkillX.Enums.InterviewStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "interviews")
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id")
    private Company company;

    private String role;

    private String experience;

    @Column(columnDefinition = "TEXT")
    private String technologies;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewStatus status;

    private Instant startedAt;

    private Instant roundStartedAt;

    private Instant roundEndAt;

    private Integer currentQuestionNumber;

    private Integer codingScore;

    private Integer aptitudeScore;

    private Integer technicalScore;

    private Integer hrScore;

    public Interview() {
    }

    public Long getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getTechnologies() {
        return technologies;
    }

    public void setTechnologies(String technologies) {
        this.technologies = technologies;
    }

    public InterviewStatus getStatus() {
        return status;
    }

    public void setStatus(InterviewStatus status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getRoundStartedAt() {
        return roundStartedAt;
    }

    public void setRoundStartedAt(Instant roundStartedAt) {
        this.roundStartedAt = roundStartedAt;
    }

    public Instant getRoundEndAt() {
        return roundEndAt;
    }

    public void setRoundEndAt(Instant roundEndAt) {
        this.roundEndAt = roundEndAt;
    }

    public Integer getCurrentQuestionNumber() {
        return currentQuestionNumber;
    }

    public void setCurrentQuestionNumber(Integer currentQuestionNumber) {
        this.currentQuestionNumber = currentQuestionNumber;
    }

    public Integer getCodingScore() {
        return codingScore;
    }

    public void setCodingScore(Integer codingScore) {
        this.codingScore = codingScore;
    }

    public Integer getAptitudeScore() {
        return aptitudeScore;
    }

    public void setAptitudeScore(Integer aptitudeScore) {
        this.aptitudeScore = aptitudeScore;
    }

    public Integer getTechnicalScore() {
        return technicalScore;
    }

    public void setTechnicalScore(Integer technicalScore) {
        this.technicalScore = technicalScore;
    }

    public Integer getHrScore() {
        return hrScore;
    }

    public void setHrScore(Integer hrScore) {
        this.hrScore = hrScore;
    }
}