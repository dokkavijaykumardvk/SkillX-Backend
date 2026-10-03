package com.Vijay.SkillX.DTO;

public record QuestionResponse(
        Long questionId,
        String round,
        String technology,
        String question,
        Integer questionNumber,
        Integer totalQuestions,
        long remainingSeconds
) {
}