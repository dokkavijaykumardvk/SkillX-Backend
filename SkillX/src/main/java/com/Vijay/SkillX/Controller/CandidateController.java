package com.Vijay.SkillX.Controller;

import java.time.Instant;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Vijay.SkillX.DTO.LoginRequest;
import com.Vijay.SkillX.DTO.LoginResponse;
import com.Vijay.SkillX.Entity.Candidate;
import com.Vijay.SkillX.Repo.CandidateRepo;

@RestController
@RequestMapping("/api/candidate")
@CrossOrigin(origins = "http://localhost:4200")
public class CandidateController {

    private final CandidateRepo candidateRepo;

    public CandidateController(CandidateRepo candidateRepo) {
        this.candidateRepo = candidateRepo;
    }

    /**
     * No password, no session token — just records the candidate's name
     * and hands back an id the frontend can keep around (in memory or
     * localStorage) for the rest of the visit. Logout is handled entirely
     * on the frontend by discarding that id/name; there is nothing to
     * invalidate server-side.
     */
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {

        String name = request.getName() == null
                ? ""
                : request.getName().trim();

        if (name.isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }

        Candidate candidate = new Candidate();
        candidate.setName(name);
        candidate.setLoggedInAt(Instant.now());

        candidateRepo.save(candidate);

        return new LoginResponse(candidate.getId(), candidate.getName());
    }
}