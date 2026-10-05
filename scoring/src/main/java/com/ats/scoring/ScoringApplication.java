package com.ats.scoring;

    import org.springframework.boot.SpringApplication;
    import org.springframework.boot.autoconfigure.SpringBootApplication;
    import org.springframework.cloud.openfeign.EnableFeignClients;

    @SpringBootApplication
    @EnableFeignClients(basePackages = "com.ats.scoring.client")
    public class ScoringApplication {
        public static void main(String[] args) {
            SpringApplication.run(ScoringApplication.class, args);
        }
    }