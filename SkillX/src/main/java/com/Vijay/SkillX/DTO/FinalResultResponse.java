package com.Vijay.SkillX.DTO;

public record FinalResultResponse(
        Long interviewId,
        String company,
        String role,
        Double codingScore,
        Double aptitudeScore,
        Double technicalScore,
        Double hrScore,
        Double finalScore,
        String recommendation,
        String strengths,
        String weaknesses,
        String preparationPlan
) {
}