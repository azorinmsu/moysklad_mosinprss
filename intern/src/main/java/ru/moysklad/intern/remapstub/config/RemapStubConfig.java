package ru.moysklad.intern.remapstub.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RemapStubProperties.class)
@ConditionalOnProperty(prefix = "app.remap-stub", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RemapStubConfig {
}
