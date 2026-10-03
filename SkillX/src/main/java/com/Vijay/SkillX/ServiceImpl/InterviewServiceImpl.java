package com.Vijay.SkillX.ServiceImpl;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Vijay.SkillX.DTO.AnswerRequest;
import com.Vijay.SkillX.DTO.AnswerResponse;
import com.Vijay.SkillX.DTO.FinalResultResponse;
import com.Vijay.SkillX.DTO.QuestionResponse;
import com.Vijay.SkillX.DTO.StartInterviewRequest;
import com.Vijay.SkillX.DTO.StartInterviewResponse;
import com.Vijay.SkillX.Entity.Answer;
import com.Vijay.SkillX.Entity.Company;
import com.Vijay.SkillX.Entity.Interview;
import com.Vijay.SkillX.Entity.InterviewResult;
import com.Vijay.SkillX.Entity.Question;
import com.Vijay.SkillX.Enums.InterviewStatus;
import com.Vijay.SkillX.Enums.RoundType;
import com.Vijay.SkillX.Repo.AnswerRepo;
import com.Vijay.SkillX.Repo.CompanyRepo;
import com.Vijay.SkillX.Repo.InterviewRepo;
import com.Vijay.SkillX.Repo.InterviewResultRepo;
import com.Vijay.SkillX.Repo.QuestionRepo;
import com.Vijay.SkillX.Service.GeminiInterviewService;
import com.Vijay.SkillX.Service.InterviewService;
import com.fasterxml.jackson.databind.JsonNode;

@Service
@Transactional
public class InterviewServiceImpl implements InterviewService {

    private final CompanyRepo companyRepository;

    private final InterviewRepo interviewRepository;

    private final QuestionRepo questionRepository;

    private final AnswerRepo answerRepository;

    private final InterviewResultRepo resultRepository;

    private final GeminiInterviewService gemini;

    public InterviewServiceImpl(
            CompanyRepo companyRepository,
            InterviewRepo interviewRepository,
            QuestionRepo questionRepository,
            AnswerRepo answerRepository,
            InterviewResultRepo resultRepository,
            GeminiInterviewService gemini) {

        this.companyRepository = companyRepository;
        this.interviewRepository = interviewRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.resultRepository = resultRepository;
        this.gemini = gemini;
    }

    @Override
    public StartInterviewResponse startInterview(
            StartInterviewRequest request) {

        // NOTE: assumes Interview has a `Company company` relation and
        // CompanyRepo exposes findByName(String). Adjust the lookup below
        // if your Company entity is keyed differently (e.g. by id).
        Company company =
                companyRepository
                        .findByName(request.getCompany())
                        .orElseGet(() -> {
                            Company newCompany = new Company();
                            newCompany.setName(request.getCompany());
                            return companyRepository.save(newCompany);
                        });

        Interview interview = new Interview();

        interview.setCompany(company);
        interview.setRole(request.getRole());
        interview.setExperience(request.getExperience());

        interview.setTechnologies(
                String.join(", ", request.getTechnologies())
        );

        interview.setStatus(
                InterviewStatus.CODING
        );

        Instant now = Instant.now();

        interview.setStartedAt(now);

        interview.setRoundStartedAt(now);

        interview.setRoundEndAt(
                now.plus(
                        Duration.ofMinutes(30)
                )
        );

        interview.setCurrentQuestionNumber(1);

        interviewRepository.save(interview);

        createQuestion(
                interview,
                RoundType.CODING,
                1
        );

        return new StartInterviewResponse(
                interview.getId(),
                company.getName(),
                interview.getRole(),
                "CODING",
                interview.getRoundEndAt(),
                1800
        );
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionResponse getCurrentQuestion(
            Long interviewId) {

        Interview interview =
                getInterview(interviewId);

        checkTimer(interview);

        RoundType round =
                getRoundType(interview);

        Question question =
                questionRepository
                        .findFirstByInterviewIdAndRoundTypeAndQuestionOrder(
                                interviewId,
                                round,
                                interview.getCurrentQuestionNumber()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Question not found"
                                )
                        );

        int total =
                getTotalQuestions(round);

        return new QuestionResponse(
                question.getId(),
                round.name(),
                question.getTechnology(),
                question.getQuestionText(),
                question.getQuestionOrder(),
                total,
                remainingSeconds(interview)
        );
    }

