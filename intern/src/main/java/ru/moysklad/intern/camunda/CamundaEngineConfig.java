package ru.moysklad.intern.camunda;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.spring.ProcessEngineFactoryBean;
import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Встроенный Camunda 7. Community Spring Boot starter несовместим со Spring Boot 4,
 * поэтому движок собирается здесь и делит DataSource приложения.
 */
@Configuration
public class CamundaEngineConfig {

    @Bean
    SpringProcessEngineConfiguration processEngineConfiguration(
            DataSource dataSource,
            PlatformTransactionManager transactionManager,
            ApplicationContext applicationContext,
            @Value("${app.camunda.job-executor-activate:true}") boolean jobExecutorActivate
    ) {
        SpringProcessEngineConfiguration configuration = new SpringProcessEngineConfiguration();
        configuration.setProcessEngineName("intern");
        configuration.setDataSource(dataSource);
        configuration.setTransactionManager(transactionManager);
        configuration.setApplicationContext(applicationContext);
        configuration.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        configuration.setHistory(ProcessEngineConfiguration.HISTORY_AUDIT);
        configuration.setJobExecutorActivate(jobExecutorActivate);
        configuration.setMetricsEnabled(false);
        configuration.setDbIdentityUsed(false);
        configuration.setAuthorizationEnabled(false);
        return configuration;
    }

    @Bean
    ProcessEngineFactoryBean processEngine(SpringProcessEngineConfiguration processEngineConfiguration) {
        ProcessEngineFactoryBean factoryBean = new ProcessEngineFactoryBean();
        factoryBean.setProcessEngineConfiguration(processEngineConfiguration);
        return factoryBean;
    }

    @Bean
    RepositoryService repositoryService(ProcessEngine processEngine) {
        return processEngine.getRepositoryService();
    }

    @Bean
    RuntimeService runtimeService(ProcessEngine processEngine) {
        return processEngine.getRuntimeService();
    }
}
