package ru.moysklad.intern.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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

    @Value("${api.credentials.login}")
    private String clientLogin;
    public String getClientLogin() { return clientLogin; }

    @Value("${api.credentials.password}")
    private String clientPassword;
    public String getClientPassword() { return clientPassword; }

    // temp function
    public String credentialsToBase64() {
        return  "Basic " + Base64
                .getEncoder()
                .encodeToString(
                        (this.login + ":" + this.password).getBytes());
    }
}
