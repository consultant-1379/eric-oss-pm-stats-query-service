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

package com.ericsson.oss.air.queryservice.util;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data class for conversion of logcontrol.json to object through ObjectMapper.
 */
@Data
@NoArgsConstructor
public class LogControl {

    private String container;
    private String severity;
}