    @Override
    public AnswerResponse submitAnswer(
            Long interviewId,
            AnswerRequest request) {

        Interview interview =
                getInterview(interviewId);

        checkTimer(interview);

        Question question =
                questionRepository
                        .findById(request.getQuestionId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Question not found"
                                )
                        );

        if (!question.getInterview()
                .getId()
                .equals(interviewId)) {

            throw new RuntimeException(
                    "Question does not belong to this interview"
            );
        }

        JsonNode evaluation =
                gemini.evaluateAnswer(
                        interview.getCompany().getName(),
                        interview.getRole(),
                        interview.getStatus().name(),
                        question.getTechnology(),
                        question.getQuestionText(),
                        request.getAnswer()
                );

        int score =
                evaluation
                        .path("score")
                        .asInt(0);

        String feedback =
                evaluation
                        .path("feedback")
                        .asText("");

        String idealAnswer =
                evaluation
                        .path("idealAnswer")
                        .asText("");

        Answer answer = new Answer();

        answer.setQuestion(question);

        answer.setAnswerText(
                request.getAnswer()
        );

        answer.setScore(score);

        answer.setFeedback(feedback);

        answer.setIdealAnswer(idealAnswer);

        answerRepository.save(answer);

        question.setScore(score);

        questionRepository.save(question);

        boolean roundCompleted =
                isRoundCompleted(interview);

        if (roundCompleted) {

            moveToNextRound(interview);

            interviewRepository.save(interview);

            return new AnswerResponse(
                    question.getId(),
                    score,
                    feedback,
                    idealAnswer,
                    true,
                    interview.getStatus().name()
            );
        }

