package ru.moysklad.intern.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import ru.moysklad.remap_1_2.ApiClient;

@Configuration
public class RemapClientConfiguration {

    @Value("${app.remap.host:localhost:8080}")
    private String host;

    @Value("${app.remap.login:student@company}")
    private String login;

    @Value("${app.remap.password:password}")
    private String password;

    @Bean
    public ApiClient remapApiClient() {
        return new ApiClient(host, false, login, password);
    }

    @Bean
    public RestClient remapRestClient() {
        return RestClient.builder()
                .baseUrl("http://" + host + "/api/remap/1.2")
                .defaultHeaders(headers -> headers.setBasicAuth(login, password))
                .build();
    }
}
