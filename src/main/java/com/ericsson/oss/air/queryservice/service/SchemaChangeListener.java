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

import static com.ericsson.oss.air.queryservice.service.QueryService.CUSTOM_METRIC_PREFIX;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.stereotype.Service;

import com.ericsson.oss.air.queryservice.model.KafkaMessage;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;

/**
 * Component responsible for receiving messages on the specific Kafka topic.
 */
@Service
@ConditionalOnProperty({"datasource.enabled", "kafka.enabled"})
public class SchemaChangeListener {

    private static Counter schemaChangeEventsCounter = Metrics.counter(CUSTOM_METRIC_PREFIX + "schema_change_events_counter");

    @Autowired
    private ExposureChecker exposureChecker;

    @Autowired
    private DatabaseInfoService databaseInfoService;

    /**
     * Receives automatically deserialized messages from the given topic (@KafkaListener annotation). Checks the
     * payload and modifies schema exposure as required. This listener reads records from all partitions as configured
     * in {@link com.ericsson.oss.air.queryservice.config.KafkaConfig.PartitionFinder}.
     * @param payload ConsumerRecord object containing the message
     */
    @KafkaListener(topicPartitions = @TopicPartition(
            topic = "${schema-listener.topics}",
            partitions = "#{@partitionFinder.partitions('${schema-listener.topics}')}",
            partitionOffsets = @PartitionOffset(partition = "*", initialOffset = "0")))
    public void listen(final ConsumerRecord<String, KafkaMessage> payload) {
        if (payload != null && payload.key() != null) {
            databaseInfoService.clearTablesCache(payload.key());
            schemaChangeEventsCounter.increment();
            if (payload.value() == null) {
                exposureChecker.hideSchemaExposure(payload.key());
            } else {
                exposureChecker.showSchemaExposure(payload.key(), payload.value().getExposure());
            }
        }
    }
}
