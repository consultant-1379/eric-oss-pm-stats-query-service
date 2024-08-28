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

package com.ericsson.oss.air.queryservice.controller.example;

import static com.ericsson.oss.air.queryservice.controller.example.TestDataEntityMapping.SCHEMA_MIXED;
import static com.ericsson.oss.air.queryservice.controller.example.TestDataEntityMapping.SCHEMA_PUBLIC;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.ericsson.oss.air.queryservice.config.KafkaTestContainersConfiguration;
import com.ericsson.oss.air.queryservice.config.PostgresContainersConfiguration;
import com.ericsson.oss.air.queryservice.service.ExposureChecker;

@SuppressWarnings("checkstyle:MultipleStringLiterals")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import({KafkaTestContainersConfiguration.class, PostgresContainersConfiguration.class})
@TestPropertySource(properties = {"odata-response.max-record-count: 5"})
public class SampleRestClientPagingTest {

    private static final String BASE_URL = "/kpi-handling/exposure/v1/";
    private static final String SCHEMA_PAGING = "paging_schema";

    @Autowired
    private MockMvc mockMvs;

    @MockBean
    private ExposureChecker schemaChangeListener;

    @BeforeEach
    void init() {
        when(schemaChangeListener.isSchemaExposed(any())).thenReturn(true);
        when(schemaChangeListener.isTableExposed(any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("Select with count and top - unchanged, unpaged")
    public void get_kpi_with_count_and_top() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$count=true&$top=3"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table\",\"@odata.count\":3,"
                                + "\"value\":[{\"date_time\":\"2022-03-01T10:07:38.206333Z\",\"first_kpi\":-32768,"
                                + "\"second_kpi\":-2147483648,\"third_kpi\":-9223372036854775808,\"fourth_kpi\":12345.6789,"
                                + "\"fifth_kpi\":12345.67890,\"sixth_kpi\":987.6543,\"seventh_kpi\":1.2345678901234567E9},"
                                + "{\"date_time\":\"2022-03-01T09:23:02.206333Z\",\"first_kpi\":32767,\"second_kpi\":2147483647,"
                                + "\"third_kpi\":9223372036854775807,\"fourth_kpi\":-12345.6789,\"fifth_kpi\":-12345.67890,"
                                + "\"sixth_kpi\":-987.6543,\"seventh_kpi\":-1.2345678901234567E9},"
                                + "{\"date_time\":\"2022-02-10T21:42:01.206333Z\",\"first_kpi\":0,\"second_kpi\":21474,"
                                + "\"third_kpi\":92236854775807,\"fourth_kpi\":-1235.6790,\"fifth_kpi\":-1235.69000,"
                                + "\"sixth_kpi\":-98.6521,\"seventh_kpi\":-1.2347890123456713E7}]}", true));
    }

