package com.Vijay.SkillX.DTO;

import java.time.Instant;

public record StartInterviewResponse(
        Long interviewId,
        String company,
        String role,
        String round,
        Instant roundEndTime,
        long remainingSeconds
) {
}