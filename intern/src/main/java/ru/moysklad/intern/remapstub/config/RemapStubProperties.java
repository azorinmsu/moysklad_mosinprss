package ru.moysklad.intern.remapstub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.remap-stub")
public class RemapStubProperties {

    /**
     * Публичный базовый URL приложения без завершающего слэша.
     * Используется в meta.href ответов Remap API.
     */
    private String publicBaseUrl = "http://localhost:8080";

    /**
     * Включить заглушку Remap API (контроллеры + сиды).
     */
    private boolean enabled = true;

    /**
     * Автоматически загружать тестовые данные при старте, если таблица товаров пуста.
     */
    private boolean seedOnStartup = true;

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isSeedOnStartup() {
        return seedOnStartup;
    }

    public void setSeedOnStartup(boolean seedOnStartup) {
        this.seedOnStartup = seedOnStartup;
    }

    public String apiBase() {
        String base = publicBaseUrl == null ? "" : publicBaseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/api/remap/1.2";
    }
}
