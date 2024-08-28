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

package com.ericsson.oss.air.queryservice.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import java.sql.SQLException;
import javax.sql.DataSource;

import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.commons.core.edm.EdmEntitySetImpl;
import org.apache.olingo.commons.core.edm.EdmProviderImpl;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitor;
import org.apache.olingo.server.core.uri.UriInfoImpl;
import org.apache.olingo.server.core.uri.queryoption.CountOptionImpl;
import org.apache.olingo.server.core.uri.queryoption.FilterOptionImpl;
import org.junit.Rule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import com.ericsson.oss.air.queryservice.config.KafkaTestContainersConfiguration;
import com.ericsson.oss.air.queryservice.config.PostgresContainersConfiguration;
import com.ericsson.oss.air.queryservice.olingo.CustomEdmProvider;

import lombok.SneakyThrows;

@SpringBootTest
@TestPropertySource(properties = {"odata-response.max-record-count: 0"})
@Import({KafkaTestContainersConfiguration.class, PostgresContainersConfiguration.class})
public class QueryServiceTest {

    private static final String SET_NAME = "testEntitySet";
    private static final String MISSING_SCHEMA = "missing_schema";

    @Autowired
    private QueryService queryService;

    @SpyBean
    private DataSource dataSource;

    @Mock
    private UriInfo mockUriInfo;

    @Rule //initMocks
    public MockitoRule rule = MockitoJUnit.rule();

    @Test
    @DisplayName("Schema not found")
    void testSchemaNotFound() {
        final EdmEntitySet entitySet = new EdmEntitySetImpl(new EdmProviderImpl(
                new CustomEdmProvider()), null, new CsdlEntitySet().setName(SET_NAME));
        final UriInfoImpl uriInfo = new UriInfoImpl();

        final ODataApplicationException exception = assertThrows(ODataApplicationException.class,
                () -> queryService.getData(entitySet, MISSING_SCHEMA, uriInfo));

        assertEquals("Invalid EntitySet", exception.getMessage());
        assertEquals(HttpStatusCode.NOT_FOUND.getStatusCode(), exception.getStatusCode());
        assertEquals(HttpStatusCode.NOT_FOUND.toString(), exception.getODataErrorCode());
    }

