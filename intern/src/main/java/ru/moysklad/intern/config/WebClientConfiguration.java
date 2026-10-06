package ru.moysklad.intern.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfiguration {
    private static final String apiMoySklad = "https://api.moysklad.ru/api/remap/1.2/";
    private static final int timeout = 1000;

    @Bean
    public WebClient webClientWithTimeout() {
        final var tcpClient = TcpClient
                .create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout)

                .doOnConnected(connection -> {
                    connection.addHandlerLast(new ReadTimeoutHandler(timeout, TimeUnit.MILLISECONDS));
                    connection.addHandlerLast(new WriteTimeoutHandler(timeout, TimeUnit.MILLISECONDS));
                });
        return WebClient
                .builder()
                .baseUrl(apiMoySklad)
                .defaultHeader(HttpHeaders.AUTHORIZATION, performCredentialsToBase64())
                .clientConnector(new ReactorClientHttpConnector(HttpClient.from(tcpClient)))
                .build();
    }
    @Value("${api.credentials.login}")
    private String clientLogin;
    public String getClientLogin() { return clientLogin; }

    @Value("${api.credentials.password}")
    private String clientPassword;
    public String getClientPassword() { return clientPassword; }

    public String performCredentialsToBase64() {
        return  "Basic " + Base64
                .getEncoder()
                .encodeToString(
                        (this.clientLogin + ":" + this.clientPassword).getBytes());
    }
}
