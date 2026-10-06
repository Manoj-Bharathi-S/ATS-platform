package com.ats.scoring.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillMatcherTest {

    @Test
    void matchesAliasesAndPunctuationVariants() {
        SkillMatcher.MatchResult result = SkillMatcher.match(
                List.of("React.js", "K8s", "Node JS", "Spring Boot"),
                List.of("React", "Kubernetes", "Node.js", "SpringBoot")
        );

        assertEquals(100.0, result.getScorePercentage());
        assertEquals(4, result.getMatchedSkills().size());
        assertTrue(result.getMissingSkills().isEmpty());
    }

    @Test
    void doesNotTreatJavaAsJavaScript() {
        SkillMatcher.MatchResult result = SkillMatcher.match(
                List.of("Java"),
                List.of("JavaScript")
        );

        assertEquals(0.0, result.getScorePercentage());
        assertEquals(List.of("Java"), result.getMissingSkills());
    }

    @Test
    void scoresUniqueCanonicalSkills() {
        SkillMatcher.MatchResult result = SkillMatcher.match(
                List.of("JavaScript", "JS", "Python"),
                List.of("js")
        );

        assertEquals(50.0, result.getScorePercentage());
        assertEquals(List.of("JavaScript"), result.getMatchedSkills());
        assertEquals(List.of("Python"), result.getMissingSkills());
    }
}
