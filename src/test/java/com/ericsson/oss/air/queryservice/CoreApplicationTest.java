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

package com.ericsson.oss.air.queryservice;

import static com.ericsson.oss.air.queryservice.service.QueryService.CUSTOM_METRIC_PREFIX;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.oss.air.queryservice.config.KafkaTestContainersConfiguration;
import com.ericsson.oss.air.queryservice.config.PostgresContainersConfiguration;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {CoreApplication.class, CoreApplicationTest.class})
@Import({KafkaTestContainersConfiguration.class, PostgresContainersConfiguration.class})
public class CoreApplicationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mvc;

    @Value("${info.app.description}")
    private String description;

    @BeforeEach
    public void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void application_starts() {
        CoreApplication.main(new String[] {});
    }

    @Test
    public void metrics_available() throws Exception {
        final MvcResult result = mvc.perform(get("/actuator/prometheus").contentType(MediaType.TEXT_PLAIN)).andExpect(status().isOk())
                .andReturn();

        Assertions.assertTrue(result.getResponse().getContentAsString().contains("jvm_threads_states_threads"));
        Assertions.assertTrue(result.getResponse().getContentAsString().contains(CUSTOM_METRIC_PREFIX + "built_count_queries_counter"));
        Assertions.assertTrue(result.getResponse().getContentAsString().contains(CUSTOM_METRIC_PREFIX + "executed_count_queries_counter"));
        Assertions.assertTrue(result.getResponse().getContentAsString().contains(CUSTOM_METRIC_PREFIX + "built_listing_queries_counter"));
        Assertions.assertTrue(result.getResponse().getContentAsString().contains(CUSTOM_METRIC_PREFIX + "executed_listing_queries_counter"));
        Assertions.assertTrue(result.getResponse().getContentAsString().contains(CUSTOM_METRIC_PREFIX + "records_counter"));
        Assertions.assertTrue(result.getResponse().getContentAsString().contains(CUSTOM_METRIC_PREFIX + "query_execution_timer"));
        Assertions.assertTrue(result.getResponse().getContentAsString().contains(CUSTOM_METRIC_PREFIX + "count_query_execution_timer"));
        Assertions.assertTrue(result.getResponse().getContentAsString().contains(CUSTOM_METRIC_PREFIX + "convert_result_list_timer"));
        Assertions.assertTrue(result.getResponse().getContentAsString().contains(CUSTOM_METRIC_PREFIX + "served_queries_counter"));
    }

    @Test
    public void info_available() throws Exception {
        final MvcResult result = mvc.perform(get("/actuator/info").contentType(MediaType.TEXT_PLAIN)).andExpect(status().isOk())
                .andReturn();

        Assertions.assertTrue(result.getResponse().getContentAsString().contains(this.description));
    }
}