    @Test
    @DisplayName("Handle exception")
    void testHandleException() throws SQLException {
        final EdmEntitySet entitySet = new EdmEntitySetImpl(new EdmProviderImpl(
                new CustomEdmProvider()), null, new CsdlEntitySet().setName(SET_NAME));
        final UriInfoImpl uriInfo = new UriInfoImpl();

        when(dataSource.getConnection()).thenThrow(new RuntimeException("test"));

        final ODataApplicationException exception = assertThrows(ODataApplicationException.class,
                () -> queryService.getData(entitySet, MISSING_SCHEMA, uriInfo));

        assertEquals("Internal Error", exception.getMessage());
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), exception.getStatusCode());
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR.toString(), exception.getODataErrorCode());
    }

    @Test
    @DisplayName("Test with null params")
    void testWithNullParams() {
        final EdmEntitySet entitySet = new EdmEntitySetImpl(new EdmProviderImpl(
                new CustomEdmProvider()), null, new CsdlEntitySet().setName(SET_NAME));
        final UriInfoImpl uriInfo = new UriInfoImpl();

        final ODataApplicationException exception1 = assertThrows(ODataApplicationException.class,
                () -> queryService.getData(null, MISSING_SCHEMA, uriInfo));
        final ODataApplicationException exception2 = assertThrows(ODataApplicationException.class,
                () -> queryService.getData(entitySet, null, uriInfo));
        final ODataApplicationException exception3 = assertThrows(ODataApplicationException.class,
                () -> queryService.getData(entitySet, MISSING_SCHEMA, null));

        assertEquals("Invalid parameters!", exception1.getMessage());
        assertEquals("Invalid parameters!", exception2.getMessage());
        assertEquals("Invalid parameters!", exception3.getMessage());
    }

    @Test
    @DisplayName("Handle exception with SQLException cause")
    void testHandleExceptionWithSqlExceptionCause() throws SQLException {
        final EdmEntitySet entitySet = new EdmEntitySetImpl(new EdmProviderImpl(
                new CustomEdmProvider()), null, new CsdlEntitySet().setName(SET_NAME));
        final UriInfoImpl uriInfo = new UriInfoImpl();

        when(dataSource.getConnection()).thenThrow(new RuntimeException("test", new SQLException("cause")));

        final ODataApplicationException exception = assertThrows(ODataApplicationException.class,
                () -> queryService.getData(entitySet, MISSING_SCHEMA, uriInfo));

        assertEquals("Error while executing query: java.sql.SQLException: cause", exception.getMessage());
        assertEquals(HttpStatusCode.NOT_FOUND.getStatusCode(), exception.getStatusCode());
        assertEquals(HttpStatusCode.NOT_FOUND.toString(), exception.getODataErrorCode());
    }

    @SneakyThrows
    @Test
    @DisplayName("Trigger handleSqlException during a count query")
    void whenGetDataCalledWithCountSqlException_shouldHandleException() {
        mockUriInfo = Mockito.mock(UriInfo.class);
        final CountOptionImpl countOption = new CountOptionImpl();
        countOption.setValue(true);
        final FilterOptionImpl filterOption = new FilterOptionImpl();
        filterOption.setExpression(new Expression() {
            @Override
            public <T> T accept(final ExpressionVisitor<T> expressionVisitor) throws ExpressionVisitException, ODataApplicationException {
                return null;
            }
        });
        Mockito.when(mockUriInfo.getCountOption()).thenReturn(countOption);
        Mockito.when(mockUriInfo.getFilterOption()).thenReturn(filterOption);

        doThrow(SQLException.class).when(dataSource).getConnection();
        final EdmEntitySet entitySet = new EdmEntitySetImpl(new EdmProviderImpl(new CustomEdmProvider()),
                null, new CsdlEntitySet().setName("testEntitySet"));

        assertThatThrownBy(() -> queryService.getData(entitySet, "missing_schema", mockUriInfo))
                .isInstanceOf(ODataApplicationException.class)
                .hasMessageStartingWith("Error while executing query")
                .hasFieldOrPropertyWithValue("statusCode", HttpStatusCode.NOT_FOUND.getStatusCode());
    }

    @SneakyThrows
    @Test
    @DisplayName("Trigger handleSqlException during a non-count query")
    void whenGetDataCalledWithoutCountSqlException_shouldHandleException() {
        mockUriInfo = Mockito.mock(UriInfo.class);
        final CountOptionImpl countOption = new CountOptionImpl();
        countOption.setValue(false);
        final FilterOptionImpl filterOption = new FilterOptionImpl();
        filterOption.setExpression(new Expression() {
            @Override
            public <T> T accept(final ExpressionVisitor<T> expressionVisitor) throws ExpressionVisitException, ODataApplicationException {
                return null;
            }
        });
        Mockito.when(mockUriInfo.getCountOption()).thenReturn(countOption);
        Mockito.when(mockUriInfo.getFilterOption()).thenReturn(filterOption);

        doThrow(SQLException.class).when(dataSource).getConnection();
        final EdmEntitySet entitySet = new EdmEntitySetImpl(new EdmProviderImpl(
                new CustomEdmProvider()), null, new CsdlEntitySet().setName("testEntitySet"));

        assertThatThrownBy(() -> queryService.getData(entitySet, "missing_schema", mockUriInfo))
                .isInstanceOf(ODataApplicationException.class)
                .hasMessageStartingWith("Error while executing query")
                .hasFieldOrPropertyWithValue("statusCode", HttpStatusCode.NOT_FOUND.getStatusCode());
    }
}
