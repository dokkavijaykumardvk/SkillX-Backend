package com.Vijay.SkillX.Repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Vijay.SkillX.Entity.Candidate;

public interface CandidateRepo extends JpaRepository<Candidate, Long> {
}