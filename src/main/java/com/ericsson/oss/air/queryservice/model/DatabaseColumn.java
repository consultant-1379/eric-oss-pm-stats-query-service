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

import lombok.Getter;
import lombok.Setter;

/**
 * POJO class representing the column metadata queried from the database.
 */

@Getter
@Setter
public class DatabaseColumn {
    private String name;
    private int type;
    private String udtName;
}
