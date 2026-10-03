package com.Vijay.SkillX.Repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Vijay.SkillX.Entity.Answer;

public interface AnswerRepo extends JpaRepository<Answer, Long> {

    List<Answer> findByQuestionInterviewId(Long interviewId);
}