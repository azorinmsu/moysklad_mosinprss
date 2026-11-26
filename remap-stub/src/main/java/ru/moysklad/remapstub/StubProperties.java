package ru.moysklad.remapstub;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "remap.stub")
public class StubProperties {

    private String publicBaseUrl = "http://localhost:8088";
    /**
     * Пустая строка — принимается любой непустой Authorization.
     * Непустое значение — требуется {@code Bearer} с этим токеном.
     */
    private String requiredToken = "";

    public String publicBaseUrl() {
        return publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public String requiredToken() {
        return requiredToken == null ? "" : requiredToken;
    }

    public void setRequiredToken(String requiredToken) {
        this.requiredToken = requiredToken;
    }
}
