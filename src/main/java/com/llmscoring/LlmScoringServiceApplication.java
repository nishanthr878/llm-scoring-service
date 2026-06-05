package com.llmscoring;

import com.llmscoring.config.ScoringConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ScoringConfig.class)
public class LlmScoringServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(LlmScoringServiceApplication.class, args);
	}

}
