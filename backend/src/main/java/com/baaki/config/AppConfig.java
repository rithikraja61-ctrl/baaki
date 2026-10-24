package com.baaki.config;

import com.baaki.repo.BusinessProfileRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AppConfig implements WebMvcConfigurer {

    @Bean
    public DemoClock clock() { return new DemoClock(); }

    @Bean
    ApplicationRunner loadDemoDate(DemoClock clock, BusinessProfileRepository repo) {
        return args -> repo.findById(1L).ifPresent(p -> clock.setDemoDate(p.getDemoDate()));
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "PUT", "DELETE");
    }
}
