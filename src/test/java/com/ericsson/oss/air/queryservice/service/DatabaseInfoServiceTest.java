/*******************************************************************************
 * COPYRIGHT Ericsson 2023
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

package com.ericsson.oss.air.queryservice.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.junit.Rule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import lombok.SneakyThrows;

@SpringBootTest(classes = DatabaseInfoService.class)
@DisplayName("Database info service")
public class DatabaseInfoServiceTest {

    private static final String TESTSCHEMA = "TST";
    private static final String TESTTABLE = "TBL";
    @Autowired
    private DatabaseInfoService databaseInfoService;

    @Mock
    private Connection mockConnection;
    @Mock
    private DatabaseMetaData mockDatabaseMetadata;
    @Mock
    private ResultSet mockResultSet;

    @MockBean
    private DataSource dataSource;
    @MockBean
    private ExposureChecker exposureChecker;

    @Rule //initMocks
    public MockitoRule rule = MockitoJUnit.rule();

    @SneakyThrows
    @Test
    @DisplayName("Throw ODataApplicationException if an SQL exception happens during getTables")
    void whenGetTablesException_shouldThrowOdataApplicationException() {
        doReturn(true).when(exposureChecker).isSchemaExposed(anyString());
        doThrow(SQLException.class).when(dataSource).getConnection();

        assertThatThrownBy(() -> databaseInfoService.getTables(TESTSCHEMA))
                .isInstanceOf(ODataApplicationException.class)
                .hasMessage("Failed to get database tables")
                .hasFieldOrPropertyWithValue("statusCode", HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @SneakyThrows
    @Test
    @DisplayName("Throw ODataApplicationException if getTables is called with a non-existing schema")
    void whenGetTablesCalledWithNonExistingSchema_shouldThrowOdataApplicationException() {
        doReturn(true).when(exposureChecker).isSchemaExposed(anyString());

        mockResultSet = Mockito.mock(ResultSet.class);
        Mockito.when(mockResultSet.next()).thenReturn(false);
        Mockito.when(mockDatabaseMetadata.getSchemas(eq(null), eq(TESTSCHEMA))).thenReturn(mockResultSet);
        Mockito.when(mockConnection.getMetaData()).thenReturn(mockDatabaseMetadata);
        doReturn(mockConnection).when(dataSource).getConnection();

        assertThatThrownBy(() -> databaseInfoService.getTables(TESTSCHEMA))
                .isInstanceOf(ODataApplicationException.class)
                .hasMessage("Invalid schema")
                .hasFieldOrPropertyWithValue("statusCode", HttpStatusCode.NOT_FOUND.getStatusCode());
    }

    @SneakyThrows
    @Test
    @DisplayName("Throw ODataApplicationException if an SQL exception happens during getColumns")
    void whenGetColumnsException_shouldThrowOdataApplicationException() {
        doThrow(SQLException.class).when(dataSource).getConnection();

        assertThatThrownBy(() -> databaseInfoService.getColumns(TESTSCHEMA, TESTTABLE))
                .isInstanceOf(ODataApplicationException.class)
                .hasMessage("Failed to get database table columns")
                .hasFieldOrPropertyWithValue("statusCode", HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @SneakyThrows
    @Test
    @DisplayName("Throw IllegalStateException if the list of columns is null")
    void whenGetColumnsResultIsNull_shouldThrowIllegalStateException() {
        doReturn(true).when(exposureChecker).isTableExposed(anyString(), anyString());

        Mockito.when(mockDatabaseMetadata.getColumns(anyString(), anyString(), anyString(), anyString())).thenReturn(null);
        Mockito.when(mockConnection.getMetaData()).thenReturn(mockDatabaseMetadata);
        doReturn(mockConnection).when(dataSource).getConnection();

        assertThatThrownBy(() -> databaseInfoService.getColumns(TESTSCHEMA, TESTTABLE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("ResultSet cannot be null");
    }
}
