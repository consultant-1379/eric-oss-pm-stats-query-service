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

import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.CommonLoggingErrorHandler;

import com.ericsson.oss.air.queryservice.model.KafkaMessage;

/**
 *  Configuration class for the Kafka related Bean constructions.
 */
@Configuration
@ConditionalOnProperty("kafka.enabled")
public class KafkaConfig {

    /**
     * PartitionFinder created for the Kafka listener.
     * @param consumerFactory kafka consumer factory
     * @return PartitionFinder to be autowired to the Kafka listener
     */
    @Bean
    public PartitionFinder partitionFinder(final ConsumerFactory<String, KafkaMessage> consumerFactory) {
        return new PartitionFinder(consumerFactory);
    }

    /**
     * Receives and logs deserialization exceptions.
     * @return CommonLoggingErrorHandler bean that is required by the Kafka listener container factories.
     */
    @Bean
    public CommonLoggingErrorHandler errorHandler() {
        return new CommonLoggingErrorHandler();
    }

    /**
     * Class for Kafka topic partition collection.
     */
    public static class PartitionFinder {
        private final ConsumerFactory<String, KafkaMessage> consumerFactory;

        /**
         * Constructor for PartitionFinder.
         * @param consumerFactory Kafka consumer factory
         */
        public PartitionFinder(final ConsumerFactory<String, KafkaMessage> consumerFactory) {
            this.consumerFactory = consumerFactory;
        }

        /**
         * Collects all partition ids of a given Kafka topic into an array.
         * @param topic name of the topic
         * @return array of partition ids
         */
        public String[] partitions(final String topic) {
            try (Consumer<String, KafkaMessage> consumer = consumerFactory.createConsumer()) {
                return consumer.partitionsFor(topic).stream().map(pi -> "" + pi.partition()).toArray(String[]::new);
            }
        }
    }
}
