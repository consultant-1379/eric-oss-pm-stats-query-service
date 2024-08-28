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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.oss.air.queryservice.config.KafkaTestContainersConfiguration;
import com.ericsson.oss.air.queryservice.config.PostgresContainersConfiguration;
import com.ericsson.oss.air.queryservice.model.KafkaMessage;

@SpringBootTest
@Import({KafkaTestContainersConfiguration.class, PostgresContainersConfiguration.class})
@TestPropertySource(properties = {"kafka.enabled=true", "management.endpoint.health.group.readiness.include=kafka,db,ping"})
public class KafkaPartitionedTest {
    private static final String BASE_URL = "/kpi-handling/exposure/v1/";
    private static final String SCHEMA_NAME = "partitest";
    private static final String PARTITIONED_BASE_TABLE_WITH_RANGE = "kpi_rolling_aggregation_1440";
    private static final String RANGE_PARTITION_20230101 = "kpi_rolling_aggregation_1440_p_2023_01_01";
    private static final String SIMPLE_TABLE = "kpi_simple";
    private static final String PARTITIONED_BASE_TABLE_WITH_LIST = "sample_regional";
    private static final String LIST_PARTITION_WEST = "sample_regional_west";
    private static final String LIST_PARTITION_EAST = "sample_regional_east";
    private static final String LIST_PARTITION_NORTH = "sample_regional_north";
    private static final String LIST_PARTITION_SOUTH = "sample_regional_south";
    private static final String KAFKA_MESSAGE_MIDDLE = "\", \"kind\": \"TABLE\" }, { \"name\": \"";

    @Autowired
    private KafkaTemplate<String, String> template;

    @Autowired
    private ConsumerFactory<String, KafkaMessage> consumerFactory;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Value("${schema-listener.topics}")
    private String topicName;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        template.setDefaultTopic(topicName);
    }

    @Test
    @DisplayName("Exposing a new schema with partitioned table")
    void whenExposingNewSchemaWithPartitionedTable_thenEndpointBecomesAvailable() throws Exception {
        final String kafkaMessage = "{ \"timestamp\": 1673348400, \"exposure\": [ { \"name\": \""
                + PARTITIONED_BASE_TABLE_WITH_RANGE
                + KAFKA_MESSAGE_MIDDLE
                + SIMPLE_TABLE + "\", \"kind\": \"TABLE\" } ]}";

        assertSchemaIsNotExposed(SCHEMA_NAME);

        template.sendDefault(SCHEMA_NAME, kafkaMessage);
        final List<ConsumerRecord<String, KafkaMessage>> kafkaRecords = getRecordsFromTopic(topicName);
        assertThat(kafkaRecords.get(kafkaRecords.size() - 1).value().getExposure()).hasSize(2);

        assertTablesAreExposed(SCHEMA_NAME, PARTITIONED_BASE_TABLE_WITH_RANGE, SIMPLE_TABLE);
        assertTablesAreNotExposed(SCHEMA_NAME, RANGE_PARTITION_20230101, PARTITIONED_BASE_TABLE_WITH_LIST,
                LIST_PARTITION_WEST, LIST_PARTITION_EAST, LIST_PARTITION_NORTH, LIST_PARTITION_SOUTH);
    }

    @Test
    @DisplayName("Modify already exposed partitions")
    void whenModifyingPartitionExposure_thenEndpointIsUpdated() throws Exception {
        final String originalConfiguration = "{ \"timestamp\": 1673348600, \"exposure\": [ { \"name\": \""
                + PARTITIONED_BASE_TABLE_WITH_LIST
                + KAFKA_MESSAGE_MIDDLE
                + LIST_PARTITION_NORTH + "\", \"kind\": \"TABLE\" } ]}";

        template.sendDefault(SCHEMA_NAME, originalConfiguration);
        List<ConsumerRecord<String, KafkaMessage>> kafkaRecords = getRecordsFromTopic(topicName);
        assertThat(kafkaRecords.get(kafkaRecords.size() - 1).value().getExposure()).hasSize(2);

        assertTablesAreExposed(SCHEMA_NAME, PARTITIONED_BASE_TABLE_WITH_LIST, LIST_PARTITION_NORTH);
        assertTablesAreNotExposed(SCHEMA_NAME, LIST_PARTITION_SOUTH, LIST_PARTITION_EAST, LIST_PARTITION_WEST);

        final String newConfiguration = "{ \"timestamp\": 1673348700, \"exposure\": [ { \"name\": \""
                + PARTITIONED_BASE_TABLE_WITH_LIST
                + KAFKA_MESSAGE_MIDDLE
                + LIST_PARTITION_WEST
                + KAFKA_MESSAGE_MIDDLE
                + LIST_PARTITION_SOUTH + "\", \"kind\": \"TABLE\"} ]}";
        template.sendDefault(SCHEMA_NAME, newConfiguration);
        kafkaRecords = getRecordsFromTopic(topicName);
        assertThat(kafkaRecords.get(kafkaRecords.size() - 1).value().getExposure()).hasSize(3);

        assertTablesAreExposed(SCHEMA_NAME, PARTITIONED_BASE_TABLE_WITH_LIST, LIST_PARTITION_WEST, LIST_PARTITION_SOUTH);
        assertTablesAreNotExposed(SCHEMA_NAME, LIST_PARTITION_NORTH, LIST_PARTITION_EAST);
    }

    private void assertSchemaIsNotExposed(final String schemaName) throws Exception {
        mvc.perform(get(BASE_URL + schemaName + "/$metadata")).andExpect(status().isNotFound());
    }

    private void assertTablesAreExposed(final String schemaName, final String... entityTypes) throws Exception {
        final ResultActions metadataResult = mvc.perform(get(BASE_URL + schemaName + "/$metadata"));
        metadataResult.andExpect(status().isOk());

        metadataResult.andExpect(MockMvcResultMatchers.xpath("//Schema[@Namespace=\"%s\"]", schemaName).exists())
                .andExpect(MockMvcResultMatchers.xpath("//Schema[@Namespace=\"%s\"]/EntityType",
                        schemaName).nodeCount(entityTypes.length));

        for (final String entityType : entityTypes) {
            metadataResult.andExpect(MockMvcResultMatchers.xpath(
                    "//Schema[@Namespace=\"%s\"]/EntityType[@Name=\"%s\"]", schemaName, entityType).exists());
        }

        for (final String entityType : entityTypes) {
            mvc.perform(get(BASE_URL + schemaName + "/" + entityType)).andExpect(status().isOk());
        }
    }

    private void assertTablesAreNotExposed(final String schemaName, final String... entityTypes) throws Exception {
        for (final String entityType : entityTypes) {
            mvc.perform(get(BASE_URL + schemaName + "/" + entityType)).andExpect(status().isNotFound());
        }
    }

    private List<ConsumerRecord<String, KafkaMessage>> getRecordsFromTopic(final String topic) {
        try (Consumer<String, KafkaMessage> consumer = consumerFactory.createConsumer()) {
            final List<PartitionInfo> partitionInfos = consumer.partitionsFor(topic);
            final List<TopicPartition> topicPartitions = partitionInfos.stream()
                    .map(e -> new TopicPartition(e.topic(), e.partition())).collect(Collectors.toList());
            consumer.assign(topicPartitions);
            consumer.seekToBeginning(topicPartitions);

            final ConsumerRecords<String, KafkaMessage> kafkaRecords = consumer.poll(Duration.of(3, ChronoUnit.SECONDS));

            return StreamSupport
                    .stream(kafkaRecords.spliterator(), false)
                    .collect(Collectors.toList());
        }
    }
}
