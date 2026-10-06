package com.ats.scoring.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * We keep DTOs in a single file for convenience here, but they could be separated.
 */
public class ScoringDtos {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JobDto {
        private Long id;
        private String title;
        private List<String> requiredSkills;
        private Integer minExperienceYears;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public List<String> getRequiredSkills() { return requiredSkills; }
        public void setRequiredSkills(List<String> requiredSkills) { this.requiredSkills = requiredSkills; }
        public Integer getMinExperienceYears() { return minExperienceYears; }
        public void setMinExperienceYears(Integer minExperienceYears) { this.minExperienceYears = minExperienceYears; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CandidateDto {
        @JsonProperty("candidate_id")
        private String candidateId;

        @JsonProperty("extracted_skills")
        private List<String> extractedSkills;

        public String getCandidateId() { return candidateId; }
        public void setCandidateId(String candidateId) { this.candidateId = candidateId; }
        public List<String> getExtractedSkills() { return extractedSkills; }
        public void setExtractedSkills(List<String> extractedSkills) { this.extractedSkills = extractedSkills; }
    }

    public static class ScoreResultDto {
        private String candidateId;
        private Long jobId;
        private double matchScorePercentage;
        private List<String> matchedSkills;
        private List<String> missingSkills;

        public String getCandidateId() { return candidateId; }
        public void setCandidateId(String candidateId) { this.candidateId = candidateId; }
        public Long getJobId() { return jobId; }
        public void setJobId(Long jobId) { this.jobId = jobId; }
        public double getMatchScorePercentage() { return matchScorePercentage; }
        public void setMatchScorePercentage(double matchScorePercentage) { this.matchScorePercentage = matchScorePercentage; }
        public List<String> getMatchedSkills() { return matchedSkills; }
        public void setMatchedSkills(List<String> matchedSkills) { this.matchedSkills = matchedSkills; }
        public List<String> getMissingSkills() { return missingSkills; }
        public void setMissingSkills(List<String> missingSkills) { this.missingSkills = missingSkills; }
    }
}
