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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ericsson.oss.air.queryservice.model.SchemaExposureConfiguration;

@DisplayName("Exposure checker without kafka")
public class ExposureCheckerWithoutKafkaTest {

    private ExposureCheckerWithoutKafka exposureChecker;

    @BeforeEach
    void init() {
        exposureChecker = new ExposureCheckerWithoutKafka();
    }

    @Test
    @DisplayName("Schema and table should be exposed by default")
    void testSchemaAndTableExposure() {
        assertTrue(exposureChecker.isSchemaExposed("schemaName"));
        assertTrue(exposureChecker.isTableExposed("schemaName", "tableName"));
    }

    @Test
    void whenOrphanedMethodsCalled_shouldNotThrow() {
        final String schemaName = "";
        final java.util.List<SchemaExposureConfiguration> exposureConfigurations = new ArrayList<>();

        assertDoesNotThrow(() -> exposureChecker.hideSchemaExposure(schemaName));
        assertDoesNotThrow(() -> exposureChecker.showSchemaExposure(schemaName, exposureConfigurations));
    }
}
