package com.quickcommerce.order.config;

import org.springframework.context.annotation.Bean;

import java.time.Clock;

public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
