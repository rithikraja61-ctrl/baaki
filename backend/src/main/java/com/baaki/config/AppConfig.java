package com.baaki.config;

import com.baaki.repository.BusinessProfileRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AppConfig implements WebMvcConfigurer {
    private final AuthInterceptor authInterceptor;

    public AppConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Bean
    public DemoClock clock() { return new DemoClock(); }

    @Bean
    ApplicationRunner loadDemoDate(DemoClock clock, BusinessProfileRepository repo) {
        return args -> repo.findById(1L).ifPresent(p -> clock.setDemoDate(p.getDemoDate()));
    }

    @org.springframework.beans.factory.annotation.Value("${baaki.cors.allowed-origins:}")
    private String customOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        java.util.List<String> origins = new java.util.ArrayList<>();
        origins.add("http://localhost:*");
        origins.add("http://127.0.0.1:*");
        origins.add("https://*.onrender.com");
        if (customOrigins != null && !customOrigins.isBlank()) {
            for (String origin : customOrigins.split(",")) {
                String trimmed = origin.trim();
                if (!trimmed.isEmpty()) {
                    origins.add(trimmed);
                }
            }
        }

        registry.addMapping("/api/**")
                .allowedOriginPatterns(origins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor).addPathPatterns("/api/**");
    }
}
