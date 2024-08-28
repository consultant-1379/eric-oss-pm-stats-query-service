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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
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
public class KafkaIntegrationTest {

    private static final String BASE_URL = "/kpi-handling/exposure/v1/";
    private static final String SCHEMA_NAME = "public";
    private static final String FIRST_TABLE = "first_table";
    private static final String SECOND_TABLE = "second_table";
    private static final String THIRD_TABLE = "third_table";
    private static final String FIRST_VIEW = "first_view";
    private static final String SECOND_VIEW = "second_view";

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
    @DisplayName("Exposing a new schema")
    void whenExposingNewSchema_thenEndpointBecomesAvailable() throws Exception {
        final String kafkaMessage = "{ \"timestamp\": 1642775672, \"exposure\": [ { \"name\": \"first_table\","
                + " \"kind\": \"TABLE\" }, { \"name\": \"first_view\", \"kind\": \"VIEW\" } ]}";

        assertSchemaIsNotExposed(SCHEMA_NAME);

        template.sendDefault(SCHEMA_NAME, kafkaMessage);
        final List<ConsumerRecord<String, KafkaMessage>> kafkaRecords = getRecordsFromTopic(topicName);
        assertThat(kafkaRecords.get(kafkaRecords.size() - 1).value().getExposure()).hasSize(2);

        assertTablesAreExposed(SCHEMA_NAME, FIRST_TABLE, FIRST_VIEW);
        assertTablesAreNotExposed(SCHEMA_NAME, SECOND_TABLE, SECOND_VIEW, THIRD_TABLE);
    }

    @Test
    @DisplayName("Exposing two new schemas")
    void whenExposingTwoNewSchemas_thenEndpointsBecomesAvailable() throws Exception {
        final String firstSchemaName = SCHEMA_NAME;
        final String firstSchemaConfig = "{ \"timestamp\": 1642775672, \"exposure\": [ { \"name\": \"first_table\", "
                + "\"kind\": \"TABLE\" }, { \"name\": \"first_view\", \"kind\": \"VIEW\" } ]}";

        final String secondSchemaName = "new_schema";
        final String secondSchemaConfig = "{ \"timestamp\": 1642775672, \"exposure\": [ { \"name\": \"new_table\", "
                + "\"kind\": \"TABLE\" } ]}";

        assertSchemaIsNotExposed(firstSchemaName);
        assertTablesAreNotExposed(firstSchemaName, FIRST_TABLE, FIRST_VIEW, SECOND_TABLE,
                SECOND_VIEW, THIRD_TABLE);
        assertSchemaIsNotExposed(secondSchemaName);
        assertTablesAreNotExposed(secondSchemaName, "new_table");

        template.sendDefault(firstSchemaName, firstSchemaConfig);
        List<ConsumerRecord<String, KafkaMessage>> kafkaRecords = getRecordsFromTopic(topicName);
        assertThat(kafkaRecords.get(kafkaRecords.size() - 1).value().getExposure()).hasSize(2);

        template.sendDefault(secondSchemaName, secondSchemaConfig);
        kafkaRecords = getRecordsFromTopic(topicName);
        assertThat(kafkaRecords.get(kafkaRecords.size() - 1).value().getExposure()).hasSize(1);

        assertTablesAreExposed(firstSchemaName, FIRST_TABLE, FIRST_VIEW);
        assertTablesAreNotExposed(firstSchemaName, SECOND_TABLE, SECOND_VIEW, THIRD_TABLE);
        assertTablesAreExposed(secondSchemaName, "new_table");
    }

    @Test
    @DisplayName("Modify already exposed schema")
    void whenModifyingAlreadyExposedSchema_thenEndpointIsUpdated() throws Exception {
        final String originalConfiguration = "{ \"timestamp\": 1642775672, \"exposure\": "
                + "[ { \"name\": \"first_table\", \"kind\": \"TABLE\" }, { \"name\": \"first_view\", \"kind\": \"VIEW\" } ]}";

        template.sendDefault(SCHEMA_NAME, originalConfiguration);
        List<ConsumerRecord<String, KafkaMessage>> kafkaRecords = getRecordsFromTopic(topicName);
        assertThat(kafkaRecords.get(kafkaRecords.size() - 1).value().getExposure()).hasSize(2);

        assertTablesAreExposed(SCHEMA_NAME, FIRST_TABLE, FIRST_VIEW);
        assertTablesAreNotExposed(SCHEMA_NAME, SECOND_TABLE, SECOND_VIEW, THIRD_TABLE);

        final String newConfiguration = "{ \"timestamp\": 1642775673, \"exposure\": "
                + "[ { \"name\": \"first_table\", \"kind\": \"TABLE\" }, { \"name\": \"second_table\", "
                + "\"kind\": \"TABLE\" }, { \"name\": \"first_view\", \"kind\": \"VIEW\" } ]}";
        template.sendDefault(SCHEMA_NAME, newConfiguration);
        kafkaRecords = getRecordsFromTopic(topicName);
        assertThat(kafkaRecords.get(kafkaRecords.size() - 1).value().getExposure()).hasSize(3);

        assertTablesAreExposed(SCHEMA_NAME, FIRST_TABLE, FIRST_VIEW, SECOND_TABLE);
        assertTablesAreNotExposed(SCHEMA_NAME, SECOND_VIEW, THIRD_TABLE);
    }

    @Test
    @DisplayName("Hide already exposed schema")
    void whenHidingAlreadyExposedSchema_thenEndpointIsUpdated() throws Exception {
        final String originalConfiguration = "{ \"timestamp\": 1642775672, \"exposure\": "
                + "[ { \"name\": \"first_table\", \"kind\": \"TABLE\" }, { \"name\": \"first_view\", \"kind\": \"VIEW\" } ]}";

        template.sendDefault(SCHEMA_NAME, originalConfiguration);
        List<ConsumerRecord<String, KafkaMessage>> kafkaRecords = getRecordsFromTopic(topicName);
        assertThat(kafkaRecords.get(kafkaRecords.size() - 1).value().getExposure()).hasSize(2);

        assertTablesAreExposed(SCHEMA_NAME, FIRST_TABLE, FIRST_VIEW);

        final String newConfiguration = null;
        template.sendDefault(SCHEMA_NAME, newConfiguration);

        kafkaRecords = getRecordsFromTopic(topicName);
        assertNull(kafkaRecords.get(kafkaRecords.size() - 1).value());

        assertSchemaIsNotExposed(SCHEMA_NAME);
        assertTablesAreNotExposed(SCHEMA_NAME, FIRST_TABLE, FIRST_VIEW,
                SECOND_TABLE, SECOND_VIEW, THIRD_TABLE);
    }

    @Test
    @DisplayName("Receive null payload")
    void whenGettingNullPayload_shouldNotThrowException() {
        assertDoesNotThrow(() -> template.sendDefault("key", null));
    }

    @Test
    @DisplayName("Receive null key")
    void whenGettingNullKey_shouldNotThrowException() {
        final String message = "{ \"timestamp\": 1642775672, \"exposure\":"
                + " [ { \"name\": \"new_table\", \"kind\": \"TABLE\" } ]}";
        assertDoesNotThrow(() -> template.sendDefault(null, message));
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
