package com.ats.scoring.dto;

import java.util.List;

/**
 * We keep DTOs in a single file for convenience here, but they could be separated.
 */
public class ScoringDtos {

    // Matches the JSON from the Job Service (port 8080)
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

    // Matches the JSON from the Python Resume Service (port 8000)
    public static class CandidateDto {
        private String candidate_id; // Maps to Python JSON key
        private List<String> extracted_skills; // Maps to Python JSON key

        public String getCandidate_id() { return candidate_id; }
        public void setCandidate_id(String candidate_id) { this.candidate_id = candidate_id; }
        public List<String> getExtracted_skills() { return extracted_skills; }
        public void setExtracted_skills(List<String> extracted_skills) { this.extracted_skills = extracted_skills; }
    }

    // The output result returned to the user
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