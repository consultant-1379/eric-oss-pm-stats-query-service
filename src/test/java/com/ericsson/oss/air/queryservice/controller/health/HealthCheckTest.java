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

package com.ericsson.oss.air.queryservice.controller.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import com.ericsson.oss.air.queryservice.CoreApplication;
import com.ericsson.oss.air.queryservice.config.KafkaTestContainersConfiguration;
import com.ericsson.oss.air.queryservice.config.PostgresContainersConfiguration;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {CoreApplication.class})
@Import({KafkaTestContainersConfiguration.class, PostgresContainersConfiguration.class})
@TestPropertySource(properties = {"kafka.enabled=true", "management.endpoint.health.group.readiness.include=kafka,db,ping"})
public class HealthCheckTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mvc;

    @Autowired
    private DataSourceHealthIndicator dataSourceHealthIndicator;

    @Autowired
    private KafkaContainer kafkaContainer;

    @Autowired
    private PostgreSQLContainer postgresContainer;

    @BeforeEach
    public void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void get_health_status_ok() throws Exception {
        mvc.perform(get("/actuator/health").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(content().json("{'status' : 'UP'}"));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    public void get_test_readiness_database_down() throws Exception {
        postgresContainer.close();
        mvc.perform(get("/actuator/health/readiness").contentType(MediaType.TEXT_PLAIN)).andExpect(status().isServiceUnavailable())
                .andExpect(content().json("{'status' : 'DOWN'}"));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    public void get_test_readiness_kafka_down() throws Exception {
        kafkaContainer.stop();
        mvc.perform(get("/actuator/health/readiness").contentType(MediaType.TEXT_PLAIN)).andExpect(status().isServiceUnavailable())
                .andExpect(content().json("{'status' : 'DOWN'}"));
    }

    @Test
    public void get_test_readiness_ok() throws Exception {
        mvc.perform(get("/actuator/health/readiness").contentType(MediaType.TEXT_PLAIN)).andExpect(status().isOk())
                .andExpect(content().json("{'status' : 'UP'}"));
    }

    @Test
    public void get_test_liveness_ok() throws Exception {
        mvc.perform(get("/actuator/health/liveness").contentType(MediaType.TEXT_PLAIN)).andExpect(status().isOk())
                .andExpect(content().json("{'status' : 'UP'}"));
    }
}