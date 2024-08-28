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

package com.ericsson.oss.air.queryservice.runner;

import java.sql.SQLException;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

/**
 * Component for checking the service's dependencies.
 */
@Component
@ConditionalOnProperty("datasource.enabled")
public class DependencyChecker {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RetryTemplate postgresConnectionRetryTemplate;

    /**
     * Checks if a connection to Postgres could be established.
     */
    public final RetryCallback checkPostgresConnection = arg -> {
        try {
            dataSource.getConnection();
        } catch (final SQLException e) {
            throw new IllegalStateException("Connecting to database was interrupted by an exception", e);
        }
        return null;
    };

    /**
     * Collects all the dependency checkers, and validate, whether they are available on application startup.
     */
    public void checkDependencies() {
        postgresConnectionRetryTemplate.execute(checkPostgresConnection, context -> {
            throw new IllegalStateException("Could not initialize Postgres connection: ", context.getLastThrowable());
        });
    }
}
