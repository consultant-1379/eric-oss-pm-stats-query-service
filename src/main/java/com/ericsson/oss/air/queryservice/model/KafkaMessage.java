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

import java.util.Collections;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * POJO class representation of the JSON message that the service will receive on the Kafka topic.
 */
public class KafkaMessage {
    @Getter @Setter private Long timestamp;
    private List<SchemaExposureConfiguration> exposure;

    /**
     * Returns the exposure configuration.
     * @return exposure configuration as unmodifiable list
     */
    public List<SchemaExposureConfiguration> getExposure() {
        return Collections.unmodifiableList(exposure);
    }
}
