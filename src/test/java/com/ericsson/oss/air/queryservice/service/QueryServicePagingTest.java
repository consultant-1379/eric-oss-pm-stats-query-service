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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import javax.sql.DataSource;

import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.commons.core.edm.EdmEntitySetImpl;
import org.apache.olingo.commons.core.edm.EdmProviderImpl;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.core.uri.UriInfoImpl;
import org.junit.Rule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;

import com.ericsson.oss.air.queryservice.config.KafkaTestContainersConfiguration;
import com.ericsson.oss.air.queryservice.config.PostgresContainersConfiguration;
import com.ericsson.oss.air.queryservice.olingo.CustomEdmProvider;

@SpringBootTest
@Import({KafkaTestContainersConfiguration.class, PostgresContainersConfiguration.class})
public class QueryServicePagingTest {

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
    @DisplayName("Schema not found (max-record-count set)")
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
    @DisplayName("Handle exception (max-record-count set)")
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
    @DisplayName("Handle exception with SQLException cause (max-record-count set)")
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
}
