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

package com.ericsson.oss.air.queryservice.controller.example;

import static java.util.Map.entry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.ericsson.oss.air.queryservice.model.DatabaseTable;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public final class TestDataEntityMapping {
    public static final String SCHEMA_PUBLIC = "public";
    public static final String SCHEMA_NEW = "new_schema";
    public static final String SCHEMA_PARTITIONS = "partitest";
    public static final String SCHEMA_MIXED = "mixed_schema";

    @SuppressWarnings("checkstyle:MultipleStringLiterals")
    private static final Map<String, Map<String, Map<String, String>>> MAPPINGS = Map.ofEntries(
            entry(SCHEMA_PUBLIC, Map.ofEntries(
                    entry("first_table", Map.ofEntries(
                            entry("date_time", "Edm.DateTimeOffset"),
                            entry("first_kpi", "Edm.Int16"),
                            entry("second_kpi", "Edm.Int32"),
                            entry("third_kpi", "Edm.Int64"),
                            entry("fourth_kpi", "Edm.Double"),
                            entry("fifth_kpi", "Edm.Double"),
                            entry("sixth_kpi", "Edm.Single"),
                            entry("seventh_kpi", "Edm.Double")
                    )),
                    entry("second_table", Map.ofEntries(
                            entry("date_time", "Edm.DateTimeOffset"),
                            entry("first_kpi", "Edm.Int64"),
                            entry("second_kpi", "Edm.Int32"),
                            entry("third_kpi", "Edm.Double"),
                            entry("fourth_kpi", "Edm.String")
                    )),
                    entry("third_table", Map.ofEntries(
                            entry("date_time", "Edm.DateTimeOffset"),
                            entry("first_kpi", "Edm.Int64"),
                            entry("second_kpi", "Edm.Int32"),
                            entry("third_kpi", "Edm.Double")
                    )),
                    entry("first_view", Map.ofEntries(
                            entry("date_time", "Edm.DateTimeOffset"),
                            entry("second_kpi", "Edm.Int32"),
                            entry("fourth_kpi", "Edm.Double"),
                            entry("sixth_kpi", "Edm.Single")
                    )),
                    entry("second_view", Map.ofEntries(
                            entry("date_time", "Edm.DateTimeOffset"),
                            entry("first_kpi", "Edm.Int64"),
                            entry("third_kpi", "Edm.Double")
                    ))
            )),
            entry(SCHEMA_NEW, Map.ofEntries(
                    entry("new_table", Map.ofEntries(
                            entry("date_time", "Edm.DateTimeOffset"),
                            entry("first_kpi", "Edm.Int64"),
                            entry("second_kpi", "Edm.Int32"),
                            entry("third_kpi", "Edm.Double"),
                            entry("fourth_kpi", "Edm.String")
                    )),
                    entry("table_all_types", Map.ofEntries(
                            entry("c_1", "Edm.Int64"),
                            entry("c_2", "Edm.Int64"),
                            entry("c_3", "Edm.Boolean"),
                            entry("c_4", "Edm.String"),
                            entry("c_5", "Edm.Boolean"),
                            entry("c_6", "Edm.String"),
                            entry("c_7", "Edm.Binary"),
                            entry("c_8", "Edm.String"),
                            entry("c_9", "Edm.String"),
                            entry("c_10", "Edm.String"),
                            entry("c_11", "Edm.String"),
                            entry("c_12", "Edm.Date"),
                            entry("c_13", "Edm.Double"),
                            entry("c_14", "Edm.String"),
                            entry("c_15", "Edm.Int32"),
                            entry("c_16", "Edm.String"),
                            entry("c_17", "Edm.String"),
                            entry("c_18", "Edm.String"),
                            entry("c_19", "Edm.String"),
                            entry("c_20", "Edm.String"),
                            entry("c_21", "Edm.String"),
                            entry("c_22", "Edm.String"),
                            entry("c_23", "Edm.Double"),
                            entry("c_24", "Edm.Double"),
                            entry("c_25", "Edm.String"),
                            entry("c_26", "Edm.String"),
                            entry("c_27", "Edm.String"),
                            entry("c_28", "Edm.String"),
                            entry("c_29", "Edm.String"),
                            entry("c_30", "Edm.Single"),
                            entry("c_31", "Edm.Int16"),
                            entry("c_32", "Edm.Int16"),
                            entry("c_33", "Edm.Int32"),
                            entry("c_34", "Edm.String"),
                            entry("c_35", "Edm.TimeOfDay"),
                            entry("c_36", "Edm.TimeOfDay"),
                            entry("c_37", "Edm.DateTimeOffset"),
                            entry("c_38", "Edm.DateTimeOffset"),
                            entry("c_39", "Edm.String"),
                            entry("c_40", "Edm.String"),
                            entry("c_41", "Edm.String"),
                            entry("c_42", "Edm.String"),
                            entry("c_43", "Edm.String")
                    )),
                    entry("table_all_array_types", Map.ofEntries(
                            entry("c_1", "Collection(Edm.Int64)"),
                            entry("c_3", "Collection(Edm.Boolean)"),
                            entry("c_4", "Collection(Edm.String)"),
                            entry("c_5", "Collection(Edm.Boolean)"),
                            entry("c_6", "Collection(Edm.String)"),
                            entry("c_8", "Collection(Edm.String)"),
                            entry("c_9", "Collection(Edm.String)"),
                            entry("c_10", "Collection(Edm.String)"),
                            entry("c_11", "Collection(Edm.String)"),
                            entry("c_12", "Collection(Edm.Date)"),
                            entry("c_13", "Collection(Edm.Double)"),
                            entry("c_14", "Collection(Edm.String)"),
                            entry("c_15", "Collection(Edm.Int32)"),
                            entry("c_16", "Collection(Edm.String)"),
                            entry("c_17", "Collection(Edm.String)"),
                            entry("c_18", "Collection(Edm.String)"),
                            entry("c_19", "Collection(Edm.String)"),
                            entry("c_20", "Collection(Edm.String)"),
                            entry("c_21", "Collection(Edm.String)"),
                            entry("c_22", "Collection(Edm.String)"),
                            entry("c_24", "Collection(Edm.Double)"),
                            entry("c_25", "Collection(Edm.String)"),
                            entry("c_26", "Collection(Edm.String)"),
                            entry("c_28", "Collection(Edm.String)"),
                            entry("c_29", "Collection(Edm.String)"),
                            entry("c_30", "Collection(Edm.Single)"),
                            entry("c_31", "Collection(Edm.Int16)"),
                            entry("c_34", "Collection(Edm.String)"),
                            entry("c_35", "Collection(Edm.TimeOfDay)"),
                            entry("c_36", "Collection(Edm.TimeOfDay)"),
                            entry("c_37", "Collection(Edm.DateTimeOffset)"),
                            entry("c_38", "Collection(Edm.DateTimeOffset)"),
                            entry("c_39", "Collection(Edm.String)"),
                            entry("c_40", "Collection(Edm.String)"),
                            entry("c_42", "Collection(Edm.String)"),
                            entry("c_43", "Collection(Edm.String)")
                    ))
            )),
            entry(SCHEMA_PARTITIONS, Map.ofEntries(
                    entry("kpi_rolling_aggregation_1440", Map.ofEntries(
                            entry("agg_column_0", "Edm.Int32"),
                            entry("aggregation_begin_time", "Edm.DateTimeOffset"),
                            entry("aggregation_end_time", "Edm.DateTimeOffset"),
                            entry("rolling_sum_integer_1440", "Edm.Int32"),
                            entry("rolling_max_integer_1440", "Edm.Int32")
                    )),
                    entry("kpi_rolling_aggregation_1440_p_2023_01_01", Map.ofEntries(
                            entry("agg_column_0", "Edm.Int32"),
                            entry("aggregation_begin_time", "Edm.DateTimeOffset"),
                            entry("aggregation_end_time", "Edm.DateTimeOffset"),
                            entry("rolling_sum_integer_1440", "Edm.Int32"),
                            entry("rolling_max_integer_1440", "Edm.Int32")
                    )),
                    entry("kpi_rolling_aggregation_1440_p_2023_01_02", Map.ofEntries(
                            entry("agg_column_0", "Edm.Int32"),
                            entry("aggregation_begin_time", "Edm.DateTimeOffset"),
                            entry("aggregation_end_time", "Edm.DateTimeOffset"),
                            entry("rolling_sum_integer_1440", "Edm.Int32"),
                            entry("rolling_max_integer_1440", "Edm.Int32")
                    )),
                    entry("kpi_rolling_aggregation_1440_p_2023_01_03", Map.ofEntries(
                            entry("agg_column_0", "Edm.Int32"),
                            entry("aggregation_begin_time", "Edm.DateTimeOffset"),
                            entry("aggregation_end_time", "Edm.DateTimeOffset"),
                            entry("rolling_sum_integer_1440", "Edm.Int32"),
                            entry("rolling_max_integer_1440", "Edm.Int32")
                    )),
                    entry("kpi_simple", Map.ofEntries(
                            entry("agg_column_0", "Edm.Int32"),
                            entry("agg_column_1", "Edm.Int32"),
                            entry("dim_1", "Edm.String"),
                            entry("dim_2", "Edm.String"),
                            entry("rolling_sum", "Edm.Int32")
                    )),
                    entry("sample_regional", Map.ofEntries(
                            entry("towercell", "Edm.String"),
                            entry("region", "Edm.String"),
                            entry("signal", "Edm.Single")
                    )),
                    entry("sample_regional_west", Map.ofEntries(
                            entry("towercell", "Edm.String"),
                            entry("region", "Edm.String"),
                            entry("signal", "Edm.Single")
                    )),
                    entry("sample_regional_east", Map.ofEntries(
                            entry("towercell", "Edm.String"),
                            entry("region", "Edm.String"),
                            entry("signal", "Edm.Single")
                    )),
                    entry("sample_regional_north", Map.ofEntries(
                            entry("towercell", "Edm.String"),
                            entry("region", "Edm.String"),
                            entry("signal", "Edm.Single")
                    )),
                    entry("sample_regional_south", Map.ofEntries(
                            entry("towercell", "Edm.String"),
                            entry("region", "Edm.String"),
                            entry("signal", "Edm.Single")
                    ))
            )),
            entry(SCHEMA_MIXED, Map.ofEntries(
                    entry("kpi_sensitive", Map.ofEntries(
                            entry("date_time", "Edm.DateTimeOffset"),
                            entry("First_kpi", "Edm.Int16"),
                            entry("sECOND_kpi", "Edm.Int32"),
                            entry("third_KPI", "Edm.Int64"),
                            entry("fOURTH_KPi", "Edm.Double"),
                            entry("Fifth_kpI", "Edm.Double"),
                            entry("SIXTH_KPI", "Edm.Single"),
                            entry("seventh_kpi", "Edm.Double")
                    )),
                    entry("kpi_csac", Map.ofEntries(
                            entry("moFdn", "Edm.String"),
                            entry("nodeFDN", "Edm.String"),
                            entry("workHours", "Edm.Int32")
                    ))
            ))
    );

    private static final Map<String, Map<String, Integer>> ROWCOUNTS = Map.ofEntries(
            entry(SCHEMA_PUBLIC, Map.ofEntries(
                    entry("first_table", 3),
                    entry("second_table", 4),
                    entry("third_table", 100 * 1000),
                    entry("first_view", 3),
                    entry("second_view", 4)
            )),
            entry(SCHEMA_NEW, Map.ofEntries(
                    entry("new_table", 2),
                    entry("table_all_types", 3),
                    entry("table_all_array_types", 4)
            )),
            entry(SCHEMA_PARTITIONS, Map.ofEntries(
                    entry("kpi_rolling_aggregation_1440", 5),
                    entry("kpi_rolling_aggregation_1440_p_2023_01_01", 2),
                    entry("kpi_rolling_aggregation_1440_p_2023_01_02", 2),
                    entry("kpi_rolling_aggregation_1440_p_2023_01_03", 1),
                    entry("kpi_simple", 4),
                    entry("sample_regional", 9),
                    entry("sample_regional_west", 3),
                    entry("sample_regional_east", 2),
                    entry("sample_regional_north", 2),
                    entry("sample_regional_south", 2)
            )),
            entry(SCHEMA_MIXED, Map.ofEntries(
                    entry("kpi_sensitive", 5),
                    entry("kpi_csac", 3)
            ))
    );

    private static final Map<String, Map<String, DatabaseTable.TableType>> TABLETYPES = Map.ofEntries(
            entry(SCHEMA_PUBLIC, Map.ofEntries(
                    entry("first_table", DatabaseTable.TableType.TABLE),
                    entry("second_table", DatabaseTable.TableType.TABLE),
                    entry("third_table", DatabaseTable.TableType.TABLE),
                    entry("first_view", DatabaseTable.TableType.VIEW),
                    entry("second_view", DatabaseTable.TableType.VIEW)
            )),
            entry(SCHEMA_NEW, Map.ofEntries(
                    entry("new_table", DatabaseTable.TableType.TABLE),
                    entry("table_all_types", DatabaseTable.TableType.TABLE),
                    entry("table_all_array_types", DatabaseTable.TableType.TABLE)
            )),
            entry(SCHEMA_PARTITIONS, Map.ofEntries(
                    entry("kpi_rolling_aggregation_1440", DatabaseTable.TableType.PARTITIONED_TABLE),
                    entry("kpi_rolling_aggregation_1440_p_2023_01_01", DatabaseTable.TableType.TABLE),
                    entry("kpi_rolling_aggregation_1440_p_2023_01_02", DatabaseTable.TableType.TABLE),
                    entry("kpi_rolling_aggregation_1440_p_2023_01_03", DatabaseTable.TableType.TABLE),
                    entry("kpi_simple", DatabaseTable.TableType.TABLE),
                    entry("sample_regional", DatabaseTable.TableType.PARTITIONED_TABLE),
                    entry("sample_regional_west", DatabaseTable.TableType.TABLE),
                    entry("sample_regional_east", DatabaseTable.TableType.TABLE),
                    entry("sample_regional_north", DatabaseTable.TableType.TABLE),
                    entry("sample_regional_south", DatabaseTable.TableType.TABLE)
            )),
            entry(SCHEMA_MIXED, Map.ofEntries(
                    entry("kpi_sensitive", DatabaseTable.TableType.TABLE),
                    entry("kpi_csac", DatabaseTable.TableType.TABLE)
            ))
    );

    public static List<String> getNamespaces() {
        return new ArrayList<>(MAPPINGS.keySet());
    }

    public static List<String> getEntityTypes(final String namespace) {
        final Map<String, Map<String, String>> entities = MAPPINGS.get(namespace);
        if (entities == null) {
            throw new IllegalArgumentException("Invalid namespace");
        }
        return new ArrayList<>(entities.keySet());
    }

    public static Map<String, String> getFieldMappings(final String namespace, final String entityType) {
        final Map<String, Map<String, String>> entities = MAPPINGS.get(namespace);
        if (entities == null) {
            throw new IllegalArgumentException("Invalid namespace");
        }
        if (!entities.containsKey(entityType)) {
            throw new IllegalArgumentException("Invalid entityType");
        }
        return entities.get(entityType);
    }

    public static int getEntityRowCount(final String namespace, final String entityType) {
        final Map<String, Integer> entities = ROWCOUNTS.get(namespace);
        if (entities == null) {
            throw new IllegalArgumentException("Invalid namespace");
        }
        if (!entities.containsKey(entityType)) {
            throw new IllegalArgumentException("Invalid entityType");
        }
        return entities.get(entityType);
    }

    public static Map<String, Set<String>> getPartitionedTableEntities() {
        return TABLETYPES.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry:: getKey,
                        e -> e.getValue().entrySet().stream()
                                .filter(sub -> sub.getValue() == DatabaseTable.TableType.PARTITIONED_TABLE)
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))))
                .entrySet().stream().filter(e -> e.getValue().size() > 0)
                .collect(Collectors.toMap(Map.Entry:: getKey, e -> e.getValue().keySet()));
    }
}
