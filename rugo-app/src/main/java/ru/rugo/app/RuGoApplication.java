package ru.rugo.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"ru.rugo.common", "ru.rugo.core", "ru.rugo.app"})
@EnableCaching
@EnableAsync
@EnableScheduling
@EntityScan(basePackages = "ru.rugo.core.entity")
@EnableJpaRepositories(basePackages = "ru.rugo.core.repository")
@ConfigurationPropertiesScan(basePackages = "ru.rugo.common.config")
public class RuGoApplication {

    public static void main(String[] args) {
        SpringApplication.run(RuGoApplication.class, args);
    }
}
