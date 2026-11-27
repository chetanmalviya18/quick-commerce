package com.quickcommerce.order.client;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

public class ServiceAuthFeignConfig {

    @Bean
    public RequestInterceptor serviceTokenInterceptor(ServiceTokenProvider tokenProvider) {
        return template -> template
                .header("Authorization", "Bearer " + tokenProvider.token());
    }
}
