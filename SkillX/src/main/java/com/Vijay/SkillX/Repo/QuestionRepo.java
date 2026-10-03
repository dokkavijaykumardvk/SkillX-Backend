package com.Vijay.SkillX.Repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Vijay.SkillX.Entity.Question;
import com.Vijay.SkillX.Enums.RoundType;

public interface QuestionRepo extends JpaRepository<Question, Long> {

    List<Question> findByInterviewIdAndRoundTypeOrderByQuestionOrder(
            Long interviewId,
            RoundType roundType
    );

    Optional<Question> findFirstByInterviewIdAndRoundTypeAndQuestionOrder(
            Long interviewId,
            RoundType roundType,
            Integer questionOrder
    );
}