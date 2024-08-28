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

package com.ericsson.oss.air.queryservice.util;

import static com.ericsson.oss.air.queryservice.service.QueryService.CUSTOM_METRIC_PREFIX;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;

/**
 * Initialize the metrics' values
 */
@Component
public class InitializeMetrics {
    private static final String CLASS = "class";
    private static final String EXCEPTION = "exception";
    private static final String NONE = "none";
    private static final String METHOD = "method";

    @Autowired
    MeterRegistry meterRegistry;

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    @PostConstruct
    private void initializeMetrics() {
        Timer.builder(CUSTOM_METRIC_PREFIX + "build_count_query_timer")
                .description("Time of building a count query measured in seconds")
                .tags(CLASS, "com.ericsson.oss.air.queryservice.service.QueryBuilderService",
                        EXCEPTION, NONE, METHOD, "buildCountQuery")
                .register(meterRegistry);
        Timer.builder(CUSTOM_METRIC_PREFIX + "build_listing_query_timer")
                .description("Time of building a listing query measured in seconds")
                .tags(CLASS, "com.ericsson.oss.air.queryservice.service.QueryBuilderService",
                        EXCEPTION, NONE, METHOD, "buildQuery")
                .register(meterRegistry);
        Timer.builder(CUSTOM_METRIC_PREFIX + "get_schema_timer")
                .description("Time of retrieving a schema measured in seconds")
                .tags(CLASS, "com.ericsson.oss.air.queryservice.service.SchemaTranslatorService", EXCEPTION, NONE,
                        METHOD, "getSchema")
                .register(meterRegistry);
        Timer.builder(CUSTOM_METRIC_PREFIX + "get_table_timer")
                .description("Time of retrieving a table measured in seconds")
                .tags(CLASS, "com.ericsson.oss.air.queryservice.service.SchemaTranslatorService",
                        EXCEPTION, NONE, METHOD, "getTable")
                .register(meterRegistry);
        Timer.builder(CUSTOM_METRIC_PREFIX + "list_columns_timer")
                .description("Time of listing columns measured in seconds")
                .tags(CLASS, "com.ericsson.oss.air.queryservice.service.DatabaseInfoService",
                        EXCEPTION, NONE, METHOD, "getColumns")
                .register(meterRegistry);
        Timer.builder(CUSTOM_METRIC_PREFIX + "list_tables_timer")
                .description("Time of listing tables measured in seconds")
                .tags(CLASS, "com.ericsson.oss.air.queryservice.service.DatabaseInfoService",
                        EXCEPTION, NONE, METHOD, "getTables")
                .register(meterRegistry);
        Counter.builder(CUSTOM_METRIC_PREFIX + "served_queries_counter")
                .register(meterRegistry);
    }
}
