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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.olingo.server.api.ODataApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.context.annotation.Import;

import com.ericsson.oss.air.queryservice.config.KafkaTestContainersConfiguration;
import com.ericsson.oss.air.queryservice.config.PostgresContainersConfiguration;
import com.ericsson.oss.air.queryservice.model.DatabaseColumn;
import com.ericsson.oss.air.queryservice.model.DatabaseTable;

@SpringBootTest
@Import({KafkaTestContainersConfiguration.class, PostgresContainersConfiguration.class})
public class CacheIntegrationTest {

    private static final String TABLES_CACHE_KEY = "tables";
    private static final String COLUMNS_CACHE_KEY = "columns";
    private static final String SCHEMA_NAME = "public";

    @MockBean
    private ExposureChecker schemaChangeListener;

    @Autowired
    private DatabaseInfoService databaseInfoService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void init() {
        when(schemaChangeListener.isSchemaExposed(any())).thenReturn(true);
        when(schemaChangeListener.isTableExposed(any(), any())).thenReturn(true);

        final Cache tablesCache = cacheManager.getCache(TABLES_CACHE_KEY);
        if (tablesCache != null) {
            tablesCache.invalidate();
        }

        final Cache columnsCache = cacheManager.getCache(COLUMNS_CACHE_KEY);
        if (columnsCache != null) {
            columnsCache.invalidate();
        }
    }

    @Test
    @DisplayName("Test tables cache - one schema")
    public void test_tables_cache_with_one_schema() throws ODataApplicationException {
        final Cache tablesCache = cacheManager.getCache(TABLES_CACHE_KEY);
        assertNull(tablesCache.get(SCHEMA_NAME));

        final Class<List<DatabaseTable>> cacheResultClass = (Class) List.class;
        final List<DatabaseTable> firstInvocationResult = databaseInfoService.getTables(SCHEMA_NAME);

        assertCacheContainsValue(TABLES_CACHE_KEY, SCHEMA_NAME, cacheResultClass, firstInvocationResult);

        final List<DatabaseTable> secondInvocationResult = databaseInfoService.getTables(SCHEMA_NAME);
        assertEquals(firstInvocationResult, secondInvocationResult);
        assertCacheContainsValue(TABLES_CACHE_KEY, SCHEMA_NAME, cacheResultClass, firstInvocationResult);
    }

    @Test
    @DisplayName("Test tables cache - two schemas")
    public void test_tables_cache_with_two_schemas() throws ODataApplicationException {
        final String firstSchemaName = SCHEMA_NAME;
        final String secondSchemaName = "new_schema";

        final Cache tablesCache = cacheManager.getCache(TABLES_CACHE_KEY);
        assertNull(tablesCache.get(firstSchemaName));
        assertNull(tablesCache.get(secondSchemaName));
        final Class<List<DatabaseTable>> cacheResultClass = (Class) List.class;

        final List<DatabaseTable> firstSchemaFirstInvocationResult = databaseInfoService.getTables(firstSchemaName);
        assertCacheContainsValue(TABLES_CACHE_KEY, firstSchemaName, cacheResultClass, firstSchemaFirstInvocationResult);

        final List<DatabaseTable> firstSchemaSecondInvocationResult = databaseInfoService.getTables(firstSchemaName);
        assertEquals(firstSchemaFirstInvocationResult, firstSchemaSecondInvocationResult);
        assertCacheContainsValue(TABLES_CACHE_KEY, firstSchemaName, cacheResultClass, firstSchemaFirstInvocationResult);

        final List<DatabaseTable> secondSchemaFirstInvocationResult = databaseInfoService.getTables(secondSchemaName);
        assertCacheContainsValue(TABLES_CACHE_KEY, secondSchemaName, cacheResultClass, secondSchemaFirstInvocationResult);

        final List<DatabaseTable> secondSchemaSecondInvocationResult = databaseInfoService.getTables(secondSchemaName);
        assertEquals(secondSchemaFirstInvocationResult, secondSchemaSecondInvocationResult);

        assertCacheContainsValue(TABLES_CACHE_KEY, secondSchemaName, cacheResultClass, secondSchemaFirstInvocationResult);
        assertCacheContainsValue(TABLES_CACHE_KEY, firstSchemaName, cacheResultClass, firstSchemaFirstInvocationResult);
    }