    @Test
    @DisplayName("Select with count and filter and top - unchanged, unpaged")
    public void get_kpi_with_count_and_filter_and_top() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_MIXED + "/kpi_sensitive?$filter=First_kpi le 3&$select=date_time,First_kpi&$count=true&$top=3"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#kpi_sensitive(date_time,First_kpi)\","
                                + "\"@odata.count\":4,\"value\":["
                                + "{\"date_time\":\"2023-05-01T05:21:23.444333Z\",\"First_kpi\":0},"
                                + "{\"date_time\":\"2023-05-02T06:31:34.555678Z\",\"First_kpi\":1},"
                                + "{\"date_time\":\"2023-05-03T07:41:45.667776Z\",\"First_kpi\":2}]}", true));
    }

    @Test
    @DisplayName("Select with count and filter and top and skip - unchanged, unpaged")
    public void get_kpi_with_count_and_filter_and_top_and_skip() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_MIXED + "/kpi_sensitive?$filter=First_kpi le 3&$select=date_time,First_kpi&$count=true&$top=3&$skip=1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#kpi_sensitive(date_time,First_kpi)\","
                                + "\"@odata.count\":4,\"value\":["
                                + "{\"date_time\":\"2023-05-02T06:31:34.555678Z\",\"First_kpi\":1},"
                                + "{\"date_time\":\"2023-05-03T07:41:45.667776Z\",\"First_kpi\":2},"
                                + "{\"date_time\":\"2023-05-04T08:51:56.788877Z\",\"First_kpi\":3}]}", true));
    }

    @Test
    @DisplayName("Select with count and top and skip - unchanged, unpaged")
    public void get_kpi_with_count_and_top_and_skip() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$count=true&$top=2&$skip=1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table\",\"@odata.count\":3,"
                                + "\"value\":[{\"date_time\":\"2022-03-01T09:23:02.206333Z\",\"first_kpi\":32767,"
                                + "\"second_kpi\":2147483647,\"third_kpi\":9223372036854775807,\"fourth_kpi\":-12345.6789,"
                                + "\"fifth_kpi\":-12345.67890,\"sixth_kpi\":-987.6543,\"seventh_kpi\":-1.2345678901234567E9},"
                                + "{\"date_time\":\"2022-02-10T21:42:01.206333Z\",\"first_kpi\":0,\"second_kpi\":21474,"
                                + "\"third_kpi\":92236854775807,\"fourth_kpi\":-1235.6790,\"fifth_kpi\":-1235.69000,"
                                + "\"sixth_kpi\":-98.6521,\"seventh_kpi\":-1.2347890123456713E7}]}", true));
    }

    @Test
    @DisplayName("Simple select - small table, paged")
    public void get_kpi_full_small_table() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_MIXED + "/kpi_csac"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#kpi_csac\",\"value\":["
                                + "{\"moFdn\":\"/epg:epg/pgw/ns[name=131-000259]/apn[name=apn01.ericsson.se]\","
                                + "\"nodeFDN\":\"ManagedElement=NodeFDNManagedElement0002,Equipment=3,SupportUnit=2\","
                                + "\"workHours\":12},"
                                + "{\"moFdn\":\"/epg:epg/pgw/ns[name=131-000259]/apn[name=apn01.ericsson.se]\","
                                + "\"nodeFDN\":\"ManagedElement=NodeFDNManagedElement0099,Equipment=2,SupportUnit=1\","
                                + "\"workHours\":16},"
                                + "{\"moFdn\":\"/epg:epg/ftc/ns[name=123-001896]/apn[name=offline.ericsson.hu]\","
                                + "\"nodeFDN\":\"ManagedElement=NodeFDNManagedElement0002,Equipment=3,SupportUnit=2\","
                                + "\"workHours\":20}]}", true));
    }

    @Test
    @DisplayName("Negative case - simple select - medium table, throws bad request")
    public void get_kpi_paging_medium_table_throws_bad_request() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PAGING + "/medium_table"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.message")
                        .value("The size of the result set has reached its upper bound: 5, "
                                + "please use $top with a value less than or equal to the upper bound."));
    }

    @Test
    @DisplayName("Negative case - simple select - large table, throws bad request")
    public void get_kpi_paging_large_table_throws_bad_request() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PAGING + "/large_table"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.message")
                        .value("The size of the result set has reached its upper bound: 5, "
                                + "please use $top with a value less than or equal to the upper bound."));
    }

    @Test
    @DisplayName("Negative case - large table with skip, throws bad request")
    public void get_kpi_paging_large_table_skip_throws_bad_request() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PAGING + "/large_table?$skip=5000"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.message")
                        .value("The size of the result set has reached its upper bound: 5, "
                                + "please use $top with a value less than or equal to the upper bound."));
    }

    @Test
    @DisplayName("Negative case - large table with top, throws bad request")
    public void get_kpi_paging_large_table_top_throws_bad_request() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PAGING + "/large_table?$top=5000"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.message")
                        .value("The size of the result set has reached its upper bound: 5, "
                                + "please use $top with a value less than or equal to the upper bound."));
    }

    @Test
    @DisplayName("Negative case - large table with skip/top, throws bad request")
    public void get_kpi_paging_large_table_skip_top_throws_bad_request() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PAGING + "/large_table?$skip=5000&$top=5000"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.message")
                        .value("The size of the result set has reached its upper bound: 5, "
                                + "please use $top with a value less than or equal to the upper bound."));
    }

    @Test
    @DisplayName("Positive case - large table with top")
    public void get_kpi_paging_large_table_top() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PAGING + "/large_table?$top=5"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#large_table\",\"value\":["
                                + "{\"date_time\":\"1990-01-01\",\"int_kpi\":100001,\"flt_kpi\":1.10},"
                                + "{\"date_time\":\"1990-01-02\",\"int_kpi\":100002,\"flt_kpi\":2.10},"
                                + "{\"date_time\":\"1990-01-03\",\"int_kpi\":100003,\"flt_kpi\":3.10},"
                                + "{\"date_time\":\"1990-01-04\",\"int_kpi\":100004,\"flt_kpi\":4.10},"
                                + "{\"date_time\":\"1990-01-05\",\"int_kpi\":100005,\"flt_kpi\":5.10}]}", true));
    }

    @Test
    @DisplayName("Positive edge case - large table with skip & top")
    public void get_kpi_paging_large_table_skip_top() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PAGING + "/large_table?$skip=19995&$top=5000"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#large_table\",\"value\":["
                                + "{\"date_time\":\"2044-09-29\",\"int_kpi\":119996,\"flt_kpi\":19996.10},"
                                + "{\"date_time\":\"2044-09-30\",\"int_kpi\":119997,\"flt_kpi\":19997.10},"
                                + "{\"date_time\":\"2044-10-01\",\"int_kpi\":119998,\"flt_kpi\":19998.10},"
                                + "{\"date_time\":\"2044-10-02\",\"int_kpi\":119999,\"flt_kpi\":19999.10},"
                                + "{\"date_time\":\"2044-10-03\",\"int_kpi\":120000,\"flt_kpi\":20000.10}]}", true));
    }

    @Test
    @DisplayName("Select with count and filter, small table")
    public void get_kpi_with_count_and_filter() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_MIXED + "/kpi_sensitive?$filter=First_kpi le 3&$select=date_time,First_kpi&$count=true"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#kpi_sensitive(date_time,First_kpi)\","
                                + "\"@odata.count\":4,\"value\":["
                                + "{\"date_time\":\"2023-05-01T05:21:23.444333Z\",\"First_kpi\":0},"
                                + "{\"date_time\":\"2023-05-02T06:31:34.555678Z\",\"First_kpi\":1},"
                                + "{\"date_time\":\"2023-05-03T07:41:45.667776Z\",\"First_kpi\":2},"
                                + "{\"date_time\":\"2023-05-04T08:51:56.788877Z\",\"First_kpi\":3}]}", true));
    }

    @Test
    @DisplayName("Select with count and skip, small table")
    public void get_kpi_with_count_and_skip() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_MIXED + "/kpi_sensitive?$skip=2&$select=date_time,First_kpi&$count=true"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#kpi_sensitive(date_time,First_kpi)\","
                                + "\"@odata.count\":5,\"value\":["
                                + "{\"date_time\":\"2023-05-03T07:41:45.667776Z\",\"First_kpi\":2},"
                                + "{\"date_time\":\"2023-05-04T08:51:56.788877Z\",\"First_kpi\":3},"
                                + "{\"date_time\":\"2023-05-05T09:00:00.820425Z\",\"First_kpi\":4}]}", true));
    }

}
