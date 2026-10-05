package com.ats.scoring.client;

import com.ats.scoring.dto.ScoringDtos.CandidateDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "resume-service", url = "http://localhost:8000")
public interface ResumeServiceClient {
    @GetMapping("/api/v1/resumes/{candidate_id}")
    CandidateDto getResumeById(@PathVariable("candidate_id") String candidateId);
}