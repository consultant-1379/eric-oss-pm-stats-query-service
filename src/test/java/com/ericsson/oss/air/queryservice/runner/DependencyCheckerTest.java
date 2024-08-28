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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;
import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.retry.support.RetryTemplate;

import lombok.SneakyThrows;

@SpringBootTest(classes = DependencyChecker.class)
@MockBeans(@MockBean(RetryTemplate.class))
class DependencyCheckerTest {
    @MockBean
    private DataSource dataSourceMock;

    @Autowired
    private DependencyChecker dependencyChecker;

    @BeforeEach
    void setup() {
        clearInvocations(dataSourceMock);
    }

    @Test
    @SneakyThrows
    void whenEveryDependencyHealthy_shouldApplicationStart() {
        assertThatCode(() -> dependencyChecker.checkDependencies()).doesNotThrowAnyException();
    }

    @Test
    @SneakyThrows
    void whenDatasourceConnectionAvailable_shouldCheckPostgresConnectionComplete() {
        dependencyChecker.checkPostgresConnection.doWithRetry(null);
        verify(dataSourceMock).getConnection();
    }

    @Test
    @SneakyThrows
    void whenDatasourceConnectionNotAvailable_shouldIllegalStateExceptionBeThrown() {
        doThrow(SQLException.class).when(dataSourceMock).getConnection();

        assertThatThrownBy(() -> dependencyChecker.checkPostgresConnection.doWithRetry(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Connecting to database was interrupted by an exception");

        verify(dataSourceMock).getConnection();
    }
}