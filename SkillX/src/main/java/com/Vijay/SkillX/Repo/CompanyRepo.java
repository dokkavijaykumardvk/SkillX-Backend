package com.Vijay.SkillX.Repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Vijay.SkillX.Entity.Company;

public interface CompanyRepo extends JpaRepository<Company, Long> {
    Optional<Company> findByName(String name);
}