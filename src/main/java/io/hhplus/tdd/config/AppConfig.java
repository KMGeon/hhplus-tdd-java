package io.hhplus.tdd.config;

import io.hhplus.tdd.domain.TimeProvider;
import io.hhplus.tdd.domain.UserPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Bean
    public TimeProvider configureUserPoint(TimeProvider timeProvider) {
        return UserPoint.setTimeProvider(timeProvider);
    }
}