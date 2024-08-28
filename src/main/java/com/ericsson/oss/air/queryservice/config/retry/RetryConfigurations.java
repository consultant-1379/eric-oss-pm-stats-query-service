/*******************************************************************************
 * COPYRIGHT Ericsson 2024
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/

package com.ericsson.oss.air.queryservice.config.retry;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.support.RetryTemplate;

/**
 * Configuration class for the retry related (Postgres) Bean constructions.
 */
@EnableRetry
@Configuration
public class RetryConfigurations {

    /**
     * Bean to create the retry configuration for Postgres.
     * @return retryConfiguration
     */
    @Bean
    @ConfigurationProperties(prefix = "retry")
    @Qualifier("retryConfiguration")
    public Map<String, Map<String, Integer>> retryConfiguration() {
        return new HashMap<>();
    }

    /**
     * RetryTemplate for checking if Postgres is not available on startup.
     * @param retryConfiguration configuration of retry
     * @return RetryTemplate, with backoff-s as configured for Postgres
     */
    @ConditionalOnProperty("datasource.enabled")
    @Bean
    public RetryTemplate postgresConnectionRetryTemplate(
            @Qualifier("retryConfiguration") final Map<String, Map<String, Integer>> retryConfiguration) {
        return RetryTemplateUtil.buildRetryTemplate(
                retryConfiguration.get("postgres").get("backoffPeriod"),
                retryConfiguration.get("postgres").get("maxAttempts")
        );
    }
}
