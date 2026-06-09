package com.llmscoring;

import com.llmscoring.config.ScoringConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(ScoringConfig.class)
@EnableScheduling
public class LlmScoringServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(LlmScoringServiceApplication.class, args);
	}

}
