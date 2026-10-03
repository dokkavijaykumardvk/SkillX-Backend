package com.Vijay.SkillX.Service;

import com.fasterxml.jackson.databind.JsonNode;

public interface GeminiInterviewService {

    JsonNode generateQuestion(
            String company,
            String role,
            String experience,
            String technologies,
            String round,
            int questionNumber);

    JsonNode evaluateAnswer(
            String company,
            String role,
            String round,
            String technology,
            String question,
            String candidateAnswer);
}