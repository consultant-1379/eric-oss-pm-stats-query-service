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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.ericsson.oss.air.queryservice.model.SchemaExposureConfiguration;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import jakarta.annotation.PostConstruct;

/**
 * Class responsible for schema and table exposure operations.
 */
@Service
@ConditionalOnProperty("kafka.enabled")
public class SchemaAndTableExposureChecker implements ExposureChecker {

    private static final Map<String, List<SchemaExposureConfiguration>> CONFIG_MAP = new HashMap<>();

    @Override
    public boolean isSchemaExposed(final String schemaName) {
        return schemaName != null && CONFIG_MAP.containsKey(schemaName);
    }

    @Override
    public boolean isTableExposed(final String schemaName, final String tableName) {
        if (schemaName == null || tableName == null || !CONFIG_MAP.containsKey(schemaName)) {
            return false;
        }
        final List<SchemaExposureConfiguration> schemaExposureConfigurations = CONFIG_MAP.get(schemaName);
        return schemaExposureConfigurations.stream().anyMatch(table -> tableName.equals(table.getName()));
    }

    @Override
    public void hideSchemaExposure(final String schemaName) {
        CONFIG_MAP.remove(schemaName);
    }

    @Override
    public void showSchemaExposure(final String schemaName, final List<SchemaExposureConfiguration> exposureConfigurations) {
        CONFIG_MAP.put(schemaName, exposureConfigurations);
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    @PostConstruct
    private void initMetrics() {
        Gauge.builder(CUSTOM_METRIC_PREFIX + "exposed_schemas_gauge", CONFIG_MAP, Map::size).strongReference(true).register(Metrics.globalRegistry);
    }
}
