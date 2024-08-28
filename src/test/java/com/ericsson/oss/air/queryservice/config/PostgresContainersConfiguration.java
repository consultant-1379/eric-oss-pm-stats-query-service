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

package com.ericsson.oss.air.queryservice.config;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

import jakarta.annotation.PostConstruct;

@TestConfiguration
public class PostgresContainersConfiguration {

    private PostgreSQLContainer container;

    @PostConstruct
    private void init() {
        container = new PostgreSQLContainer("postgres:13.5")
                .withDatabaseName("test")
                .withUsername("sa");
        container
                .withInitScript("db_init.sql");
        container.start();
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Bean
    public PostgreSQLContainer postgreSQLContainer() {
        return container;
    }

    @Bean
    public DataSource dataSource() {
        final DataSourceBuilder builder = DataSourceBuilder.create();
        builder.driverClassName(container.getDriverClassName());
        builder.url(container.getJdbcUrl());
        builder.username(container.getUsername());
        builder.password(container.getPassword());
        return builder.build();
    }
}
