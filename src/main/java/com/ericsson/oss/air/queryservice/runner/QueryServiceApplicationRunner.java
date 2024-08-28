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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Application runner to check for the necessary dependencies.
 */
@Component
@ConditionalOnProperty("datasource.enabled")
public class QueryServiceApplicationRunner implements ApplicationRunner {

    @Autowired
    private DependencyChecker dependencyChecker;

    /**
     * Initializer method, to check, whether the required dependencies could be established.
     * This method is automatically started by Spring, after the initialization of the ApplicationContext is finished.
     *
     * @param args not used, but provided by Spring by default
     */
    @Override
    public void run(final ApplicationArguments args) throws SQLException {
        dependencyChecker.checkDependencies();
    }
}
