package ru.moysklad.remapstub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(StubProperties.class)
public class RemapStubApplication {

    public static void main(String[] args) {
        SpringApplication.run(RemapStubApplication.class, args);
    }
}
