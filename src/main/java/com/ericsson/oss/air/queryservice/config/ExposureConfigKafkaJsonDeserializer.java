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

import java.io.IOException;
import java.util.Arrays;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import com.ericsson.oss.air.queryservice.model.KafkaMessage;

/**
 *  Class responsible for deserializing JSON messages into KafkaMessage POJOs.
 */
public class ExposureConfigKafkaJsonDeserializer extends JsonDeserializer<KafkaMessage> {

    /**
     * Calls the appropriate deserializing method.
     * @param topic the name of the topic
     * @param headers Kafka headers
     * @param data array of raw bytes
     */
    @Override
    public KafkaMessage deserialize(final String topic, final Headers headers, final byte[] data) {
        return deserialize(topic, data);
    }

    /**
     * Reconstructs the KafkaMessage data structure of a given topic from a series of bytes.
     * @param topic the name of the topic
     * @param data array of raw bytes
     */
    @Override
    public KafkaMessage deserialize(final String topic, final byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            return objectMapper.readValue(data, KafkaMessage.class);
        } catch (final IOException e) {
            throw new SerializationException(
                    "Can't deserialize data [" + Arrays.toString(data) + "] from topic [" + topic + "]", e);
        }
    }
}
