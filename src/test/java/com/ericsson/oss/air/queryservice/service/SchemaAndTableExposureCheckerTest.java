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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ericsson.oss.air.queryservice.model.SchemaExposureConfiguration;

@DisplayName("Schema and table exposure checker")
public class SchemaAndTableExposureCheckerTest {

    private static final String SCHEMA_NAME = "schemaName";

    private SchemaAndTableExposureChecker schemaAndTableExposureChecker;

    @BeforeEach
    void init() {
        schemaAndTableExposureChecker = new SchemaAndTableExposureChecker();
    }

    @Test
    @DisplayName("Expose table")
    void testExposeTable() {
        assertFalse(schemaAndTableExposureChecker.isSchemaExposed(SCHEMA_NAME));
        assertFalse(schemaAndTableExposureChecker.isTableExposed(SCHEMA_NAME, "tableName"));

        final SchemaExposureConfiguration exposureConfiguration = new SchemaExposureConfiguration();
        exposureConfiguration.setKind(SchemaExposureConfiguration.Type.TABLE);
        exposureConfiguration.setName("tableName");
        schemaAndTableExposureChecker.showSchemaExposure(SCHEMA_NAME, Collections.singletonList(exposureConfiguration));

        assertTrue(schemaAndTableExposureChecker.isSchemaExposed(SCHEMA_NAME));
        assertTrue(schemaAndTableExposureChecker.isTableExposed(SCHEMA_NAME, "tableName"));
    }
}
