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

import static com.ericsson.oss.air.queryservice.service.QueryService.CUSTOM_METRIC_PREFIX;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.sql.DataSource;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.ericsson.oss.air.queryservice.model.DatabaseColumn;
import com.ericsson.oss.air.queryservice.model.DatabaseTable;

import io.micrometer.core.annotation.Timed;

/**
 * Component responsible for PostgreSql metadata queries.
 */
@Service
@ConditionalOnProperty("datasource.enabled")
public class DatabaseInfoService {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ExposureChecker exposureChecker;

    /**
     * Reads metadata about tables and views belonging to a specific database schema (filtered by exposure checks).
     * @param schema database schema id
     * @return list of {@link DatabaseTable} POJOs
     * @throws ODataApplicationException in case of an invalid schema or database errors
     */
    @Cacheable("tables")
    @Timed(CUSTOM_METRIC_PREFIX + "list_tables_timer")
    public List<DatabaseTable> getTables(final String schema) throws ODataApplicationException {
        if (!exposureChecker.isSchemaExposed(schema)) {
            throw new ODataApplicationException("Invalid schema",
                    HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH, HttpStatusCode.NOT_FOUND.toString());
        }
        final List<DatabaseTable> tables = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                ResultSet availableSchemas = connection.getMetaData().getSchemas(null, schema);
                ResultSet resultSet = connection.getMetaData().getTables(null, schema, "%",
                     new String[]{"TABLE", "VIEW", "PARTITIONED TABLE"})) {
            boolean schemaExists = false;
            while (availableSchemas.next()) {
                if (availableSchemas.getString("TABLE_SCHEM").equals(schema)) {
                    schemaExists = true;
                    break;
                }
            }
            if (!schemaExists) {
                throw new ODataApplicationException("Invalid schema",
                        HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH, HttpStatusCode.NOT_FOUND.toString());
            }
            while (resultSet.next()) {
                final DatabaseTable table = new DatabaseTable();
                table.setSchema(schema);
                table.setName(resultSet.getString("TABLE_NAME"));
                table.setType(DatabaseTable.TableType.getByValue(resultSet.getString("TABLE_TYPE")));
                if (!exposureChecker.isTableExposed(schema, table.getName())) {
                    continue;
                }
                tables.add(table);
            }
        } catch (final SQLException e) {
            throw new ODataApplicationException("Failed to get database tables",
                    HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH, e);
        }

        return tables;
    }

    /**
     * The method triggers a cache evict operation.
     * @param schema database schema id
     */
    @CacheEvict(value = {"tables"}, allEntries = true)
    public void clearTablesCache(final String schema) {
        // No need to do anything else, the CacheEvict annotation clears the cache.
    }

    /**
     * Reads metadata about columns belonging to a specific database table of a schema (filtered by exposure checks).
     * @param schema database schema id
     * @param table database table name
     * @return list of {@link DatabaseColumn} POJOs
     * @throws ODataApplicationException in case of an invalid schema, unexposed table or database errors
     */
    // @Cacheable("columns")
    @Timed(CUSTOM_METRIC_PREFIX + "list_columns_timer")
    public List<DatabaseColumn> getColumns(final String schema, final String table) throws ODataApplicationException {
        final List<DatabaseColumn> columns = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
                ResultSet resultSet = connection.getMetaData().getColumns(null, schema, table, "%")) {
            if (!exposureChecker.isTableExposed(schema, table)) {
                throw new ODataApplicationException("Invalid EntitySet",
                        HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH, HttpStatusCode.NOT_FOUND.toString());
            }
            if (resultSet == null) {
                throw new IllegalStateException("ResultSet cannot be null");
            }
            while (resultSet.next()) {
                final DatabaseColumn column = new DatabaseColumn();
                column.setName(resultSet.getString("COLUMN_NAME"));
                column.setType(resultSet.getInt("DATA_TYPE"));
                column.setUdtName(resultSet.getString("TYPE_NAME"));
                columns.add(column);
            }
        } catch (final SQLException e) {
            throw new ODataApplicationException("Failed to get database table columns",
                    HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH, e);
        }

        return columns;
    }
}