    @Test
    @DisplayName("Test columns cache - one schema and one table")
    @Disabled("Column cache has been turned off")
    public void test_columns_cache_with_one_schema_one_table() throws ODataApplicationException {
        final String tableName = "first_table";

        final Cache cache = cacheManager.getCache(COLUMNS_CACHE_KEY);
        assertNull(cache.get(SCHEMA_NAME));
        final Class<List<DatabaseColumn>> cacheResultClass = (Class) List.class;

        final List<DatabaseColumn> firstInvocationResult = databaseInfoService.getColumns(SCHEMA_NAME, tableName);
        assertCacheContainsValue(COLUMNS_CACHE_KEY,
                new SimpleKey(SCHEMA_NAME, tableName), cacheResultClass, firstInvocationResult);

        final List<DatabaseColumn> secondInvocationResult = databaseInfoService.getColumns(SCHEMA_NAME, tableName);
        assertEquals(firstInvocationResult, secondInvocationResult);
        assertCacheContainsValue(COLUMNS_CACHE_KEY,
                new SimpleKey(SCHEMA_NAME, tableName), cacheResultClass, firstInvocationResult);
    }

    @Test
    @DisplayName("Test columns cache - one schema and two tables")
    @Disabled("Column cache has been turned off")
    public void test_columns_cache_with_one_schema_two_tables() throws ODataApplicationException {
        final String firstTableName = "first_table";
        final String secondTableName = "second_table";

        final Cache cache = cacheManager.getCache(COLUMNS_CACHE_KEY);
        assertNull(cache.get(SCHEMA_NAME));
        final Class<List<DatabaseColumn>> cacheResultClass = (Class) List.class;

        final List<DatabaseColumn> firstTableFirstInvocationResult = databaseInfoService.getColumns(SCHEMA_NAME, firstTableName);
        assertCacheContainsValue(COLUMNS_CACHE_KEY,
                new SimpleKey(SCHEMA_NAME, firstTableName), cacheResultClass, firstTableFirstInvocationResult);

        final List<DatabaseColumn> firstTableSecondInvocationResult = databaseInfoService.getColumns(SCHEMA_NAME, firstTableName);
        assertEquals(firstTableFirstInvocationResult, firstTableSecondInvocationResult);
        assertCacheContainsValue(COLUMNS_CACHE_KEY,
                new SimpleKey(SCHEMA_NAME, firstTableName), cacheResultClass, firstTableFirstInvocationResult);

        final List<DatabaseColumn> secondTableFirstInvocationResult = databaseInfoService.getColumns(SCHEMA_NAME, secondTableName);
        assertCacheContainsValue(COLUMNS_CACHE_KEY,
                new SimpleKey(SCHEMA_NAME, secondTableName), cacheResultClass, secondTableFirstInvocationResult);

        final List<DatabaseColumn> secondTableSecondInvocationResult = databaseInfoService.getColumns(SCHEMA_NAME, secondTableName);
        assertEquals(secondTableFirstInvocationResult, secondTableSecondInvocationResult);
        assertCacheContainsValue(COLUMNS_CACHE_KEY,
                new SimpleKey(SCHEMA_NAME, secondTableName), cacheResultClass, secondTableFirstInvocationResult);
    }

    private <T> void assertCacheContainsValue(final String cacheName, final Object cachedValueKey,
                                              final Class<T> cachedValueType, final T expectedCachedValue) {
        final Cache cache = cacheManager.getCache(cacheName);

        assertNotNull(cache);
        final T cachedValue = cache.get(cachedValueKey, cachedValueType);
        assertEquals(expectedCachedValue, cachedValue);
    }
}