        return new AnswerResponse(
                question.getId(),
                score,
                feedback,
                idealAnswer,
                false,
                interview.getStatus().name()
        );
    }

    @Override
    public QuestionResponse nextQuestion(
            Long interviewId) {

        Interview interview =
                getInterview(interviewId);

        checkTimer(interview);

        if (interview.getStatus()
                == InterviewStatus.COMPLETED) {

            throw new RuntimeException(
                    "Interview already completed"
            );
        }

        int next =
                interview.getCurrentQuestionNumber() + 1;

        RoundType round =
                getRoundType(interview);

        if (next > getTotalQuestions(round)) {

            throw new RuntimeException(
                    "Round questions completed"
            );
        }

        interview.setCurrentQuestionNumber(next);

        createQuestion(
                interview,
                round,
                next
        );

        interviewRepository.save(interview);

        return getCurrentQuestion(interviewId);
    }

    @Override
    public FinalResultResponse finishInterview(
            Long interviewId) {

        Interview interview =
                getInterview(interviewId);

        interview.setStatus(
                InterviewStatus.COMPLETED
        );

        interview.setRoundEndAt(
                Instant.now()
        );

        interviewRepository.save(interview);

        return calculateFinalResult(interview);
    }

    @Override
    @Transactional(readOnly = true)
    public FinalResultResponse getResult(
            Long interviewId) {

        Interview interview =
                getInterview(interviewId);

        return calculateFinalResult(interview);
    }

    private void createQuestion(
            Interview interview,
            RoundType round,
            int questionNumber) {

        JsonNode json =
                gemini.generateQuestion(
                        interview.getCompany().getName(),
                        interview.getRole(),
                        interview.getExperience(),
                        interview.getTechnologies(),
                        round.name(),
                        questionNumber
                );

        Question question =
                new Question();

        question.setInterview(interview);

        question.setRoundType(round);

        question.setTechnology(
                json.path("technology")
                        .asText("General")
        );

        question.setQuestionText(
                json.path("question")
                        .asText()
        );

        question.setExpectedAnswer(
                json.path("expectedAnswer")
                        .asText("")
        );

        question.setQuestionOrder(
                questionNumber
        );

        questionRepository.save(question);
    }

    private void moveToNextRound(
            Interview interview) {

        switch (interview.getStatus()) {

            case CODING -> {

                interview.setStatus(
                        InterviewStatus.APTITUDE
                );

                startRound(
                        interview,
                        10
                );

                createQuestion(
                        interview,
                        RoundType.APTITUDE,
                        1
                );
            }

            case APTITUDE -> {

                interview.setStatus(
                        InterviewStatus.TECHNICAL
                );

                startRound(
                        interview,
                        10
                );

                createQuestion(
                        interview,
                        RoundType.TECHNICAL,
                        1
                );
            }

            case TECHNICAL -> {

                interview.setStatus(
                        InterviewStatus.HR
                );

                startRound(
                        interview,
                        20
                );

                createQuestion(
                        interview,
                        RoundType.HR,
                        1
                );
            }

            case HR -> {

                interview.setStatus(
                        InterviewStatus.COMPLETED
                );

                interview.setRoundEndAt(
                        Instant.now()
                );
            }

            default -> {
            }
        }
    }

    private void startRound(
            Interview interview,
            int minutes) {

        Instant now = Instant.now();

        interview.setRoundStartedAt(now);

        interview.setRoundEndAt(
                now.plus(
                        Duration.ofMinutes(minutes)
                )
        );

        interview.setCurrentQuestionNumber(1);
    }

    private boolean isRoundCompleted(
            Interview interview) {

        RoundType round =
                getRoundType(interview);

        List<Question> questions =
                questionRepository
                        .findByInterviewIdAndRoundTypeOrderByQuestionOrder(
                                interview.getId(),
                                round
                        );

        int totalRequired =
                getTotalQuestions(round);

        // Fetch all answers for this interview once, rather than
        // re-querying inside the stream filter (was previously an N+1).
        List<Answer> answers =
                answerRepository
                        .findByQuestionInterviewId(
                                interview.getId()
                        );

        long answered =
                questions.stream()
                        .filter(question ->
                                answers.stream()
                                        .anyMatch(answer ->
                                                answer.getQuestion()
                                                        .getId()
                                                        .equals(
                                                                question.getId()
                                                        )
                                        )
                        )
                        .count();

        return answered >= totalRequired;
    }

    private int getTotalQuestions(
            RoundType round) {

        return switch (round) {

            case CODING -> 1;

            case APTITUDE -> 2;

            case TECHNICAL -> 2;

            case HR -> 2;

            default -> 0;
        };
    }

    private RoundType getRoundType(
            Interview interview) {

        return switch (interview.getStatus()) {

            case CODING ->
                    RoundType.CODING;

            case APTITUDE ->
                    RoundType.APTITUDE;

            case TECHNICAL ->
                    RoundType.TECHNICAL;

            case HR ->
                    RoundType.HR;

            default ->
                    RoundType.COMPLETED;
        };
    }

    private void checkTimer(
            Interview interview) {

        if (interview.getStatus()
                == InterviewStatus.COMPLETED) {

            throw new RuntimeException(
                    "Interview completed"
            );
        }

        if (interview.getRoundEndAt() == null) {

            throw new RuntimeException(
                    "Round timer not initialized"
            );
        }

        if (Instant.now()
                .isAfter(
                        interview.getRoundEndAt()
                )) {

            interview.setStatus(
                    InterviewStatus.EXPIRED
            );

            interviewRepository.save(interview);

            throw new RuntimeException(
                    "Time limit exceeded for this round"
            );
        }
    }

    private long remainingSeconds(
            Interview interview) {

        long seconds =
                Duration.between(
                        Instant.now(),
                        interview.getRoundEndAt()
                ).getSeconds();

        return Math.max(
                seconds,
                0
        );
    }

    private FinalResultResponse calculateFinalResult(
            Interview interview) {

        List<Answer> answers =
                answerRepository
                        .findByQuestionInterviewId(
                                interview.getId()
                        );

        double coding =
                average(
                        answers,
                        RoundType.CODING
                );

        double aptitude =
                average(
                        answers,
                        RoundType.APTITUDE
                );

        double technical =
                average(
                        answers,
                        RoundType.TECHNICAL
                );

        double hr =
                average(
                        answers,
                        RoundType.HR
                );

        /*
         * Weightage:
         *
         * Coding    = 25%
         * Aptitude  = 15%
         * Technical = 40%
         * HR        = 20%
         */

        double finalScore =
                coding * 0.25
                        + aptitude * 0.15
                        + technical * 0.40
                        + hr * 0.20;

        finalScore =
                round(finalScore);

        String recommendation;

        if (finalScore >= 80) {

            recommendation =
                    "INTERVIEW READY";

        } else if (finalScore >= 65) {

            recommendation =
                    "NEEDS MINOR PREPARATION";

        } else if (finalScore >= 50) {

            recommendation =
                    "NEEDS MORE PREPARATION";

        } else {

            recommendation =
                    "NOT READY";
        }

        InterviewResult result =
                resultRepository
                        .findByInterviewId(
                                interview.getId()
                        )
                        .orElse(
                                new InterviewResult()
                        );

        result.setInterview(interview);

        result.setCodingScore(coding);

        result.setAptitudeScore(aptitude);

        result.setTechnicalScore(technical);

        result.setHrScore(hr);

        result.setFinalScore(finalScore);

        result.setRecommendation(
                recommendation
        );

        result.setStrengths(
                generateStrengths(
                        coding,
                        aptitude,
                        technical,
                        hr
                )
        );

        result.setWeaknesses(
                generateWeaknesses(
                        coding,
                        aptitude,
                        technical,
                        hr
                )
        );

        result.setPreparationPlan(
                generatePreparationPlan(
                        coding,
                        aptitude,
                        technical,
                        hr
                )
        );

        resultRepository.save(result);

        return new FinalResultResponse(
                interview.getId(),
                interview.getCompany().getName(),
                interview.getRole(),
                coding,
                aptitude,
                technical,
                hr,
                finalScore,
                recommendation,
                result.getStrengths(),
                result.getWeaknesses(),
                result.getPreparationPlan()
        );
    }

    private double average(
            List<Answer> answers,
            RoundType round) {

        return answers.stream()
                .filter(answer ->
                        answer.getQuestion()
                                .getRoundType()
                                == round
                )
                .mapToInt(
                        Answer::getScore
                )
                .average()
                .orElse(0);
    }

    private String generateStrengths(
            double coding,
            double aptitude,
            double technical,
            double hr) {

        StringBuilder result =
                new StringBuilder();

        if (coding >= 70) {

            result.append(
                    "Good programming ability. "
            );
        }

        if (aptitude >= 70) {

            result.append(
                    "Good aptitude and logical reasoning. "
            );
        }

        if (technical >= 70) {

            result.append(
                    "Strong technical knowledge. "
            );
        }

        if (hr >= 70) {

            result.append(
                    "Good communication and HR performance. "
            );
        }

        if (result.isEmpty()) {

            result.append(
                    "Keep practicing fundamentals."
            );
        }

        return result.toString();
    }

    private String generateWeaknesses(
            double coding,
            double aptitude,
            double technical,
            double hr) {

        StringBuilder result =
                new StringBuilder();

        if (coding < 70) {

            result.append(
                    "Improve coding/problem solving. "
            );
        }

        if (aptitude < 70) {

            result.append(
                    "Practice aptitude and logical reasoning. "
            );
        }

        if (technical < 70) {

            result.append(
                    "Revise technical concepts and frameworks. "
            );
        }

        if (hr < 70) {

            result.append(
                    "Improve HR communication and behavioral answers. "
            );
        }

        if (result.isEmpty()) {

            result.append(
                    "No major weakness detected."
            );
        }

        return result.toString();
    }

    private String generatePreparationPlan(
            double coding,
            double aptitude,
            double technical,
            double hr) {

        StringBuilder result =
                new StringBuilder();

        if (coding < 70) {

            result.append(
                    "Practice 1 coding problems every day. "
            );
        }

        if (aptitude < 70) {

            result.append(
                    "Practice quantitative and logical reasoning. "
            );
        }

        if (technical < 70) {

            result.append(
                    "Revise your selected languages, frameworks and databases. "
            );
        }

        if (hr < 70) {

            result.append(
                    "Practice HR and behavioral interview questions. "
            );
        }

        if (result.isEmpty()) {

            result.append(
                    "Continue practicing full-length mock interviews."
            );
        }

        return result.toString();
    }

    private double round(
            double value) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }

    private Interview getInterview(
            Long interviewId) {

        return interviewRepository
                .findById(interviewId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Interview not found: "
                                        + interviewId
                        )
                );
    }
}