package com.ats.scoring.client;

import com.ats.scoring.dto.ScoringDtos.JobDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "job-service", url = "http://localhost:8080")
public interface JobServiceClient {
    @GetMapping("/jobs/{id}")
    JobDto getJobById(@PathVariable("id") Long id);
}