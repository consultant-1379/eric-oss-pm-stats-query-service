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

package com.ericsson.oss.air.queryservice.controller.health;

import java.time.Duration;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.KafkaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Component;

import com.ericsson.oss.air.queryservice.model.KafkaMessage;

/**
 * Class to determine the availability of Kafka.
 */
@Component
@ConditionalOnProperty("kafka.enabled")
public class KafkaHealthIndicator implements HealthIndicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaHealthIndicator.class);

    @Autowired
    private ConsumerFactory<String, KafkaMessage> consumerFactory;

    /**
     * Determines the Kafka health status by checking a consumer's ability to list topics.
     * @return a new Health instance with a Status of UP or DOWN
     */
    @Override
    public Health health() {
        Health.Builder status = Health.down();
        try (Consumer<String, KafkaMessage> consumer = consumerFactory.createConsumer()) {
            consumer.listTopics(Duration.ofMillis(1000));
            status = Health.up();
        } catch (final KafkaException e) {
            status = Health.down();
            status.withDetail("Error", "Connection timeout: 1000ms exceeded");
            LOGGER.error("Unable to connect to Kafka: Connection timeout: 1000ms exceeded", e);
        }
        return status.build();
    }
}
