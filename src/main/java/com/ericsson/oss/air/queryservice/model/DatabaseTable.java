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

package com.ericsson.oss.air.queryservice.model;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * POJO class representing the table/view metadata queried from the database.
 */
@Getter
@Setter
public class DatabaseTable {
    private String schema;
    private String name;
    private TableType type;

    /**
     * Database metadata element types.
     * @see #TABLE
     * @see #VIEW
     * @see #PARTITIONED_TABLE
     */
    public enum TableType {
        TABLE("TABLE"), VIEW("VIEW"), PARTITIONED_TABLE("PARTITIONED TABLE");

        private static final Map<String, TableType> DICT = new HashMap<>();

        static {
            for (final TableType t: TableType.values()) {
                DICT.put(t.getValue(), t);
            }
        }

        private final String value;

        TableType(final String value) {
            this.value = value;
        }

        /**
         * Gets the attached value.
         * @return the value
         */
        public String getValue() {
            return value;
        }

        /**
         * Gets the enum by value.
         * @param value value attached to the enum
         * @return the enum
         */
        public static TableType getByValue(final String value) {
            return DICT.get(value);
        }
    }
}
