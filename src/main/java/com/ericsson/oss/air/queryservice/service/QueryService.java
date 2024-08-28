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

import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.sql.DataSource;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.queryoption.CountOption;
import org.hibernate.type.descriptor.jdbc.JdbcTypeJavaClassMappings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;

/**
 * Component responsible for database query operations and entity conversions.
 */
@Service
@ConditionalOnProperty("datasource.enabled")
public class QueryService {
    public static final String CUSTOM_METRIC_PREFIX = "pm_stats_query_service_";

    private static final String SQL_STATE_TABLE_DOES_NOT_EXIST = "42P01";

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryService.class);

    private static Counter recordsCounter = Metrics.counter(CUSTOM_METRIC_PREFIX + "records_counter");

    private static Counter builtCountQueriesCounter = Metrics.counter(CUSTOM_METRIC_PREFIX + "built_count_queries_counter");

    private static Counter executedCountQueriesCounter = Metrics.counter(CUSTOM_METRIC_PREFIX + "executed_count_queries_counter");

    private static Counter builtListingQueriesCounter = Metrics.counter(CUSTOM_METRIC_PREFIX + "built_listing_queries_counter");

    private static Counter executedListingQueriesCounter = Metrics.counter(CUSTOM_METRIC_PREFIX + "executed_listing_queries_counter");

    private static Timer queryExecutionTimer = Metrics.timer(CUSTOM_METRIC_PREFIX + "query_execution_timer");

    private static Timer queryCounterExecutionTimer = Metrics.timer(CUSTOM_METRIC_PREFIX + "count_query_execution_timer");

    private static Timer odataOperationConvertResultListTimer = Metrics.timer(CUSTOM_METRIC_PREFIX + "convert_result_list_timer");

    @Value("${odata-response.max-record-count}")
    private int maxRecordCount;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private QueryBuilderService queryBuilderService;

    /**
     * Reads database records and builds an entity collection from the results.
     * @param edmEntitySet entity set
     * @param schema database schema id
     * @param uriInfo uri information extracted from the OData request uri, contains query options
     * @return a new entity collection containing the converted OData entities
     * @throws ODataApplicationException in case of invalid arguments or any execution errors
     */
    @SuppressWarnings("PMD.CloseResource")
    public EntityCollection getData(final EdmEntitySet edmEntitySet, final String schema, final UriInfo uriInfo)
            throws ODataApplicationException {
        if (isInputParametersEmpty(edmEntitySet, schema, uriInfo)) {
            throw new ODataApplicationException("Invalid parameters!",
                    HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH, HttpStatusCode.BAD_REQUEST.toString());
        }

        final EntityCollection entityCollection = new EntityCollection();
        final CountOption countOption = uriInfo.getCountOption();
        Integer count = 0;

        final Timer.Sample sample = Timer.start();
        if (shouldRunCountQuery(uriInfo)) {
            final String countQuery = queryBuilderService.buildCountQuery(schema, edmEntitySet.getName(), uriInfo);
            builtCountQueriesCounter.increment();

            try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(countQuery)) {
                LOGGER.debug("Executing count query");
                try (ResultSet resultSet = statement.executeQuery()) {
                    executedCountQueriesCounter.increment();
                    resultSet.next();
                    count = resultSet.getInt(1);

                    if (isThereCountOption(countOption)) {
                        entityCollection.setCount(count);
                    }
                }
            } catch (final Exception e) {
                handleException(e);
            }
        }

        sample.stop(queryCounterExecutionTimer);

        if (shouldReturnBadRequest(uriInfo, count)) {
            throw new ODataApplicationException(
                    String.format("The size of the result set has reached its upper bound: "
                            + "%d, please use $top with a value less than or equal to the upper bound.", maxRecordCount),
                    HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH, HttpStatusCode.BAD_REQUEST.toString());
        }

        final String buildQuery = queryBuilderService.buildQuery(schema, edmEntitySet.getName(), uriInfo);
        builtListingQueriesCounter.increment();

        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(buildQuery);
                ResultSet resultSet = Objects.requireNonNull(queryExecutionTimer.recordCallable(statement::executeQuery))) {
            LOGGER.debug("Executing build query, maximum page size: {}", maxRecordCount);
            executedListingQueriesCounter.increment();
            final List<Entity> odataEntities =
                    odataOperationConvertResultListTimer.recordCallable(() -> convertResultToODataEntities(resultSet));
            Objects.requireNonNull(odataEntities, "OData entities must not be null.");
            recordsCounter.increment(odataEntities.size());
            LOGGER.debug("Query returned {} results", odataEntities.size());
            entityCollection.getEntities().addAll(odataEntities);
        } catch (final Exception e) {
            handleException(e);
        }
        return entityCollection;
    }

    private boolean isInputParametersEmpty(final EdmEntitySet edmEntitySet, final String schema, final UriInfo uriInfo) {
        return edmEntitySet == null || schema == null || uriInfo == null;
    }

    private boolean isThereCountOption(final CountOption countOption) {
        return countOption != null && countOption.getValue();
    }

    private boolean shouldRunCountQuery(final UriInfo uriInfo) {
        final CountOption countOption = uriInfo.getCountOption();
        return isThereCountOption(countOption)
                || !(uriInfo.getTopOption() != null && uriInfo.getTopOption().getValue() <= maxRecordCount);
    }

    private boolean shouldReturnBadRequest(final UriInfo uriInfo, final Integer count) {
        // !(top <= max) & !(count - skip <= max) & (count > max)
        return !(uriInfo.getTopOption() != null && uriInfo.getTopOption().getValue() <= maxRecordCount)
                && !(uriInfo.getSkipOption() != null && count - uriInfo.getSkipOption().getValue() <= maxRecordCount)
                && count > maxRecordCount;
    }

    /**
     * Logs and rethrows exceptions.
     * @param e the original Exception
     * @throws ODataApplicationException when this method is invoked
     */
    private void handleException(final Exception e) throws ODataApplicationException {
        LOGGER.error(e.getMessage(), e);
        if (e instanceof SQLException) {
            final SQLException sqlEx = (SQLException) e;
            if (SQL_STATE_TABLE_DOES_NOT_EXIST.equals(sqlEx.getSQLState())) {
                throw new ODataApplicationException("Invalid EntitySet",
                        HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH, HttpStatusCode.NOT_FOUND.toString());
            } else {
                throw new ODataApplicationException("Error while executing query: " + e,
                        HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH, HttpStatusCode.NOT_FOUND.toString());
            }
        } else if (e instanceof ODataApplicationException) {
            throw new ODataApplicationException(
                    String.format("The size of the result set has reached its upper bound: %d, "
                            + "please use $top with a value less than or equal to the upper bound.", maxRecordCount),
                    HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH, HttpStatusCode.BAD_REQUEST.toString());
        } else if (e instanceof Exception) {
            if (e.getCause() instanceof SQLException) {
                handleException((SQLException) e.getCause());
            } else {
                LOGGER.error(e.getMessage(), e);
                throw new ODataApplicationException("Internal Error",
                        HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH, e, HttpStatusCode.INTERNAL_SERVER_ERROR.toString());
            }
        }
    }

    /**
     * Converts SQL types to Java types based on JDBC type mappings.
     * @param typeCode constant used to identify generic SQL types
     */
    private Class<?> convertSqlTypeToJavaType(final int typeCode) {
        if (typeCode == Types.SQLXML) {
            return String.class;
        }
        return JdbcTypeJavaClassMappings.INSTANCE.determineJavaClassForJdbcTypeCode(typeCode);
    }

    /**
     * Converts database result set elements to entities.
     * @param resultSet database result set
     * @return list of OData entities
     * @throws ODataApplicationException in case of a PostgreSql array conversion error or an unsupported data type
     * @throws SQLException if a database access error / JDBC error occurs or the method is called on a closed result set
     */
    private List<Entity> convertResultToODataEntities(final ResultSet resultSet) throws ODataApplicationException, SQLException {
        final List<Entity> entities = new ArrayList<>();
        final ResultSetMetaData resultMetadata = resultSet.getMetaData();
        final int columnCount = resultMetadata.getColumnCount();

        while (resultSet.next()) {
            final Entity entity = new Entity();
            for (int i = 1; i <= columnCount; i++) {
                final String name = resultMetadata.getColumnName(i);
                final int typeCode = resultMetadata.getColumnType(i);
                final JDBCType jdbcType = JDBCType.valueOf(typeCode);
                final String typeName = resultMetadata.getColumnTypeName(i);
                if (jdbcType == JDBCType.ARRAY) {
                    final Object resultSetProtoValue = JdbcUtils.getResultSetValue(resultSet, i, convertSqlTypeToJavaType(typeCode));
                    if (resultSetProtoValue instanceof org.postgresql.jdbc.PgArray) {
                        entity.addProperty(new Property(typeName,
                                name,
                                ValueType.COLLECTION_PRIMITIVE,
                                EdmTypeConverter.convertPgArrayToTypedList(
                                        ((org.postgresql.jdbc.PgArray) resultSetProtoValue).getArray(), typeName)
                        ));
                    }
                } else {
                    entity.addProperty(new Property(typeName,
                            name,
                            ValueType.PRIMITIVE,
                            JdbcUtils.getResultSetValue(resultSet, i, convertSqlTypeToJavaType(typeCode))));
                }
            }
            entities.add(entity);
        }
        return entities;
    }
}
