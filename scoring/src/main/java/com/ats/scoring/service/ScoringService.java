package com.ats.scoring.service;

import com.ats.scoring.client.JobServiceClient;
import com.ats.scoring.client.ResumeServiceClient;
import com.ats.scoring.dto.ScoringDtos.CandidateDto;
import com.ats.scoring.dto.ScoringDtos.JobDto;
import com.ats.scoring.dto.ScoringDtos.ScoreResultDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ScoringService {

    private final JobServiceClient jobClient;
    private final ResumeServiceClient resumeClient;

    public ScoringService(JobServiceClient jobClient, ResumeServiceClient resumeClient) {
        this.jobClient = jobClient;
        this.resumeClient = resumeClient;
    }

    public ScoreResultDto calculateScore(Long jobId, String candidateId) {
        // 1. Fetch data from external microservices via Feign
        JobDto job = jobClient.getJobById(jobId);
        CandidateDto candidate = resumeClient.getResumeById(candidateId);

        if (job == null || candidate == null) {
            throw new RuntimeException("Job or Candidate not found");
        }

        // 2. Extract skills safely (handle potential nulls from external APIs)
        List<String> jobSkills = job.getRequiredSkills() != null ? job.getRequiredSkills() : List.of();
        List<String> candidateSkills = candidate.getExtracted_skills() != null ? candidate.getExtracted_skills() : List.of();

        // 3. Convert lists to lowercase sets for case-insensitive matching
        Set<String> requiredSet = jobSkills.stream().map(String::toLowerCase).collect(Collectors.toSet());
        Set<String> applicantSet = candidateSkills.stream().map(String::toLowerCase).collect(Collectors.toSet());

        // 4. Find matches and misses
        List<String> matchedSkills = requiredSet.stream()
                .filter(applicantSet::contains)
                .collect(Collectors.toList());

        List<String> missingSkills = requiredSet.stream()
                .filter(skill -> !applicantSet.contains(skill))
                .collect(Collectors.toList());

        // 5. Calculate Score (Percentage of required skills found)
        double score = 0.0;
        if (!requiredSet.isEmpty()) {
            score = ((double) matchedSkills.size() / requiredSet.size()) * 100.0;
        }

        // 6. Build Result
        ScoreResultDto result = new ScoreResultDto();
        result.setJobId(jobId);
        result.setCandidateId(candidateId);
        result.setMatchScorePercentage(Math.round(score * 100.0) / 100.0); // Round to 2 decimals
        result.setMatchedSkills(matchedSkills);
        result.setMissingSkills(missingSkills);

        return result;
    }
}