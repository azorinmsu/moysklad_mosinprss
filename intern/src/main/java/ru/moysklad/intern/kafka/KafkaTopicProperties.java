package ru.moysklad.intern.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.topics")
public class KafkaTopicProperties {

    private String stockCommands = "stock.commands";
    private String salesCommands = "sales.commands";
    private String processingResults = "processing.results";
    private String processingResultsDlq = "processing.results.dlq";

    public String getStockCommands() {
        return stockCommands;
    }

    public void setStockCommands(String stockCommands) {
        this.stockCommands = stockCommands;
    }

    public String getSalesCommands() {
        return salesCommands;
    }

    public void setSalesCommands(String salesCommands) {
        this.salesCommands = salesCommands;
    }

    public String getProcessingResults() {
        return processingResults;
    }

    public void setProcessingResults(String processingResults) {
        this.processingResults = processingResults;
    }

    public String getProcessingResultsDlq() {
        return processingResultsDlq;
    }

    public void setProcessingResultsDlq(String processingResultsDlq) {
        this.processingResultsDlq = processingResultsDlq;
    }
}
