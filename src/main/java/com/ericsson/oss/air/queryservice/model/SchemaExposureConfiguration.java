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

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Data;

/**
 * POJO class representing a schema element with a type of TABLE or VIEW.
 */
@Data
public class SchemaExposureConfiguration {
    private String name;
    private Type kind;

    /**
     * Exposure element types.
     * @see #TABLE
     * @see #VIEW
     */
    public enum Type {
        TABLE("TABLE"), VIEW("VIEW");

        private final String value;

        Type(final String value) {
            this.value = value;
        }

        /**
         * Returns the value.
         * @return current value
         */
        @JsonValue
        public String getValue() {
            return value;
        }
    }
}
