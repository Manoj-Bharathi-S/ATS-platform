package com.ats.scoring.controller;

import com.ats.scoring.dto.ScoringDtos.ScoreResultDto;
import com.ats.scoring.service.ScoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/score")
@CrossOrigin(origins = "*") // <-- ADD THIS LINE
public class ScoringController {

    private final ScoringService scoringService;

    public ScoringController(ScoringService scoringService) {
        this.scoringService = scoringService;
    }

    @PostMapping
    public ResponseEntity<ScoreResultDto> scoreCandidate(
            @RequestParam Long jobId,
            @RequestParam String candidateId) {
        try {
            ScoreResultDto result = scoringService.calculateScore(jobId, candidateId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
             System.err.println("Error calculating score: " + e.getMessage());
             // In a production app, handle 404s from Feign clients more specifically here
             return ResponseEntity.badRequest().build();
        }
    }
}