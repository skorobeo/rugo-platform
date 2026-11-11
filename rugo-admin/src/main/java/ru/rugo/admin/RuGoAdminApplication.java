package ru.rugo.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"ru.rugo.common", "ru.rugo.core", "ru.rugo.admin"})
@EnableScheduling
@EntityScan(basePackages = "ru.rugo.core.entity")
@EnableJpaRepositories(basePackages = "ru.rugo.core.repository")
@ConfigurationPropertiesScan(basePackages = {"ru.rugo.common.config", "ru.rugo.admin.config"})
public class RuGoAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(RuGoAdminApplication.class, args);
    }
}
