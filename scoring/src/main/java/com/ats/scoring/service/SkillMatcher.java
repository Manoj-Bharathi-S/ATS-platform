package com.ats.scoring.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Canonicalizes skill names so resume extraction and job requirements can be
 * compared even when punctuation, spacing, or aliases differ
 * (e.g. React.js vs React, k8s vs Kubernetes).
 */
public final class SkillMatcher {

    private static final Map<String, String> ALIASES = new HashMap<>();

    static {
        alias("python", "python", "python3");
        alias("java", "java");
        alias("javascript", "javascript", "js", "ecmascript");
        alias("typescript", "typescript", "ts");
        alias("cpp", "cpp", "cplusplus", "cplusplus");
        alias("csharp", "csharp", "csharp");
        alias("go", "go", "golang", "golang");
        alias("rust", "rust");
        alias("ruby", "ruby", "rubyonrails", "rails");
        alias("php", "php");
        alias("swift", "swift");
        alias("kotlin", "kotlin");
        alias("scala", "scala");
        alias("sql", "sql");
        alias("html", "html", "html5");
        alias("css", "css", "css3");
        alias("react", "react", "reactjs");
        alias("angular", "angular", "angularjs");
        alias("vue", "vue", "vuejs");
        alias("nextjs", "nextjs");
        alias("nodejs", "nodejs", "node");
        alias("express", "express", "expressjs");
        alias("springboot", "springboot", "spring", "springframework");
        alias("hibernate", "hibernate");
        alias("django", "django");
        alias("flask", "flask");
        alias("fastapi", "fastapi");
        alias("rest", "rest", "restapi", "restful");
        alias("graphql", "graphql");
        alias("grpc", "grpc");
        alias("mysql", "mysql");
        alias("postgresql", "postgresql", "postgres", "psql");
        alias("mongodb", "mongodb", "mongo");
        alias("redis", "redis");
        alias("oracle", "oracle", "oracledb", "oracledatabase");
        alias("sqlserver", "sqlserver", "mssql", "mssql");
        alias("elasticsearch", "elasticsearch");
        alias("kafka", "kafka", "apachekafka");
        alias("rabbitmq", "rabbitmq");
        alias("docker", "docker");
        alias("kubernetes", "kubernetes", "k8s");
        alias("aws", "aws", "amazonwebservices", "amazonaws");
        alias("azure", "azure", "microsoftazure");
        alias("gcp", "gcp", "googlecloud", "googlecloudplatform");
        alias("git", "git");
        alias("github", "github");
        alias("gitlab", "gitlab");
        alias("cicd", "cicd", "continuousintegration");
        alias("jenkins", "jenkins");
        alias("terraform", "terraform");
        alias("linux", "linux");
        alias("unix", "unix");
        alias("pandas", "pandas");
        alias("numpy", "numpy");
        alias("tensorflow", "tensorflow");
        alias("pytorch", "pytorch");
        alias("scikitlearn", "scikitlearn", "sklearn");
        alias("spark", "spark", "apachespark", "pyspark");
        alias("hadoop", "hadoop");
        alias("airflow", "airflow", "apacheairflow");
        alias("tableau", "tableau");
        alias("powerbi", "powerbi");
        alias("excel", "excel", "microsoftexcel", "msexcel");
        alias("jira", "jira");
        alias("agile", "agile", "scrum");
        alias("microservices", "microservices", "microservice");
        alias("oop", "oop", "objectoriented");
        alias("datastructures", "datastructures", "dsa");
        alias("machinelearning", "machinelearning", "ml");
        alias("deeplearning", "deeplearning");
        alias("nlp", "nlp", "naturallanguageprocessing");
        alias("llm", "llm", "largelanguagemodel", "largelanguagemodels");
        alias("openai", "openai", "gpt", "chatgpt");
        alias("langchain", "langchain");
        alias("selenium", "selenium");
        alias("junit", "junit");
        alias("maven", "maven");
        alias("gradle", "gradle");
        alias("webpack", "webpack");
        alias("redux", "redux");
        alias("sass", "sass", "scss");
        alias("tailwind", "tailwind", "tailwindcss");
        alias("bootstrap", "bootstrap");
        alias("figma", "figma");
        alias("android", "android");
        alias("ios", "ios");
        alias("reactnative", "reactnative");
        alias("flutter", "flutter");
        alias("dotnet", "dotnet", "aspnet");
    }

    private SkillMatcher() {}

    public static String canonicalKey(String skill) {
        if (skill == null) {
            return "";
        }
        String prepared = skill.toLowerCase(Locale.ROOT).trim();
        prepared = prepared.replace("c++", "cpp")
                .replace("c#", "csharp")
                .replace("f#", "fsharp")
                .replace(".net", "dotnet");
        String key = prepared.replaceAll("[^a-z0-9]", "");
        if (key.isEmpty()) {
            return "";
        }
        return ALIASES.getOrDefault(key, key);
    }

    public static Set<String> canonicalKeys(List<String> skills) {
        Set<String> keys = new LinkedHashSet<>();
        if (skills == null) {
            return keys;
        }
        for (String skill : skills) {
            String key = canonicalKey(skill);
            if (!key.isEmpty()) {
                keys.add(key);
            }
        }
        return keys;
    }

    public static MatchResult match(List<String> requiredSkills, List<String> candidateSkills) {
        Set<String> applicantKeys = canonicalKeys(candidateSkills);
        List<String> required = requiredSkills != null ? requiredSkills : List.of();

        Set<String> requiredKeys = canonicalKeys(required);
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        Set<String> seenMatched = new LinkedHashSet<>();
        Set<String> seenMissing = new LinkedHashSet<>();

        for (String skill : required) {
            String key = canonicalKey(skill);
            if (key.isEmpty()) {
                continue;
            }
            if (applicantKeys.contains(key)) {
                if (seenMatched.add(key)) {
                    matched.add(skill);
                }
            } else if (seenMissing.add(key)) {
                missing.add(skill);
            }
        }

        double score = 0.0;
        if (!requiredKeys.isEmpty()) {
            long hitCount = requiredKeys.stream().filter(applicantKeys::contains).count();
            score = (hitCount * 100.0) / requiredKeys.size();
        }
        return new MatchResult(score, matched, missing);
    }

    private static void alias(String canonical, String... keys) {
        ALIASES.put(canonical, canonical);
        for (String key : keys) {
            ALIASES.put(key, canonical);
        }
    }

    public static final class MatchResult {
        private final double scorePercentage;
        private final List<String> matchedSkills;
        private final List<String> missingSkills;

        public MatchResult(double scorePercentage, List<String> matchedSkills, List<String> missingSkills) {
            this.scorePercentage = scorePercentage;
            this.matchedSkills = matchedSkills;
            this.missingSkills = missingSkills;
        }

        public double getScorePercentage() {
            return scorePercentage;
        }

        public List<String> getMatchedSkills() {
            return matchedSkills;
        }

        public List<String> getMissingSkills() {
            return missingSkills;
        }
    }
}
