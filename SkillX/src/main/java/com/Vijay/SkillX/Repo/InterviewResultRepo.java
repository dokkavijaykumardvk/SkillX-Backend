package com.Vijay.SkillX.Repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Vijay.SkillX.Entity.InterviewResult;

public interface InterviewResultRepo
        extends JpaRepository<InterviewResult, Long> {

    Optional<InterviewResult> findByInterviewId(Long interviewId);
}