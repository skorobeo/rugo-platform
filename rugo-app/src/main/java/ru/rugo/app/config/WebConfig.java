package ru.rugo.app.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ru.rugo.common.config.AppProperties;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties properties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**").addResourceLocations("file:uploads/");
    }

    /**
     * CORS нужен только для staging (фронт на другом origin). В проде статика и
     * /api отдаются с одного домена через Nginx, поэтому по умолчанию выключен.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String origin = properties.webappOrigin();
        if (origin != null && !origin.isBlank()) {
            registry.addMapping("/api/**")
                    .allowedOrigins(origin)
                    .allowedMethods("GET", "POST", "OPTIONS")
                    .allowedHeaders("Content-Type", "X-Init-Data");
        }
    }
}
