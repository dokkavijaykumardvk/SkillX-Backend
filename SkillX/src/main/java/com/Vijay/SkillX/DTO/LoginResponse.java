package com.Vijay.SkillX.DTO;

public class LoginResponse {

    private Long candidateId;
    private String name;

    public LoginResponse() {
    }

    public LoginResponse(Long candidateId, String name) {
        this.candidateId = candidateId;
        this.name = name;
    }

    public Long getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(Long candidateId) {
        this.candidateId = candidateId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}