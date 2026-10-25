package com.baaki.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
@Profile("postgres")
public class PostgresDataSourceConfig {
    private static final Logger log = LoggerFactory.getLogger(PostgresDataSourceConfig.class);

    @Value("${DATABASE_URL:}")
    private String databaseUrl;

    @Value("${SPRING_DATASOURCE_URL:}")
    private String springDatasourceUrl;

    @Value("${SPRING_DATASOURCE_USERNAME:}")
    private String springDatasourceUsername;

    @Value("${SPRING_DATASOURCE_PASSWORD:}")
    private String springDatasourcePassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.postgresql.Driver");

        if (databaseUrl != null && !databaseUrl.isBlank() && (databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://"))) {
            log.info("Configuring PostgreSQL from Render DATABASE_URL");
            try {
                URI uri = new URI(databaseUrl);
                String userInfo = uri.getUserInfo();
                if (userInfo != null) {
                    String[] parts = userInfo.split(":", 2);
                    config.setUsername(parts[0]);
                    if (parts.length > 1) {
                        config.setPassword(parts[1]);
                    }
                }
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String query = uri.getQuery();
                String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getPath()
                        + (query != null ? "?" + query : "");
                config.setJdbcUrl(jdbcUrl);
            } catch (Exception e) {
                log.warn("Failed to parse DATABASE_URL, falling back to SPRING_DATASOURCE_URL: {}", e.getMessage());
                config.setJdbcUrl(springDatasourceUrl);
                config.setUsername(springDatasourceUsername);
                config.setPassword(springDatasourcePassword);
            }
        } else {
            log.info("Configuring PostgreSQL from SPRING_DATASOURCE_URL");
            config.setJdbcUrl(springDatasourceUrl);
            config.setUsername(springDatasourceUsername);
            config.setPassword(springDatasourcePassword);
        }

        return new HikariDataSource(config);
    }
}
