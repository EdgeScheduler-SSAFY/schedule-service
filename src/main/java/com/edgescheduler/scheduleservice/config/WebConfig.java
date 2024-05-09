package com.edgescheduler.scheduleservice.config;

import org.springframework.web.servlet.config.annotation.CorsRegistry;

//@Configuration
public class WebConfig {

    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins("http://localhost:3000", "https://edgescheduler.co.kr/")
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true);
    }
}
