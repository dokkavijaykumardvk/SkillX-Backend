package com.Vijay.SkillX.DTO;

public record AnswerResponse(
        Long questionId,
        Integer score,
        String feedback,
        String idealAnswer,
        boolean roundCompleted,
        String nextRound
) {
}