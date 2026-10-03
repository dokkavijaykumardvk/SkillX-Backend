package com.Vijay.SkillX.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Vijay.SkillX.DTO.AnswerRequest;
import com.Vijay.SkillX.DTO.AnswerResponse;
import com.Vijay.SkillX.DTO.FinalResultResponse;
import com.Vijay.SkillX.DTO.QuestionResponse;
import com.Vijay.SkillX.DTO.StartInterviewRequest;
import com.Vijay.SkillX.DTO.StartInterviewResponse;
import com.Vijay.SkillX.Service.InterviewService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/interviews")
@CrossOrigin(origins = "http://localhost:4200")
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(
            InterviewService interviewService) {

        this.interviewService = interviewService;
    }

    @PostMapping("/start")
    public ResponseEntity<StartInterviewResponse> startInterview(
            @Valid
            @RequestBody
            StartInterviewRequest request) {

        return ResponseEntity.ok(
                interviewService.startInterview(request)
        );
    }

    @GetMapping("/{id}/question")
    public ResponseEntity<QuestionResponse> currentQuestion(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                interviewService.getCurrentQuestion(id)
        );
    }

    @PostMapping("/{id}/answer")
    public ResponseEntity<AnswerResponse> submitAnswer(
            @PathVariable Long id,

            @Valid
            @RequestBody
            AnswerRequest request) {

        return ResponseEntity.ok(
                interviewService.submitAnswer(
                        id,
                        request
                )
        );
    }

    @GetMapping("/{id}/next")
    public ResponseEntity<QuestionResponse> nextQuestion(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                interviewService.nextQuestion(id)
        );
    }

    @PostMapping("/{id}/finish")
    public ResponseEntity<FinalResultResponse> finishInterview(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                interviewService.finishInterview(id)
        );
    }

    @GetMapping("/{id}/result")
    public ResponseEntity<FinalResultResponse> result(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                interviewService.getResult(id)
        );
    }
}