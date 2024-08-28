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

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.ericsson.oss.air.queryservice.model.SchemaExposureConfiguration;

/**
 * Class responsible for schema and table exposure operations having all exposure states fixed.
 */
@Service
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "false")
public class ExposureCheckerWithoutKafka implements ExposureChecker {

    /**
     * Determines whether a schema is exposed or not.
     * @param schemaName schema name
     * @return true always
     */
    @Override
    public boolean isSchemaExposed(final String schemaName) {
        return true;
    }

    /**
     * Determines whether a table of a schema is exposed or not.
     * @param schemaName schema name
     * @param tableName table name
     * @return true always
     */
    @Override
    public boolean isTableExposed(final String schemaName, final String tableName) {
        return true;
    }

    /**
     * This method has no effect intentionally.
     * @param schemaName schema name
     */
    @Override
    public void hideSchemaExposure(final String schemaName) {
        // intentionally left empty
    }

    /**
     * This method has no effect intentionally.
     * @param schemaName schema name
     * @param exposureConfigurations list of schema element POJOs
     */
    @Override
    public void showSchemaExposure(final String schemaName, final List<SchemaExposureConfiguration> exposureConfigurations) {
        // intentionally left empty
    }
}
