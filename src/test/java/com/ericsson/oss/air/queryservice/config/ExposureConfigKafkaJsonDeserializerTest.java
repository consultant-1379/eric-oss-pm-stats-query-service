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

package com.ericsson.oss.air.queryservice.config;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.common.errors.SerializationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Exposure config Kafka JSON deserialzer")
public class ExposureConfigKafkaJsonDeserializerTest {

    @Test
    @DisplayName("Receive invalid message")
    void whenReceivingInvalidMessage_thenThrowsException() {
        final ExposureConfigKafkaJsonDeserializer deserializer = new ExposureConfigKafkaJsonDeserializer();

        final String message = "{ ";

        assertThrows(SerializationException.class, () -> deserializer.deserialize("topic", message.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("Receive null message")
    void whenReceivingNullMessage_thenReturnsNull() {
        final ExposureConfigKafkaJsonDeserializer deserializer = new ExposureConfigKafkaJsonDeserializer();

        assertNull(deserializer.deserialize("topic", null));
    }
}
