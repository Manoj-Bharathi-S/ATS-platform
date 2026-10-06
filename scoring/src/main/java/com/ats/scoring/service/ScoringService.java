package com.ats.scoring.service;

import com.ats.scoring.client.JobServiceClient;
import com.ats.scoring.client.ResumeServiceClient;
import com.ats.scoring.dto.ScoringDtos.CandidateDto;
import com.ats.scoring.dto.ScoringDtos.JobDto;
import com.ats.scoring.dto.ScoringDtos.ScoreResultDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScoringService {

    private final JobServiceClient jobClient;
    private final ResumeServiceClient resumeClient;

    public ScoringService(JobServiceClient jobClient, ResumeServiceClient resumeClient) {
        this.jobClient = jobClient;
        this.resumeClient = resumeClient;
    }

    public ScoreResultDto calculateScore(Long jobId, String candidateId) {
        JobDto job = jobClient.getJobById(jobId);
        CandidateDto candidate = resumeClient.getResumeById(candidateId);

        if (job == null || candidate == null) {
            throw new RuntimeException("Job or Candidate not found");
        }

        List<String> jobSkills = job.getRequiredSkills() != null ? job.getRequiredSkills() : List.of();
        List<String> candidateSkills = candidate.getExtractedSkills() != null
                ? candidate.getExtractedSkills()
                : List.of();

        SkillMatcher.MatchResult match = SkillMatcher.match(jobSkills, candidateSkills);

        ScoreResultDto result = new ScoreResultDto();
        result.setJobId(jobId);
        result.setCandidateId(candidateId);
        result.setMatchScorePercentage(Math.round(match.getScorePercentage() * 100.0) / 100.0);
        result.setMatchedSkills(match.getMatchedSkills());
        result.setMissingSkills(match.getMissingSkills());

        return result;
    }
}
