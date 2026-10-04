package ru.moysklad.intern.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class Configuration {
    public Configuration() {}

    @Value("${credentials.login}")
    private String login;
    public String getLogin() { return login; }

    @Value("${credentials.password}")
    private String password;
    public String getPassword() { return password; }

    // temp function
    public String credentialsToBase64() {
        return Base64.getEncoder().encodeToString((this.login + ":" + this.password).getBytes());
    }
}
