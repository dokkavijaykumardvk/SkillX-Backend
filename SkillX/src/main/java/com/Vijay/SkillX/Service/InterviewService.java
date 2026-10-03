package com.Vijay.SkillX.Service;

import com.Vijay.SkillX.DTO.AnswerRequest;
import com.Vijay.SkillX.DTO.AnswerResponse;
import com.Vijay.SkillX.DTO.FinalResultResponse;
import com.Vijay.SkillX.DTO.QuestionResponse;
import com.Vijay.SkillX.DTO.StartInterviewRequest;
import com.Vijay.SkillX.DTO.StartInterviewResponse;

public interface InterviewService {

    StartInterviewResponse startInterview(
            StartInterviewRequest request
    );

    QuestionResponse getCurrentQuestion(
            Long interviewId
    );

    AnswerResponse submitAnswer(
            Long interviewId,
            AnswerRequest request
    );

    QuestionResponse nextQuestion(
            Long interviewId
    );

    FinalResultResponse finishInterview(
            Long interviewId
    );

    FinalResultResponse getResult(
            Long interviewId
    );
}