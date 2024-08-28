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

import java.sql.JDBCType;
import java.util.ArrayList;
import java.util.List;

import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.server.api.ODataApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.ericsson.oss.air.queryservice.model.DatabaseColumn;
import com.ericsson.oss.air.queryservice.model.DatabaseTable;
import com.ericsson.oss.air.queryservice.olingo.CustomEdmProvider;

import io.micrometer.core.annotation.Timed;

/**
 * Component responsible for creating OData {@link CustomEdmProvider CSDL} objects from database metadata.
 */
@Component
@ConditionalOnProperty("datasource.enabled")
public class SchemaTranslatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaTranslatorService.class);

    @Autowired
    private DatabaseInfoService databaseInfoService;

    /**
     * Creates a new {@link CustomEdmProvider CSDL} schema containing entity types.
     * @param schemaName database schema id
     * @return {@link CustomEdmProvider CSDL} schema
     */
    @Timed(CUSTOM_METRIC_PREFIX + "get_schema_timer")
    public CsdlSchema getSchema(final String schemaName) throws ODataApplicationException {
        LOGGER.debug("Getting schema from schema translator");
        final CsdlSchema schema = new CsdlSchema();
        schema.setNamespace(schemaName);

        final List<CsdlEntityType> entityTypes = new ArrayList<>();

        final List<DatabaseTable> tables = databaseInfoService.getTables(schemaName);
        for (final DatabaseTable table : tables) {
            entityTypes.add(getTable(schemaName, table.getName()));
        }
        schema.setEntityTypes(entityTypes);

        return schema;
    }

    /**
     * Creates an {@link CustomEdmProvider CSDL} entity type based on a database element (table) of a specific schema.
     * @param schemaName database schema id
     * @param tableName database table name
     * @return OData {@link CustomEdmProvider CSDL} entity type
     */
    @Timed(CUSTOM_METRIC_PREFIX + "get_table_timer")
    public CsdlEntityType getTable(final String schemaName, final String tableName) throws ODataApplicationException {
        LOGGER.debug("Getting table from schema translator");
        final CsdlEntityType entityType = new CsdlEntityType();
        entityType.setName(tableName);

        final List<CsdlProperty> properties = new ArrayList<>();
        final List<DatabaseColumn> columns = databaseInfoService.getColumns(schemaName, tableName);
        for (final DatabaseColumn column : columns) {
            final JDBCType jdbcType = JDBCType.valueOf(column.getType());
            final CsdlProperty property;
            if (jdbcType == JDBCType.ARRAY) {
                property = new CsdlProperty().setName(column.getName())
                        .setType(EdmTypeConverter.convertArrayTypeToEdmType(column.getUdtName()).getFullQualifiedName())
                        .setNullable(true)
                        .setCollection(true);
            } else {
                property = new CsdlProperty().setName(column.getName())
                        .setType(EdmTypeConverter.convertJdbcTypeToEdmType(jdbcType).getFullQualifiedName());
            }
            properties.add(property);
        }

        entityType.setProperties(properties);
        return entityType;
    }
}