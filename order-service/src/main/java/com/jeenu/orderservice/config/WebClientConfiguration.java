package com.jeenu.orderservice.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfiguration {
    @Bean
    public WebClient webClient(){
        return WebClient.builder().build();
    }

    @Bean
    @LoadBalanced // balancing the load - to give service name instead of localhost:portNumber
    public WebClient.Builder webClientBuilder(){
        return WebClient.builder();
    }
}
