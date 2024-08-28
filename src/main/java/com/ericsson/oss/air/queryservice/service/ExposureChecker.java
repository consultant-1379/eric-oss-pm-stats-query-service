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

import java.util.List;

import com.ericsson.oss.air.queryservice.model.SchemaExposureConfiguration;

/**
 *  Interface responsible for schema and table exposure operations.
 */
public interface ExposureChecker {

    /**
     * Determines whether a schema is exposed or not.
     * @param schemaName schema name
     * @return true if the schema is exposed, false otherwise
     */
    boolean isSchemaExposed(String schemaName);

    /**
     * Determines whether a table of a schema is exposed or not.
     * @param schemaName schema name
     * @param tableName table name
     * @return true if the table is exposed, false otherwise
     */
    boolean isTableExposed(String schemaName, String tableName);

    /**
     * Removes the exposure of a schema.
     * @param schemaName schema name
     */
    void hideSchemaExposure(String schemaName);

    /**
     * Sets the exposure of a schema and its elements.
     * @param schemaName schema name
     * @param exposureConfigurations list of schema element POJOs
     */
    void showSchemaExposure(String schemaName, List<SchemaExposureConfiguration> exposureConfigurations);
}