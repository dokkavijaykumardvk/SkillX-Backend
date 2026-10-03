package com.Vijay.SkillX.ServiceImpl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.Vijay.SkillX.Service.GeminiInterviewService;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class GeminiInterviewServiceImpl implements GeminiInterviewService {

    private static final int MAX_RETRIES = 2;

    private static final String[] QUESTION_KEYS = { "technology", "question", "expectedAnswer" };
    private static final String[] EVAL_KEYS = { "score", "feedback", "idealAnswer" };

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public GeminiInterviewServiceImpl(ChatClient.Builder builder) {
        this.chatClient = builder.build();

        JsonFactory lenientJsonFactory = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
                .build();
        this.objectMapper = new ObjectMapper(lenientJsonFactory);
    }

    @Override
    public JsonNode generateQuestion(
            String company, String role, String experience,
            String technologies, String round, int questionNumber) {

        String prompt = """
                You are SkillX, an expert AI interviewer.

                Create ONE realistic mock interview question.

                Company: %s
                Role: %s
                Experience: %s
                Technologies: %s
                Interview Round: %s
                Question Number: %d

                Rules:
                1. This is a simulated interview. Do not claim it is an exact confidential company question.
                2. Use the candidate's selected technologies and match difficulty to experience.
                3. Coding round: programming problems. Aptitude round: aptitude, logical or quantitative questions.
                   Technical round: selected languages, frameworks, databases. HR round: behavioral questions.
                4. Return exactly ONE question.
                5. Return ONLY raw valid JSON. No markdown code fences.
                6. Never use double quotes inside string values; use single quotes instead.
                7. Escape all newlines within string values as \\n.
                8. Keep "expectedAnswer" under 100 words.

                Required JSON:
                {
                  "technology": "technology name",
                  "question": "question text",
                  "expectedAnswer": "important points expected in the answer"
                }
                """.formatted(company, role, experience, technologies, round, questionNumber);

        return callAndParseWithRetry(prompt, MAX_RETRIES, QUESTION_KEYS);
    }

    @Override
    public JsonNode evaluateAnswer(
            String company, String role, String round,
            String technology, String question, String candidateAnswer) {

        String prompt = """
                You are an expert interviewer evaluating a candidate.

                Company: %s
                Role: %s
                Interview Round: %s
                Technology: %s

                Question:
                %s

                Candidate Answer:
                %s

                Evaluate fairly and give a score from 0 to 100.
                Consider correctness, technical understanding, completeness, clarity and practical knowledge.
                Coding: algorithm, correctness, time/space complexity, edge cases.
                Aptitude: correct result, reasoning, logical approach.
                HR: communication, confidence, relevance, professionalism.

                Rules:
                1. Return ONLY raw valid JSON. No markdown code fences.
                2. Never use double quotes inside string values; use single quotes instead.
                3. Escape all newlines within string values as \\n.
                4. Keep "feedback" under 60 words and "idealAnswer" under 120 words.

                Required JSON:
                {
                  "score": 0,
                  "feedback": "short useful feedback",
                  "idealAnswer": "ideal answer"
                }
                """.formatted(company, role, round, technology, question, candidateAnswer);

        try {
            return callAndParseWithRetry(prompt, MAX_RETRIES, EVAL_KEYS);
        } catch (RuntimeException e) {
            // Don't lose the user's answer with a 500 if Gemini misbehaves.
            ObjectNode fallback = objectMapper.createObjectNode();
            fallback.put("score", 0);
            fallback.put("feedback", "Automatic evaluation failed. Please try again.");
            fallback.put("idealAnswer", "");
            return fallback;
        }
    }

    private JsonNode callAndParseWithRetry(String prompt, int retriesLeft, String[] keys) {

        String response = chatClient.prompt().user(prompt).call().content();

        try {
            return parseJson(response, keys);
        } catch (InvalidResponseException e) {
            if (retriesLeft > 0) {
                return callAndParseWithRetry(prompt, retriesLeft - 1, keys);
            }
            throw new RuntimeException(
                    "Gemini returned invalid JSON after retrying. "
                            + "Check response-mime-type and max-output-tokens.", e);
        }
    }

    private JsonNode parseJson(String response, String[] keys) {

        if (response == null || response.isBlank()) {
            throw new InvalidResponseException("Empty response from Gemini", null);
        }

        String cleaned = response
                .replace("```json", "")
                .replace("```JSON", "")
                .replace("```", "")
                .trim();

        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');

        if (start < 0 || end < start) {
            throw new InvalidResponseException("No JSON object found: " + response, null);
        }

        cleaned = cleaned.substring(start, end + 1);

        // 1) Strict parse
        try {
            return objectMapper.readTree(cleaned);
        } catch (Exception strictError) {
            // 2) Repair: extract known fields even if quotes inside values are unescaped
            JsonNode repaired = repair(cleaned, keys);
            if (repaired != null) {
                return repaired;
            }
            throw new InvalidResponseException("Unparseable JSON: " + response, strictError);
        }
    }

    /**
     * Rebuilds the object field by field. Each value is taken as everything between
     * its key's colon and the start of the next key (or the final closing brace),
     * so unescaped quotes inside values do not break parsing.
     * Returns null if any expected key is missing or the keys are out of order.
     */
    private JsonNode repair(String json, String[] keys) {

        int[] keyStart = new int[keys.length];
        int[] valueStart = new int[keys.length];

        for (int i = 0; i < keys.length; i++) {
            Matcher m = Pattern
                    .compile("\"" + Pattern.quote(keys[i]) + "\"\\s*:")
                    .matcher(json);
            if (!m.find()) {
                return null;
            }
            keyStart[i] = m.start();
            valueStart[i] = m.end();
            if (i > 0 && keyStart[i] <= keyStart[i - 1]) {
                return null; // keys not in expected order
            }
        }

        ObjectNode node = objectMapper.createObjectNode();
        int lastBrace = json.lastIndexOf('}');

        for (int i = 0; i < keys.length; i++) {
            int end = (i + 1 < keys.length) ? keyStart[i + 1] : lastBrace;
            if (end < valueStart[i]) {
                return null;
            }

            String raw = json.substring(valueStart[i], end).trim();
            if (raw.endsWith(",")) {
                raw = raw.substring(0, raw.length() - 1).trim();
            }

            if ("score".equals(keys[i])) {
                Matcher num = Pattern.compile("-?\\d+(\\.\\d+)?").matcher(raw);
                if (!num.find()) {
                    return null;
                }
                node.put("score", (int) Math.round(Double.parseDouble(num.group())));
            } else {
                if (raw.startsWith("\"")) {
                    raw = raw.substring(1);
                }
                if (raw.endsWith("\"")) {
                    raw = raw.substring(0, raw.length() - 1);
                }
                raw = raw.replace("\\n", "\n").replace("\\\"", "\"");
                node.put(keys[i], raw.trim());
            }
        }
        return node;
    }

    /** Thrown when Gemini's response can't be turned into valid JSON. */
    private static class InvalidResponseException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        InvalidResponseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}