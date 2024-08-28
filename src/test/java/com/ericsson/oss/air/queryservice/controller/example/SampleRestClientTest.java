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

package com.ericsson.oss.air.queryservice.controller.example;

import static com.ericsson.oss.air.queryservice.controller.example.TestDataEntityMapping.SCHEMA_MIXED;
import static com.ericsson.oss.air.queryservice.controller.example.TestDataEntityMapping.SCHEMA_NEW;
import static com.ericsson.oss.air.queryservice.controller.example.TestDataEntityMapping.SCHEMA_PARTITIONS;
import static com.ericsson.oss.air.queryservice.controller.example.TestDataEntityMapping.SCHEMA_PUBLIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.ericsson.oss.air.queryservice.config.KafkaTestContainersConfiguration;
import com.ericsson.oss.air.queryservice.config.PostgresContainersConfiguration;
import com.ericsson.oss.air.queryservice.service.ExposureChecker;

@SuppressWarnings("checkstyle:MultipleStringLiterals")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import({KafkaTestContainersConfiguration.class, PostgresContainersConfiguration.class})
public class SampleRestClientTest {

    private static final String XPATH_SCHEMA_WITH_NAMESPACE = "Schema[@Namespace=\"%s\"]";
    private static final String XPATH_ENTITY_TYPE_WITH_NAME = "EntityType[@Name=\"%s\"]";
    private static final String XPATH_PROPERTY_WITH_NAME = "Property[@Name=\"%s\"]";
    private static final String XPATH_ENTITY_PROPERTY_TYPE_SELECTOR = "//" + XPATH_SCHEMA_WITH_NAMESPACE
            + "/" + XPATH_ENTITY_TYPE_WITH_NAME + "/" + XPATH_PROPERTY_WITH_NAME + "/@Type";
    private static final String BASE_URL = "/kpi-handling/exposure/v1/";
    private static final String WRONG_BASE_URL = "/api/v1/kpi/";
    private static final String SCHEMA_NOT_EXISTING = "non_existent";

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
    @DisplayName("Metadata for schema: public")
    void get_metadata_public() throws Exception {
        assertThat(TestDataEntityMapping.getNamespaces()).contains(SCHEMA_PUBLIC);
        get_schema_metadata(SCHEMA_PUBLIC);
    }

    @Test
    @DisplayName("Metadata for schema: schema_new")
    void get_metadata_schema_new() throws Exception {
        assertThat(TestDataEntityMapping.getNamespaces()).contains(SCHEMA_NEW);
        get_schema_metadata(SCHEMA_NEW);
    }

    @Test
    @DisplayName("Metadata for schema with partitioned tables")
    void get_metadata_partitioned() throws Exception {
        assertThat(TestDataEntityMapping.getNamespaces()).contains(SCHEMA_PARTITIONS);
        get_schema_metadata(SCHEMA_PARTITIONS);
    }

    @Test
    @DisplayName("Metadata for schema with case sensitive column names")
    void get_metadata_sensitive() throws Exception {
        assertThat(TestDataEntityMapping.getNamespaces()).contains(SCHEMA_MIXED);
        get_schema_metadata(SCHEMA_MIXED);
    }

    @Test
    @DisplayName("Metadata for non-existing schema")
    public void get_metadata_non_existing_schema() throws Exception {
        final String schemaNamespace = "non_existing_schema";

        assertThat(TestDataEntityMapping.getNamespaces()).doesNotContain(schemaNamespace);

        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + schemaNamespace + "/$metadata"))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"error\":{\"code\":\"Not Found\","
                                + "\"message\":\"org.apache.olingo.server.api.ODataApplicationException: Invalid schema\"}}"));
    }

    void get_schema_metadata(final String schemaNamespace) throws Exception {
        final List<String> entityTypes = TestDataEntityMapping.getEntityTypes(schemaNamespace);
        final int schemaEntityCountIncludingPartitions = entityTypes.size();
        final int schemaComplexCount = 0;

        final ResultActions result = mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL + schemaNamespace + "/$metadata"));

        result.andExpectAll(
                MockMvcResultMatchers.status().isOk(),
                MockMvcResultMatchers.xpath("//" + XPATH_SCHEMA_WITH_NAMESPACE, schemaNamespace).exists(),
                MockMvcResultMatchers.xpath("//" + XPATH_SCHEMA_WITH_NAMESPACE
                        + "/EntityType", schemaNamespace).nodeCount(schemaEntityCountIncludingPartitions),
                MockMvcResultMatchers.xpath("//" + XPATH_SCHEMA_WITH_NAMESPACE
                        + "/ComplexType", schemaNamespace).nodeCount(schemaComplexCount)
        );

        for (final String entityType: entityTypes) {
            final Map<String, String> fieldMappings = TestDataEntityMapping.getFieldMappings(schemaNamespace, entityType);

            result.andExpectAll(
                    MockMvcResultMatchers.xpath("//" + XPATH_SCHEMA_WITH_NAMESPACE
                            + "/" + XPATH_ENTITY_TYPE_WITH_NAME, schemaNamespace, entityType).exists(),
                    MockMvcResultMatchers.xpath("//" + XPATH_SCHEMA_WITH_NAMESPACE
                            + "/" + XPATH_ENTITY_TYPE_WITH_NAME
                            + "/Property", schemaNamespace, entityType).nodeCount(fieldMappings.size())
            );

            for (final Map.Entry<String, String> set: fieldMappings.entrySet()) {
                result.andExpect(
                        MockMvcResultMatchers.xpath(XPATH_ENTITY_PROPERTY_TYPE_SELECTOR,
                                schemaNamespace, entityType, set.getKey()).string(set.getValue())
                );
            }
        }
    }

    @Test
    @DisplayName("Malformed url without schema")
    public void get_kpi_malformed_url_no_schema() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @DisplayName("Metadata url with wrong routing parts")
    public void get_kpi_malformed_url_wrong_base() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(WRONG_BASE_URL))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @DisplayName("Malformed url with wrong base url and correct schema")
    public void get_kpi_malformed_url_wrong_base_with_schema() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(WRONG_BASE_URL + SCHEMA_NEW))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"error\":{\"code\":\"Not Found\",\"message\":\"Invalid EntitySet\"}}", true));
    }

    @Test
    @DisplayName("Blocked data modification request")
    public void post_kpi_blocked_request() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.post(BASE_URL).content(""))
                .andExpect(MockMvcResultMatchers.status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("Invalid method (not implemented)")
    public void get_kpi_invalid_method() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$filter=substringof('test', '2') eq true"))
                .andExpect(MockMvcResultMatchers.status().isNotImplemented())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"error\":{\"code\":\"Not Implemented\","
                                + "\"message\":\"Error while processing filter: Method call substringof not implemented\"}}"));
    }

    @Test
    @DisplayName("Not supported filter element: alias")
    public void get_kpi_alias_not_supported() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$filter=-fourth_kpi eq @number&@number=2"))
                .andExpect(MockMvcResultMatchers.status().isNotImplemented())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"error\":{\"code\":\"Not Implemented\","
                                + "\"message\":\"Error while processing filter: Aliases are not implemented\"}}"));
    }

    @Test
    @DisplayName("Not supported filter element: string list")
    public void get_kpi_binary_operator_string_list_not_supported() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$filter='test' in ('asd', 'test')"))
                .andExpect(MockMvcResultMatchers.status().isNotImplemented())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"error\":{\"code\":\"Not Implemented\","
                                + "\"message\":\"Error while processing filter: "
                                + "Binary operators for string lists are not implemented\"}}"));
    }

    @Test
    @DisplayName("Non-existent schema")
    public void get_kpi_non_existent_schema() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL + SCHEMA_NOT_EXISTING + "/new_table"))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @DisplayName("Non-existent table")
    public void get_kpi_non_existent_table() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL + SCHEMA_NEW + "/non_existent_table"))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"error\":{\"code\":\"Not Found\",\"message\":\"Invalid EntitySet\"}}", true));
    }

    @Test
    @DisplayName("Simple order by clause")
    public void get_kpi_with_order_by() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$orderby=date_time desc"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table\","
                                + "\"value\": [{\"date_time\": \"2022-03-01T10:07:38.206333Z\","
                                + "\"first_kpi\": -32768,\"second_kpi\": -2147483648,"
                                + "\"third_kpi\": -9223372036854775808,\"fourth_kpi\": 12345.6789,"
                                + "\"fifth_kpi\": 12345.67890,\"sixth_kpi\": 987.6543,"
                                + "\"seventh_kpi\": 1.2345678901234567E9}, "
                                + "{\"date_time\": \"2022-03-01T09:23:02.206333Z\",\"first_kpi\": 32767,"
                                + "\"second_kpi\": 2147483647,\"third_kpi\": 9223372036854775807,"
                                + "\"fourth_kpi\": -12345.6789,\"fifth_kpi\": -12345.67890,"
                                + "\"sixth_kpi\": -987.6543,\"seventh_kpi\": -1.2345678901234567E9},"
                                + "{\"date_time\": \"2022-02-10T21:42:01.206333Z\",\"first_kpi\": 0,"
                                + "\"second_kpi\": 21474,\"third_kpi\": 92236854775807,\"fourth_kpi\": -1235.6790,"
                                + "\"fifth_kpi\": -1235.69000,\"sixth_kpi\": -98.6521,"
                                + "\"seventh_kpi\": -1.2347890123456713E7}]}"));
    }

    @Test
    @DisplayName("Simple top query option")
    public void get_kpi_with_top() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL + SCHEMA_PUBLIC + "/first_table?$top=2"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\": \"$metadata#first_table\","
                                + "\"value\": [{\"date_time\": \"2022-03-01T10:07:38.206333Z\","
                                + "\"first_kpi\": -32768,\"second_kpi\": -2147483648,\"third_kpi\": -9223372036854775808,"
                                + "\"fourth_kpi\": 12345.6789,\"fifth_kpi\": 12345.67890,"
                                + "\"sixth_kpi\": 987.6543,\"seventh_kpi\": 1.2345678901234567E9}, "
                                + "{\"date_time\": \"2022-03-01T09:23:02.206333Z\",\"first_kpi\": 32767,"
                                + "\"second_kpi\": 2147483647,\"third_kpi\": 9223372036854775807,"
                                + "\"fourth_kpi\": -12345.6789,\"fifth_kpi\": -12345.67890,\"sixth_kpi\": -987.6543,"
                                + "\"seventh_kpi\": -1.2345678901234567E9}]}"));
    }

    @Test
    @DisplayName("Simple skip query option")
    public void get_kpi_with_skip() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL + SCHEMA_PUBLIC + "/first_table?$skip=2"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\": \"$metadata#first_table\","
                                + "\"value\": [{\"date_time\": \"2022-02-10T21:42:01.206333Z\","
                                + "\"first_kpi\": 0,\"second_kpi\": 21474,\"third_kpi\": 92236854775807,"
                                + "\"fourth_kpi\": -1235.6790,\"fifth_kpi\": -1235.69000,\"sixth_kpi\": -98.6521,"
                                + "\"seventh_kpi\": -1.2347890123456713E7}]}"));
    }

    @Test
    @DisplayName("Filter with arithmetic operator: add")
    public void get_kpi_with_filter_arithmetic_operator_add() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$filter=first_kpi add fourth_kpi eq -1235.6790"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table\","
                                + "\"value\":[{\"date_time\":\"2022-02-10T21:42:01.206333Z\","
                                + "\"first_kpi\":0,\"second_kpi\":21474,\"third_kpi\":92236854775807,"
                                + "\"fourth_kpi\":-1235.6790,\"fifth_kpi\":-1235.69000,\"sixth_kpi\":-98.6521,"
                                + "\"seventh_kpi\":-1.2347890123456713E7}]}"));
    }

    @Test
    @DisplayName("Filter with arithmetic operator: sub")
    public void get_kpi_with_filter_arithmetic_operator_sub() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$filter=second_kpi sub first_kpi eq 21474"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table\","
                                + "\"value\":[{\"date_time\":\"2022-02-10T21:42:01.206333Z\","
                                + "\"first_kpi\":0,\"second_kpi\":21474,\"third_kpi\":92236854775807,"
                                + "\"fourth_kpi\":-1235.6790,\"fifth_kpi\":-1235.69000,"
                                + "\"sixth_kpi\":-98.6521,\"seventh_kpi\":-1.2347890123456713E7}]}"));
    }

    @Test
    @DisplayName("Filter with arithmetic operator: mul")
    public void get_kpi_with_filter_arithmetic_operator_mul() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$filter=first_kpi mul fourth_kpi eq 0"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table\","
                                + "\"value\":[{\"date_time\":\"2022-02-10T21:42:01.206333Z\","
                                + "\"first_kpi\":0,\"second_kpi\":21474,\"third_kpi\":92236854775807,"
                                + "\"fourth_kpi\":-1235.6790,\"fifth_kpi\":-1235.69000,\"sixth_kpi\":-98.6521,"
                                + "\"seventh_kpi\":-1.2347890123456713E7}]}"));
    }

    @Test
    @DisplayName("Filter with arithmetic operator: div")
    public void get_kpi_with_filter_arithmetic_operator_div() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$filter=first_kpi div fourth_kpi eq 0"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table\","
                                + "\"value\":[{\"date_time\":\"2022-02-10T21:42:01.206333Z\","
                                + "\"first_kpi\":0,\"second_kpi\":21474,\"third_kpi\":92236854775807,"
                                + "\"fourth_kpi\":-1235.6790,\"fifth_kpi\":-1235.69000,\"sixth_kpi\":-98.6521,"
                                + "\"seventh_kpi\":-1.2347890123456713E7}]}"));
    }

    @Test
    @DisplayName("Filter with arithmetic operator: mod")
    public void get_kpi_with_filter_logical_operator_mod() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$filter=first_kpi mod fourth_kpi eq 0"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table\","
                                + "\"value\":[{\"date_time\":\"2022-02-10T21:42:01.206333Z\","
                                + "\"first_kpi\":0,\"second_kpi\":21474,\"third_kpi\":92236854775807,"
                                + "\"fourth_kpi\":-1235.6790,\"fifth_kpi\":-1235.69000,\"sixth_kpi\":-98.6521,"
                                + "\"seventh_kpi\":-1.2347890123456713E7}]}"));
    }

    @Test
    @DisplayName("Filter with logical operator: not-equal")
    public void get_kpi_with_filter_logical_operator_ne() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$filter=first_kpi mod fourth_kpi ne 0"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table\","
                                + "\"value\":[{\"date_time\":\"2022-03-01T10:07:38.206333Z\",\"first_kpi\":-32768,"
                                + "\"second_kpi\":-2147483648,\"third_kpi\":-9223372036854775808,\"fourth_kpi\":12345.6789,"
                                + "\"fifth_kpi\":12345.67890,\"sixth_kpi\":987.6543,\"seventh_kpi\":1.2345678901234567E9},"
                                + "{\"date_time\":\"2022-03-01T09:23:02.206333Z\",\"first_kpi\":32767,\"second_kpi\":2147483647,"
                                + "\"third_kpi\":9223372036854775807,\"fourth_kpi\":-12345.6789,\"fifth_kpi\":-12345.67890,"
                                + "\"sixth_kpi\":-987.6543,\"seventh_kpi\":-1.2345678901234567E9}]}"));
    }

    @Test
    @DisplayName("Filter with logical operator: greater-or-equal")
    public void get_kpi_with_filter_logical_operator_ge() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$filter=first_kpi mod fourth_kpi ge 0"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table\","
                                + "\"value\":[{\"date_time\":\"2022-03-01T09:23:02.206333Z\",\"first_kpi\":32767,"
                                + "\"second_kpi\":2147483647,\"third_kpi\":9223372036854775807,\"fourth_kpi\":-12345.6789,"
                                + "\"fifth_kpi\":-12345.67890,\"sixth_kpi\":-987.6543,\"seventh_kpi\":-1.2345678901234567E9},"
                                + "{\"date_time\":\"2022-02-10T21:42:01.206333Z\",\"first_kpi\":0,\"second_kpi\":21474,"
                                + "\"third_kpi\":92236854775807,\"fourth_kpi\":-1235.6790,\"fifth_kpi\":-1235.69000,"
                                + "\"sixth_kpi\":-98.6521,\"seventh_kpi\":-1.2347890123456713E7}]}"));
    }

    @Test
    @DisplayName("Filter with logical operator: greater-then")
    public void get_kpi_with_filter_logical_operator_gt() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$filter=first_kpi mod fourth_kpi gt 0"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table\","
                                + "\"value\":[{\"date_time\":\"2022-03-01T09:23:02.206333Z\",\"first_kpi\":32767,"
                                + "\"second_kpi\":2147483647,\"third_kpi\":9223372036854775807,\"fourth_kpi\":-12345.6789,"
                                + "\"fifth_kpi\":-12345.67890,\"sixth_kpi\":-987.6543,\"seventh_kpi\":-1.2345678901234567E9}]}"));
    }

    @Test
    @DisplayName("Filter with logical operator: less-or-equal")
    public void get_kpi_with_filter_logical_operator_le() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$filter=first_kpi mod fourth_kpi le 0"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table\","
                                + "\"value\":[{\"date_time\":\"2022-03-01T10:07:38.206333Z\",\"first_kpi\":-32768,"
                                + "\"second_kpi\":-2147483648,\"third_kpi\":-9223372036854775808,\"fourth_kpi\":12345.6789,"
                                + "\"fifth_kpi\":12345.67890,\"sixth_kpi\":987.6543,\"seventh_kpi\":1.2345678901234567E9},"
                                + "{\"date_time\":\"2022-02-10T21:42:01.206333Z\",\"first_kpi\":0,\"second_kpi\":21474,"
                                + "\"third_kpi\":92236854775807,\"fourth_kpi\":-1235.6790,\"fifth_kpi\":-1235.69000,"
                                + "\"sixth_kpi\":-98.6521,\"seventh_kpi\":-1.2347890123456713E7}]}"));
    }

    @Test
    @DisplayName("Filter with logical operator: less-than")
    public void get_kpi_with_filter_logical_operator_lt() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$filter=first_kpi mod fourth_kpi lt 0"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table\","
                                + "\"value\":[{\"date_time\":\"2022-03-01T10:07:38.206333Z\",\"first_kpi\":-32768,"
                                + "\"second_kpi\":-2147483648,\"third_kpi\":-9223372036854775808,\"fourth_kpi\":12345.6789,"
                                + "\"fifth_kpi\":12345.67890,\"sixth_kpi\":987.6543,\"seventh_kpi\":1.2345678901234567E9}]}"));
    }

    @Test
    @DisplayName("Filter with logical operator: and")
    public void get_kpi_with_filter_logical_operator_and() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$filter=first_kpi eq 0 and second_kpi ge 21474"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table\","
                                + "\"value\":[{\"date_time\":\"2022-02-10T21:42:01.206333Z\",\"first_kpi\":0,"
                                + "\"second_kpi\":21474,\"third_kpi\":92236854775807,\"fourth_kpi\":-1235.6790,"
                                + "\"fifth_kpi\":-1235.69000,\"sixth_kpi\":-98.6521,\"seventh_kpi\":-1.2347890123456713E7}]}"));
    }

    @Test
    @DisplayName("Filter with logical operator: or")
    public void get_kpi_with_filter_logical_operator_or() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$filter=first_kpi eq 0 or second_kpi ge 21474"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table\","
                                + "\"value\":[{\"date_time\":\"2022-03-01T09:23:02.206333Z\",\"first_kpi\":32767,"
                                + "\"second_kpi\":2147483647,\"third_kpi\":9223372036854775807,\"fourth_kpi\":-12345.6789,"
                                + "\"fifth_kpi\":-12345.67890,\"sixth_kpi\":-987.6543,\"seventh_kpi\":-1.2345678901234567E9},"
                                + "{\"date_time\":\"2022-02-10T21:42:01.206333Z\",\"first_kpi\":0,\"second_kpi\":21474,"
                                + "\"third_kpi\":92236854775807,\"fourth_kpi\":-1235.6790,\"fifth_kpi\":-1235.69000,"
                                + "\"sixth_kpi\":-98.6521,\"seventh_kpi\":-1.2347890123456713E7}]}"));
    }

    @Test
    @DisplayName("Filter with unary operator: not")
    public void get_kpi_with_filter_unary_operator_not() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$filter=not (second_kpi eq 21474)"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table\","
                                + "\"value\":[{\"date_time\":\"2022-03-01T10:07:38.206333Z\",\"first_kpi\":-32768,"
                                + "\"second_kpi\":-2147483648,\"third_kpi\":-9223372036854775808,\"fourth_kpi\":12345.6789,"
                                + "\"fifth_kpi\":12345.67890,\"sixth_kpi\":987.6543,\"seventh_kpi\":1.2345678901234567E9},"
                                + "{\"date_time\":\"2022-03-01T09:23:02.206333Z\",\"first_kpi\":32767,\"second_kpi\":2147483647,"
                                + "\"third_kpi\":9223372036854775807,\"fourth_kpi\":-12345.6789,\"fifth_kpi\":-12345.67890,"
                                + "\"sixth_kpi\":-987.6543,\"seventh_kpi\":-1.2345678901234567E9}]}"));
    }

    @Test
    @DisplayName("Filter with unary operator: minus")
    public void get_kpi_with_filter_unary_operator_minus() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$filter=first_kpi sub -fourth_kpi gt 1235.6790"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table\","
                                + "\"value\":[{\"date_time\":\"2022-03-01T09:23:02.206333Z\",\"first_kpi\":32767,"
                                + "\"second_kpi\":2147483647,\"third_kpi\":9223372036854775807,\"fourth_kpi\":-12345.6789,"
                                + "\"fifth_kpi\":-12345.67890,\"sixth_kpi\":-987.6543,\"seventh_kpi\":-1.2345678901234567E9}]}"));
    }

    @Test
    @DisplayName("Filter with string function: concat")
    public void get_kpi_with_filter_string_function_concat() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/second_table?$filter=concat('asd', fourth_kpi) eq 'asdasd'"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#second_table\","
                                + "\"value\":[{\"date_time\":\"2022-03-01T10:07:38.206333Z\",\"first_kpi\":9223372036854775807,"
                                + "\"second_kpi\":-2147483648,\"third_kpi\":-12345.67890,\"fourth_kpi\":\"asd\"}]}"));
    }

    @Test
    @DisplayName("Filter with string function: contains")
    public void get_kpi_with_filter_string_function_contains() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL + SCHEMA_PUBLIC + "/second_table?$filter=contains(fourth_kpi, 'sd')"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#second_table\","
                                + "\"value\":[{\"date_time\":\"2022-03-01T10:07:38.206333Z\",\"first_kpi\":9223372036854775807,"
                                + "\"second_kpi\":-2147483648,\"third_kpi\":-12345.67890,\"fourth_kpi\":\"asd\"}]}"));
    }

    @Test
    @DisplayName("Filter with string function: startswith")
    public void get_kpi_with_filter_string_function_startswith() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/second_table?$filter=startswith(fourth_kpi,'q')"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#second_table\","
                                + "\"value\":[{\"date_time\":\"2022-03-02T07:30:38.206333Z\",\"first_kpi\":56546234234,"
                                + "\"second_kpi\":-545334,\"third_kpi\":86955.48300,\"fourth_kpi\":\"qwertz\"}]}"));
    }

    @Test
    @DisplayName("Filter with string function: endswith")
    public void get_kpi_with_filter_string_function_endswith() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/second_table?$filter=endswith(fourth_kpi,'tz')"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#second_table\","
                                + "\"value\":[{\"date_time\":\"2022-03-02T07:30:38.206333Z\",\"first_kpi\":56546234234,"
                                + "\"second_kpi\":-545334,\"third_kpi\":86955.48300,\"fourth_kpi\":\"qwertz\"}]}"));
    }

    @Test
    @DisplayName("Filter with string function: indexof")
    public void get_kpi_with_filter_string_function_indexof() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/second_table?$filter=indexof(fourth_kpi, 'sd') eq 2"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#second_table\","
                                + "\"value\":[{\"date_time\":\"2022-03-01T10:07:38.206333Z\",\"first_kpi\":9223372036854775807,"
                                + "\"second_kpi\":-2147483648,\"third_kpi\":-12345.67890,\"fourth_kpi\":\"asd\"}]}"));
    }

    @Test
    @DisplayName("Filter with string function: length")
    public void get_kpi_with_filter_string_function_length() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/second_table?$filter=length(fourth_kpi) eq 3"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#second_table\","
                                + "\"value\":[{\"date_time\":\"2022-03-01T10:07:38.206333Z\",\"first_kpi\":9223372036854775807,"
                                + "\"second_kpi\":-2147483648,\"third_kpi\":-12345.67890,\"fourth_kpi\":\"asd\"}]}"));
    }

    @Test
    @DisplayName("Filter with string function: substring and one parameter")
    public void get_kpi_with_filter_string_function_substring_one_parameter() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/second_table?$filter=substring(fourth_kpi, 2) eq 'sd'"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#second_table\","
                                + "\"value\":[{\"date_time\":\"2022-03-01T10:07:38.206333Z\",\"first_kpi\":9223372036854775807,"
                                + "\"second_kpi\":-2147483648,\"third_kpi\":-12345.67890,\"fourth_kpi\":\"asd\"}]}"));
    }

    @Test
    @DisplayName("Filter with string function: substring and two parameters")
    public void get_kpi_with_filter_string_function_substring_two_parameters() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/second_table?$filter=substring(fourth_kpi, 2, 4) eq 'wert'"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#second_table\","
                                + "\"value\":[{\"date_time\":\"2022-03-02T07:30:38.206333Z\",\"first_kpi\":56546234234,"
                                + "\"second_kpi\":-545334,\"third_kpi\":86955.48300,\"fourth_kpi\":\"qwertz\"}]}"));
    }

    @Test
    @DisplayName("Filter with string function: tolower")
    public void get_kpi_with_filter_string_function_tolower() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/second_table?$filter=tolower('ASD') eq fourth_kpi"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#second_table\","
                                + "\"value\":[{\"date_time\":\"2022-03-01T10:07:38.206333Z\",\"first_kpi\":9223372036854775807,"
                                + "\"second_kpi\":-2147483648,\"third_kpi\":-12345.67890,\"fourth_kpi\":\"asd\"}]}"));
    }

    @Test
    @DisplayName("Filter with string function: toupper")
    public void get_kpi_with_filter_string_function_toupper() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/second_table?$filter=toupper(fourth_kpi) eq 'ASD'"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#second_table\","
                                + "\"value\":[{\"date_time\":\"2022-03-01T10:07:38.206333Z\",\"first_kpi\":9223372036854775807,"
                                + "\"second_kpi\":-2147483648,\"third_kpi\":-12345.67890,\"fourth_kpi\":\"asd\"}]}"));
    }

    @Test
    @DisplayName("Filter with string function: trim")
    public void get_kpi_with_filter_string_function_trim() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/second_table?$filter=trim(' asd ') eq fourth_kpi"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#second_table\","
                                + "\"value\":[{\"date_time\":\"2022-03-01T10:07:38.206333Z\",\"first_kpi\":9223372036854775807,"
                                + "\"second_kpi\":-2147483648,\"third_kpi\":-12345.67890,\"fourth_kpi\":\"asd\"}]}"));
    }

    @Test
    @DisplayName("Filter with number function: ceiling")
    public void get_kpi_with_filter_number_function_ceiling() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/second_table?$filter=ceiling(third_kpi) eq 86956"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#second_table\","
                                + "\"value\":[{\"date_time\":\"2022-03-02T07:30:38.206333Z\",\"first_kpi\":56546234234,"
                                + "\"second_kpi\":-545334,\"third_kpi\":86955.48300,\"fourth_kpi\":null},"
                                + "{\"date_time\":\"2022-03-02T07:30:38.206333Z\",\"first_kpi\":56546234234,"
                                + "\"second_kpi\":-545334,\"third_kpi\":86955.48300,\"fourth_kpi\":\"qwertz\"}]}"));
    }

    @Test
    @DisplayName("Filter with number function: floor")
    public void get_kpi_with_filter_number_function_floor() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/second_table?$filter=floor(third_kpi) eq 86955"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#second_table\","
                                + "\"value\":[{\"date_time\":\"2022-03-02T07:30:38.206333Z\",\"first_kpi\":56546234234,"
                                + "\"second_kpi\":-545334,\"third_kpi\":86955.48300,\"fourth_kpi\":null},"
                                + "{\"date_time\":\"2022-03-02T07:30:38.206333Z\",\"first_kpi\":56546234234,"
                                + "\"second_kpi\":-545334,\"third_kpi\":86955.48300,\"fourth_kpi\":\"qwertz\"}]}"));
    }

    @Test
    @DisplayName("Filter with number function: round")
    public void get_kpi_with_filter_number_function_round() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/second_table?$filter=round(third_kpi) eq 86955"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#second_table\","
                                + "\"value\":[{\"date_time\":\"2022-03-02T07:30:38.206333Z\",\"first_kpi\":56546234234,"
                                + "\"second_kpi\":-545334,\"third_kpi\":86955.48300,\"fourth_kpi\":null},"
                                + "{\"date_time\":\"2022-03-02T07:30:38.206333Z\",\"first_kpi\":56546234234,"
                                + "\"second_kpi\":-545334,\"third_kpi\":86955.48300,\"fourth_kpi\":\"qwertz\"}]}"));
    }

    @Test
    public void get_kpi_with_filter_date_function_date() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$filter=date(date_time) eq 2022-03-01"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table\","
                                + "\"value\":[{\"date_time\":\"2022-03-01T10:07:38.206333Z\",\"first_kpi\":-32768,"
                                + "\"second_kpi\":-2147483648,\"third_kpi\":-9223372036854775808,\"fourth_kpi\":12345.6789,"
                                + "\"fifth_kpi\":12345.67890,\"sixth_kpi\":987.6543,\"seventh_kpi\":1.2345678901234567E9},"
                                + "{\"date_time\":\"2022-03-01T09:23:02.206333Z\",\"first_kpi\":32767,\"second_kpi\":2147483647,"
                                + "\"third_kpi\":9223372036854775807,\"fourth_kpi\":-12345.6789,\"fifth_kpi\":-12345.67890,"
                                + "\"sixth_kpi\":-987.6543,\"seventh_kpi\":-1.2345678901234567E9}]}", true));
    }

    @Test
    @DisplayName("Filter with date function: day")
    public void get_kpi_with_filter_date_function_day() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$filter=day(date_time) eq 10"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table\","
                                + "\"value\":[{\"date_time\":\"2022-02-10T21:42:01.206333Z\",\"first_kpi\":0,"
                                + "\"second_kpi\":21474,\"third_kpi\":92236854775807,\"fourth_kpi\":-1235.6790,"
                                + "\"fifth_kpi\":-1235.69000,\"sixth_kpi\":-98.6521,\"seventh_kpi\":-1.2347890123456713E7}]}",
                        true));
    }

    @Test
    @DisplayName("Filter with date function: fractionalseconds")
    public void get_kpi_with_filter_date_function_fractionalseconds() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$filter=fractionalseconds(date_time) eq 0.206"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table\","
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
    @DisplayName("Filter with date function: hour")
    public void get_kpi_with_filter_date_function_hour() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$filter=hour(date_time) eq 10"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table\","
                                + "\"value\":[{\"date_time\":\"2022-03-01T09:23:02.206333Z\",\"first_kpi\":32767,"
                                + "\"second_kpi\":2147483647,\"third_kpi\":9223372036854775807,\"fourth_kpi\":-12345.6789,"
                                + "\"fifth_kpi\":-12345.67890,\"sixth_kpi\":-987.6543,"
                                + "\"seventh_kpi\":-1.2345678901234567E9}]}", true));
    }

    @Test
    @DisplayName("Filter with date function: maxdatetime")
    public void get_kpi_with_filter_date_function_maxdatetime() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$filter=date_time lt maxdatetime()"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table\","
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
    @DisplayName("Filter with date function: mindatetime")
    public void get_kpi_with_filter_date_function_mindatetime() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$filter=date_time lt mindatetime()"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table\",\"value\":[]}", true));
    }

    @Test
    @DisplayName("Filter with date function: minute")
    public void get_kpi_with_filter_date_function_minute() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$filter=minute(date_time) eq 23"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table\","
                                + "\"value\":[{\"date_time\":\"2022-03-01T09:23:02.206333Z\",\"first_kpi\":32767,"
                                + "\"second_kpi\":2147483647,\"third_kpi\":9223372036854775807,\"fourth_kpi\":-12345.6789,"
                                + "\"fifth_kpi\":-12345.67890,\"sixth_kpi\":-987.6543,"
                                + "\"seventh_kpi\":-1.2345678901234567E9}]}", true));
    }

    @Test
    @DisplayName("Filter with date function: month")
    public void get_kpi_with_filter_date_function_month() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$filter=month(date_time) eq 2"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table\","
                                + "\"value\":[{\"date_time\":\"2022-02-10T21:42:01.206333Z\",\"first_kpi\":0,"
                                + "\"second_kpi\":21474,\"third_kpi\":92236854775807,\"fourth_kpi\":-1235.6790,"
                                + "\"fifth_kpi\":-1235.69000,\"sixth_kpi\":-98.6521,"
                                + "\"seventh_kpi\":-1.2347890123456713E7}]}", true));
    }

    @Test
    @DisplayName("Filter with date function: now")
    public void get_kpi_with_filter_date_function_now() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$filter=date_time eq now()"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table\",\"value\":[]}", true));
    }

    @Test
    @DisplayName("Filter with date function: second")
    public void get_kpi_with_filter_date_function_second() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$filter=second(date_time) eq 1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table\","
                                + "\"value\":[{\"date_time\":\"2022-02-10T21:42:01.206333Z\",\"first_kpi\":0,"
                                + "\"second_kpi\":21474,\"third_kpi\":92236854775807,\"fourth_kpi\":-1235.6790,"
                                + "\"fifth_kpi\":-1235.69000,\"sixth_kpi\":-98.6521,"
                                + "\"seventh_kpi\":-1.2347890123456713E7}]}", true));
    }

    @Test
    @DisplayName("Filter with date function: time")
    public void get_kpi_with_filter_date_function_time() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$filter=time(date_time) eq 10:23:02.206333"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table\","
                                + "\"value\":[{\"date_time\":\"2022-03-01T09:23:02.206333Z\",\"first_kpi\":32767,"
                                + "\"second_kpi\":2147483647,\"third_kpi\":9223372036854775807,\"fourth_kpi\":-12345.6789,"
                                + "\"fifth_kpi\":-12345.67890,\"sixth_kpi\":-987.6543,"
                                + "\"seventh_kpi\":-1.2345678901234567E9}]}", true));
    }

    @Test
    @DisplayName("Filter with date function: totaloffsetminutes")
    public void get_kpi_with_filter_date_function_totaloffsetminutes() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$filter=totaloffsetminutes(date_time) eq 0"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table\","
                                + "\"value\":[{\"date_time\":\"2022-03-01T10:07:38.206333Z\",\"first_kpi\":-32768,"
                                + "\"second_kpi\":-2147483648,\"third_kpi\":-9223372036854775808,\"fourth_kpi\":12345.6789,"
                                + "\"fifth_kpi\":12345.67890,\"sixth_kpi\":987.6543,\"seventh_kpi\":1.2345678901234567E9},"
                                + "{\"date_time\":\"2022-03-01T09:23:02.206333Z\",\"first_kpi\":32767,"
                                + "\"second_kpi\":2147483647,\"third_kpi\":9223372036854775807,\"fourth_kpi\":-12345.6789,"
                                + "\"fifth_kpi\":-12345.67890,\"sixth_kpi\":-987.6543,\"seventh_kpi\":-1.2345678901234567E9},"
                                + "{\"date_time\":\"2022-02-10T21:42:01.206333Z\",\"first_kpi\":0,\"second_kpi\":21474,"
                                + "\"third_kpi\":92236854775807,\"fourth_kpi\":-1235.6790,\"fifth_kpi\":-1235.69000,"
                                + "\"sixth_kpi\":-98.6521,\"seventh_kpi\":-1.2347890123456713E7}]}", true));
    }

    @Test
    @DisplayName("Filter with date function: totalseconds")
    public void get_kpi_with_filter_date_function_totalseconds() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$filter=totalseconds(duration'-P1DT1H0M0.1S') eq 0"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table\",\"value\":[]}", true));
    }

    @Test
    @DisplayName("Filter with date function: year")
    public void get_kpi_with_filter_date_function_year() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$filter=year(date_time) eq 2022"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table\","
                                + "\"value\":[{\"date_time\":\"2022-03-01T10:07:38.206333Z\",\"first_kpi\":-32768,"
                                + "\"second_kpi\":-2147483648,\"third_kpi\":-9223372036854775808,\"fourth_kpi\":12345.6789,"
                                + "\"fifth_kpi\":12345.67890,\"sixth_kpi\":987.6543,\"seventh_kpi\":1.2345678901234567E9},"
                                + "{\"date_time\":\"2022-03-01T09:23:02.206333Z\",\"first_kpi\":32767,"
                                + "\"second_kpi\":2147483647,\"third_kpi\":9223372036854775807,\"fourth_kpi\":-12345.6789,"
                                + "\"fifth_kpi\":-12345.67890,\"sixth_kpi\":-987.6543,\"seventh_kpi\":-1.2345678901234567E9},"
                                + "{\"date_time\":\"2022-02-10T21:42:01.206333Z\",\"first_kpi\":0,\"second_kpi\":21474,"
                                + "\"third_kpi\":92236854775807,\"fourth_kpi\":-1235.6790,\"fifth_kpi\":-1235.69000,"
                                + "\"sixth_kpi\":-98.6521,\"seventh_kpi\":-1.2347890123456713E7}]}", true));
    }

    @Test
    @DisplayName("Simple select with count")
    public void get_kpi_with_count() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$count=true"))
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
    @DisplayName("Select with count and filter")
    public void get_kpi_with_count_and_filter() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$filter=second(date_time) eq 1 or minute(date_time) eq 23&$count=true"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table\",\"@odata.count\":2,"
                                + "\"value\":[{\"date_time\":\"2022-03-01T09:23:02.206333Z\",\"first_kpi\":32767,"
                                + "\"second_kpi\":2147483647,\"third_kpi\":9223372036854775807,\"fourth_kpi\":-12345.6789,"
                                + "\"fifth_kpi\":-12345.67890,\"sixth_kpi\":-987.6543,\"seventh_kpi\":-1.2345678901234567E9},"
                                + "{\"date_time\":\"2022-02-10T21:42:01.206333Z\",\"first_kpi\":0,\"second_kpi\":21474,"
                                + "\"third_kpi\":92236854775807,\"fourth_kpi\":-1235.6790,\"fifth_kpi\":-1235.69000,"
                                + "\"sixth_kpi\":-98.6521,\"seventh_kpi\":-1.2347890123456713E7}]}", true));
    }

    @Test
    @DisplayName("Select with count and top")
    public void get_kpi_with_count_and_top() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$count=true&$top=2"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table\",\"@odata.count\":3,"
                                + "\"value\":[{\"date_time\":\"2022-03-01T10:07:38.206333Z\",\"first_kpi\":-32768,"
                                + "\"second_kpi\":-2147483648,\"third_kpi\":-9223372036854775808,\"fourth_kpi\":12345.6789,"
                                + "\"fifth_kpi\":12345.67890,\"sixth_kpi\":987.6543,\"seventh_kpi\":1.2345678901234567E9},"
                                + "{\"date_time\":\"2022-03-01T09:23:02.206333Z\",\"first_kpi\":32767,\"second_kpi\":2147483647,"
                                + "\"third_kpi\":9223372036854775807,\"fourth_kpi\":-12345.6789,\"fifth_kpi\":-12345.67890,"
                                + "\"sixth_kpi\":-987.6543,\"seventh_kpi\":-1.2345678901234567E9}]}", true));
    }

    @Test
    @DisplayName("Select with count and skip")
    public void get_kpi_with_count_and_skip() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$count=true&$skip=2"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table\",\"@odata.count\":3,"
                                + "\"value\":[{\"date_time\":\"2022-02-10T21:42:01.206333Z\",\"first_kpi\":0,"
                                + "\"second_kpi\":21474,\"third_kpi\":92236854775807,\"fourth_kpi\":-1235.6790,"
                                + "\"fifth_kpi\":-1235.69000,\"sixth_kpi\":-98.6521,"
                                + "\"seventh_kpi\":-1.2347890123456713E7}]}", true));
    }

    @Test
    @DisplayName("Select with count and top and skip")
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
    @DisplayName("Simple select with one column")
    public void get_kpi_select_one_column() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$select=date_time"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table(date_time)\","
                                + "\"value\":[{\"date_time\":\"2022-03-01T10:07:38.206333Z\"},"
                                + "{\"date_time\":\"2022-03-01T09:23:02.206333Z\"},"
                                + "{\"date_time\":\"2022-02-10T21:42:01.206333Z\"}]}", true));
    }

    @Test
    @DisplayName("Simple select with multiple columns")
    public void get_kpi_select_multi_column() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_PUBLIC + "/first_table?$select=date_time,first_kpi"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#first_table(date_time,first_kpi)\","
                                + "\"value\":[{\"date_time\":\"2022-03-01T10:07:38.206333Z\",\"first_kpi\":-32768},"
                                + "{\"date_time\":\"2022-03-01T09:23:02.206333Z\",\"first_kpi\":32767},"
                                + "{\"date_time\":\"2022-02-10T21:42:01.206333Z\",\"first_kpi\":0}]}", true));
    }

    @Test
    @DisplayName("Simple select without query options")
    public void get_kpi_new_schema_all() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_NEW + "/new_table"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#new_table\","
                                + "\"value\":[{\"date_time\":\"2022-03-01T10:07:38.206333Z\",\"first_kpi\":45,"
                                + "\"second_kpi\":468221,\"third_kpi\":33465.00000,\"fourth_kpi\":\"new_kpi\"},"
                                + "{\"date_time\":\"2022-03-01T10:07:38.206333Z\",\"first_kpi\":26,\"second_kpi\":218221,"
                                + "\"third_kpi\":89346.00000,\"fourth_kpi\":\"old_kpi\"}]}", true));
    }

    @Test
    @DisplayName("Simple select with one column from schema_new")
    public void get_kpi_new_schema_select() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL + SCHEMA_NEW + "/new_table?$select=first_kpi"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#new_table(first_kpi)\","
                                + "\"value\":[{\"first_kpi\":45},{\"first_kpi\":26}]}", true));
    }

    @Test
    @DisplayName("Simple select with count from schema_new")
    public void get_kpi_new_schema_count() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL + SCHEMA_NEW + "/new_table?$count=true"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#new_table\","
                                + "\"@odata.count\":2,\"value\":[{\"date_time\":\"2022-03-01T10:07:38.206333Z\","
                                + "\"first_kpi\":45,\"second_kpi\":468221,\"third_kpi\":33465.00000,\"fourth_kpi\":\"new_kpi\"},"
                                + "{\"date_time\":\"2022-03-01T10:07:38.206333Z\",\"first_kpi\":26,\"second_kpi\":218221,"
                                + "\"third_kpi\":89346.00000,\"fourth_kpi\":\"old_kpi\"}]}", true));
    }

    @Test
    @DisplayName("Simple select with ordering from schema_new")
    public void get_kpi_new_schema_order_by() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL + SCHEMA_NEW + "/new_table?$orderby=fourth_kpi desc"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#new_table\","
                                + "\"value\":[{\"date_time\":\"2022-03-01T10:07:38.206333Z\",\"first_kpi\":26,"
                                + "\"second_kpi\":218221,\"third_kpi\":89346.00000,\"fourth_kpi\":\"old_kpi\"},"
                                + "{\"date_time\":\"2022-03-01T10:07:38.206333Z\",\"first_kpi\":45,\"second_kpi\":468221,"
                                + "\"third_kpi\":33465.00000,\"fourth_kpi\":\"new_kpi\"}]}", true));
    }

    @Test
    @DisplayName("Simple select with filtering from schema_new")
    public void get_kpi_new_schema_filter() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_NEW + "/new_table?$filter=first_kpi lt 40 and startswith(fourth_kpi, 'old')"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#new_table\","
                                + "\"value\":[{\"date_time\":\"2022-03-01T10:07:38.206333Z\",\"first_kpi\":26,"
                                + "\"second_kpi\":218221,\"third_kpi\":89346.00000,\"fourth_kpi\":\"old_kpi\"}]}", true));
    }

    @Test
    @DisplayName("Simple select with many postgres data types")
    public void get_kpi_all_postgres_types() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL + SCHEMA_NEW + "/table_all_types"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#table_all_types\","
                                + "\"value\":[{\"c_1\":1,\"c_2\":3,\"c_3\":true,\"c_4\":\"101\",\"c_5\":true,\"c_6\":\"(3.0,5.0),"
                                + "(1.0,2.0)\",\"c_7\":\"AT19FtetT++2G9lbdlyM6w==\",\"c_8\":\"chr\",\"c_9\":\"test\","
                                + "\"c_10\":\"192.168.0.0/16\",\"c_11\":\"<(1.2,123.1),10.0>\",\"c_12\":\"2013-06-01\","
                                + "\"c_13\":99.2,\"c_14\":\"192.168.2.1/24\",\"c_15\":23,"
                                + "\"c_16\":\"0 years 0 mons 0 days 0 hours 40 mins 0.0 secs\","
                                + "\"c_17\":\"{\\\"prop\\\": \\\"value\\\"}\",\"c_18\":\"{\\\"prop\\\": \\\"value\\\"}\","
                                + "\"c_19\":\"{1.0,-1.0,0.0}\",\"c_20\":\"[(1.0,1.0),(2.0,2.0)]\",\"c_21\":\"08:00:2b:01:02:03\","
                                + "\"c_22\":\"08:00:2b:01:02:03:04:05\",\"c_23\":123.0,\"c_24\":12345.320,"
                                + "\"c_25\":\"((0.0,0.0),(1.0,1.0),(2.0,0.0))\",\"c_26\":\"16/B374D848\",\"c_27\":null,"
                                + "\"c_28\":\"(2.0,2.0)\",\"c_29\":\"((-2.0,0.0),(-1.7320508075688774,0.9999999999999999),"
                                + "(-1.0000000000000002,1.7320508075688772),(-1.2246467991473532E-16,2.0),(0.9999999999999996,"
                                + "1.7320508075688774),(1.732050807568877,1.0000000000000007),(2.0,2.4492935982947064E-16),"
                                + "(1.7320508075688776,-0.9999999999999994),(1.0000000000000009,-1.7320508075688767),"
                                + "(3.6739403974420594E-16,-2.0),(-0.9999999999999987,-1.732050807568878),(-1.7320508075688767,"
                                + "-1.0000000000000009))\",\"c_30\":1.0,\"c_31\":2,\"c_32\":3,\"c_33\":4,"
                                + "\"c_34\":\"text\",\"c_35\":\"11:10:33\",\"c_36\":\"13:05:06\","
                                + "\"c_37\":\"2004-10-19T08:23:54Z\",\"c_38\":\"2004-10-19T08:23:54Z\","
                                + "\"c_39\":\"'fat' & 'rat'\",\"c_40\":\"'test' 'tsvector'\",\"c_41\":null,"
                                + "\"c_42\":\"a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11\",\"c_43\":\"<foo>bar</foo>\"},"
                                + "{\"c_1\":2,\"c_2\":4,\"c_3\":true,\"c_4\":\"101\",\"c_5\":true,\"c_6\":\"(3.0,5.0),"
                                + "(1.0,2.0)\",\"c_7\":\"AT19FtetT++2G9lbdlyM6w==\",\"c_8\":\"chr\",\"c_9\":\"test\","
                                + "\"c_10\":\"192.168.0.0/16\",\"c_11\":\"<(1.2,123.1),10.0>\",\"c_12\":\"2013-06-01\","
                                + "\"c_13\":99.2,\"c_14\":\"192.168.2.1/24\",\"c_15\":23,"
                                + "\"c_16\":\"0 years 0 mons 0 days 0 hours 40 mins 0.0 secs\","
                                + "\"c_17\":\"{\\\"prop\\\": \\\"value\\\"}\",\"c_18\":\"{\\\"prop\\\": \\\"value\\\"}\","
                                + "\"c_19\":\"{1.0,-1.0,0.0}\",\"c_20\":\"[(1.0,1.0),(2.0,2.0)]\","
                                + "\"c_21\":\"08:00:2b:01:02:03\",\"c_22\":\"08:00:2b:01:02:03:04:05\","
                                + "\"c_23\":123.0,\"c_24\":12345.320,\"c_25\":\"((0.0,0.0),(1.0,1.0),(2.0,0.0))\","
                                + "\"c_26\":\"16/B374D848\",\"c_27\":null,\"c_28\":\"(2.0,2.0)\",\"c_29\":\"((-2.0,0.0),"
                                + "(-1.7320508075688774,0.9999999999999999),(-1.0000000000000002,1.7320508075688772),"
                                + "(-1.2246467991473532E-16,2.0),(0.9999999999999996,1.7320508075688774),(1.732050807568877,"
                                + "1.0000000000000007),(2.0,2.4492935982947064E-16),(1.7320508075688776,-0.9999999999999994),"
                                + "(1.0000000000000009,-1.7320508075688767),(3.6739403974420594E-16,-2.0),(-0.9999999999999987,"
                                + "-1.732050807568878),(-1.7320508075688767,-1.0000000000000009))\",\"c_30\":1.0,\""
                                + "c_31\":2,\"c_32\":3,\"c_33\":4,\"c_34\":\"text\",\"c_35\":\"11:10:33\","
                                + "\"c_36\":\"13:05:06\",\"c_37\":\"2004-10-19T08:23:54Z\",\"c_38\":\"2004-10-19T08:23:54Z\","
                                + "\"c_39\":\"'fat' & 'rat'\",\"c_40\":\"'test' 'tsvector'\",\"c_41\":null,"
                                + "\"c_42\":\"a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11\",\"c_43\":\"<foo>bar</foo>\"},"
                                + "{\"c_1\":3,\"c_2\":5,\"c_3\":true,\"c_4\":\"101\",\"c_5\":true,\"c_6\":\"(3.0,5.0),"
                                + "(1.0,2.0)\",\"c_7\":\"AT19FtetT++2G9lbdlyM6w==\",\"c_8\":\"chr\",\"c_9\":\"test\","
                                + "\"c_10\":\"192.168.0.0/16\",\"c_11\":\"<(1.2,123.1),10.0>\",\"c_12\":\"2013-06-01\","
                                + "\"c_13\":99.2,\"c_14\":\"192.168.2.1/24\",\"c_15\":23,"
                                + "\"c_16\":\"0 years 0 mons 0 days 0 hours 40 mins 0.0 secs\","
                                + "\"c_17\":\"{\\\"prop\\\": \\\"value\\\"}\",\"c_18\":\"{\\\"prop\\\": \\\"value\\\"}\","
                                + "\"c_19\":\"{1.0,-1.0,0.0}\",\"c_20\":\"[(1.0,1.0),(2.0,2.0)]\","
                                + "\"c_21\":\"08:00:2b:01:02:03\",\"c_22\":\"08:00:2b:01:02:03:04:05\",\"c_23\":123.0,"
                                + "\"c_24\":12345.320,\"c_25\":\"((0.0,0.0),(1.0,1.0),(2.0,0.0))\",\"c_26\":\"16/B374D848\","
                                + "\"c_27\":null,\"c_28\":\"(2.0,2.0)\",\"c_29\":\"((-2.0,0.0),(-1.7320508075688774,"
                                + "0.9999999999999999),(-1.0000000000000002,1.7320508075688772),(-1.2246467991473532E-16,2.0),"
                                + "(0.9999999999999996,1.7320508075688774),(1.732050807568877,1.0000000000000007),"
                                + "(2.0,2.4492935982947064E-16),(1.7320508075688776,-0.9999999999999994),"
                                + "(1.0000000000000009,-1.7320508075688767),(3.6739403974420594E-16,-2.0),"
                                + "(-0.9999999999999987,-1.732050807568878),(-1.7320508075688767,-1.0000000000000009))\","
                                + "\"c_30\":1.0,\"c_31\":2,\"c_32\":3,\"c_33\":4,\"c_34\":\"text\",\"c_35\":\"11:10:33\","
                                + "\"c_36\":\"13:05:06\",\"c_37\":\"2004-10-19T08:23:54Z\",\"c_38\":\"2004-10-19T08:23:54Z\","
                                + "\"c_39\":\"'fat' & 'rat'\",\"c_40\":\"'test' 'tsvector'\",\"c_41\":null,"
                                + "\"c_42\":\"a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11\",\"c_43\":\"<foo>bar</foo>\"}]}", true));
    }

    @Test
    @DisplayName("Select with filter with parentheses - 1")
    public void get_kpi_binary_operator_parentheses() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_NEW + "/table_all_types?$filter=(c_1 eq 1 or c_1 eq 2) and c_37 eq 2004-10-19T10:23:54Z"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#table_all_types\","
                                + "\"value\":[{\"c_1\":1,\"c_2\":3,\"c_3\":true,\"c_4\":\"101\",\"c_5\":true,"
                                + "\"c_6\":\"(3.0,5.0),(1.0,2.0)\",\"c_7\":\"AT19FtetT++2G9lbdlyM6w==\","
                                + "\"c_8\":\"chr\",\"c_9\":\"test\",\"c_10\":\"192.168.0.0/16\",\"c_11\":\"<(1.2,123.1),10.0>\","
                                + "\"c_12\":\"2013-06-01\",\"c_13\":99.2,\"c_14\":\"192.168.2.1/24\",\"c_15\":23,"
                                + "\"c_16\":\"0 years 0 mons 0 days 0 hours 40 mins 0.0 secs\","
                                + "\"c_17\":\"{\\\"prop\\\": \\\"value\\\"}\",\"c_18\":\"{\\\"prop\\\": \\\"value\\\"}\","
                                + "\"c_19\":\"{1.0,-1.0,0.0}\",\"c_20\":\"[(1.0,1.0),(2.0,2.0)]\",\"c_21\":\"08:00:2b:01:02:03\","
                                + "\"c_22\":\"08:00:2b:01:02:03:04:05\",\"c_23\":123.0,\"c_24\":12345.320,"
                                + "\"c_25\":\"((0.0,0.0),(1.0,1.0),(2.0,0.0))\",\"c_26\":\"16/B374D848\",\"c_27\":null,"
                                + "\"c_28\":\"(2.0,2.0)\",\"c_29\":\"((-2.0,0.0),(-1.7320508075688774,0.9999999999999999),"
                                + "(-1.0000000000000002,1.7320508075688772),(-1.2246467991473532E-16,2.0),"
                                + "(0.9999999999999996,1.7320508075688774),(1.732050807568877,1.0000000000000007),"
                                + "(2.0,2.4492935982947064E-16),(1.7320508075688776,-0.9999999999999994),"
                                + "(1.0000000000000009,-1.7320508075688767),(3.6739403974420594E-16,-2.0),"
                                + "(-0.9999999999999987,-1.732050807568878),(-1.7320508075688767,-1.0000000000000009))\","
                                + "\"c_30\":1.0,\"c_31\":2,\"c_32\":3,\"c_33\":4,\"c_34\":\"text\",\"c_35\":\"11:10:33\","
                                + "\"c_36\":\"13:05:06\",\"c_37\":\"2004-10-19T08:23:54Z\",\"c_38\":\"2004-10-19T08:23:54Z\","
                                + "\"c_39\":\"'fat' & 'rat'\",\"c_40\":\"'test' 'tsvector'\",\"c_41\":null,"
                                + "\"c_42\":\"a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11\",\"c_43\":\"<foo>bar</foo>\"},"
                                + "{\"c_1\":2,\"c_2\":4,\"c_3\":true,\"c_4\":\"101\",\"c_5\":true,"
                                + "\"c_6\":\"(3.0,5.0),(1.0,2.0)\",\"c_7\":\"AT19FtetT++2G9lbdlyM6w==\","
                                + "\"c_8\":\"chr\",\"c_9\":\"test\",\"c_10\":\"192.168.0.0/16\","
                                + "\"c_11\":\"<(1.2,123.1),10.0>\",\"c_12\":\"2013-06-01\",\"c_13\":99.2,"
                                + "\"c_14\":\"192.168.2.1/24\",\"c_15\":23,"
                                + "\"c_16\":\"0 years 0 mons 0 days 0 hours 40 mins 0.0 secs\","
                                + "\"c_17\":\"{\\\"prop\\\": \\\"value\\\"}\",\"c_18\":\"{\\\"prop\\\": \\\"value\\\"}\","
                                + "\"c_19\":\"{1.0,-1.0,0.0}\",\"c_20\":\"[(1.0,1.0),(2.0,2.0)]\","
                                + "\"c_21\":\"08:00:2b:01:02:03\",\"c_22\":\"08:00:2b:01:02:03:04:05\","
                                + "\"c_23\":123.0,\"c_24\":12345.320,\"c_25\":\"((0.0,0.0),(1.0,1.0),(2.0,0.0))\","
                                + "\"c_26\":\"16/B374D848\",\"c_27\":null,\"c_28\":\"(2.0,2.0)\",\"c_29\":\"((-2.0,0.0),"
                                + "(-1.7320508075688774,0.9999999999999999),(-1.0000000000000002,1.7320508075688772),"
                                + "(-1.2246467991473532E-16,2.0),(0.9999999999999996,1.7320508075688774),"
                                + "(1.732050807568877,1.0000000000000007),(2.0,2.4492935982947064E-16),"
                                + "(1.7320508075688776,-0.9999999999999994),(1.0000000000000009,-1.7320508075688767),"
                                + "(3.6739403974420594E-16,-2.0),(-0.9999999999999987,-1.732050807568878),"
                                + "(-1.7320508075688767,-1.0000000000000009))\",\"c_30\":1.0,\"c_31\":2,\"c_32\":3,"
                                + "\"c_33\":4,\"c_34\":\"text\",\"c_35\":\"11:10:33\",\"c_36\":\"13:05:06\","
                                + "\"c_37\":\"2004-10-19T08:23:54Z\",\"c_38\":\"2004-10-19T08:23:54Z\","
                                + "\"c_39\":\"'fat' & 'rat'\",\"c_40\":\"'test' 'tsvector'\",\"c_41\":null,"
                                + "\"c_42\":\"a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11\",\"c_43\":\"<foo>bar</foo>\"}]}", true));
    }

    @Test
    @DisplayName("Select with filter with parentheses - 2")
    public void get_kpi_binary_operator_parentheses_2() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_NEW + "/table_all_types?$filter=c_1 eq 2 or (c_2 eq 4 and c_37 eq 2004-10-19T10:23:54Z)"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#table_all_types\","
                                + "\"value\":[{\"c_1\":2,\"c_2\":4,\"c_3\":true,\"c_4\":\"101\",\"c_5\":true,"
                                + "\"c_6\":\"(3.0,5.0),(1.0,2.0)\",\"c_7\":\"AT19FtetT++2G9lbdlyM6w==\",\"c_8\":\"chr\","
                                + "\"c_9\":\"test\",\"c_10\":\"192.168.0.0/16\",\"c_11\":\"<(1.2,123.1),10.0>\","
                                + "\"c_12\":\"2013-06-01\",\"c_13\":99.2,\"c_14\":\"192.168.2.1/24\",\"c_15\":23,"
                                + "\"c_16\":\"0 years 0 mons 0 days 0 hours 40 mins 0.0 secs\","
                                + "\"c_17\":\"{\\\"prop\\\": \\\"value\\\"}\",\"c_18\":\"{\\\"prop\\\": \\\"value\\\"}\","
                                + "\"c_19\":\"{1.0,-1.0,0.0}\",\"c_20\":\"[(1.0,1.0),(2.0,2.0)]\","
                                + "\"c_21\":\"08:00:2b:01:02:03\",\"c_22\":\"08:00:2b:01:02:03:04:05\","
                                + "\"c_23\":123.0,\"c_24\":12345.320,\"c_25\":\"((0.0,0.0),(1.0,1.0),(2.0,0.0))\","
                                + "\"c_26\":\"16/B374D848\",\"c_27\":null,\"c_28\":\"(2.0,2.0)\","
                                + "\"c_29\":\"((-2.0,0.0),(-1.7320508075688774,0.9999999999999999),"
                                + "(-1.0000000000000002,1.7320508075688772),(-1.2246467991473532E-16,2.0),"
                                + "(0.9999999999999996,1.7320508075688774),(1.732050807568877,1.0000000000000007),"
                                + "(2.0,2.4492935982947064E-16),(1.7320508075688776,-0.9999999999999994),"
                                + "(1.0000000000000009,-1.7320508075688767),(3.6739403974420594E-16,-2.0),"
                                + "(-0.9999999999999987,-1.732050807568878),(-1.7320508075688767,-1.0000000000000009))\","
                                + "\"c_30\":1.0,\"c_31\":2,\"c_32\":3,\"c_33\":4,\"c_34\":\"text\","
                                + "\"c_35\":\"11:10:33\",\"c_36\":\"13:05:06\",\"c_37\":\"2004-10-19T08:23:54Z\","
                                + "\"c_38\":\"2004-10-19T08:23:54Z\",\"c_39\":\"'fat' & 'rat'\","
                                + "\"c_40\":\"'test' 'tsvector'\",\"c_41\":null,"
                                + "\"c_42\":\"a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11\",\"c_43\":\"<foo>bar</foo>\"}]}", true));
    }

    @Test
    @DisplayName("Simple select with many postgres array data types")
    public void get_kpi_all_postgres_array_types() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL + SCHEMA_NEW + "/table_all_array_types"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#table_all_array_types\","
                                + "\"value\":[{\"c_1\":[123456,234567],\"c_3\":[true,false],\"c_4\":[\"101\",\"100\"],"
                                + "\"c_5\":[true,false],\"c_6\":[\"(3.0,5.0),(1.0,2.0)\",\"(10.0,6.0),(8.0,4.0)\"],"
                                + "\"c_8\":[\"chr\",\"abc\"],\"c_9\":[\"test\",\"abcd\"],"
                                + "\"c_10\":[\"192.168.0.0/16\",\"10.1.2.3/32\"],"
                                + "\"c_11\":[\"<(1.2,123.1),10.0>\",\"<(3.4,5.6),20.0>\"],"
                                + "\"c_12\":[\"2013-06-01\",\"2022-09-10\"],\"c_13\":[99.2,3.14159],"
                                + "\"c_14\":[\"192.168.2.1/24\",\"192.168.0.1/24\"],\"c_15\":[23,45],"
                                + "\"c_16\":[\"0 years 0 mons 0 days 0 hours 40 mins 0.0 secs\","
                                + "\"0 years 0 mons 0 days 2 hours 0 mins 0.0 secs\"],"
                                + "\"c_17\":[\"{\\\"prop1\\\": \\\"value1\\\"}\",\"{\\\"prop2\\\": \\\"value2\\\"}\"],"
                                + "\"c_18\":[\"{\\\"single\\\": \\\"value\\\"}\"],\"c_19\":[\"{1.0,-1.0,0.0}\"],"
                                + "\"c_20\":[\"[(1.0,1.0),(2.0,2.0)]\"],\"c_21\":[\"08:00:2b:01:02:03\"],"
                                + "\"c_22\":[\"08:00:2b:01:02:03:04:05\"],\"c_24\":[12345.32,6789.45],"
                                + "\"c_25\":[\"((0.0,0.0),(1.0,1.0),(2.0,0.0))\"],\"c_26\":[\"16/B374D848\"],"
                                + "\"c_28\":[\"(2.0,2.0)\",\"(3.0,3.0)\"],\"c_29\":[\"((-2.0,0.0),(-1.7320508075688774,"
                                + "0.9999999999999999),(-1.0000000000000002,1.7320508075688772),(-1.2246467991473532E-16,2.0),"
                                + "(0.9999999999999996,1.7320508075688774),(1.732050807568877,1.0000000000000007),"
                                + "(2.0,2.4492935982947064E-16),(1.7320508075688776,-0.9999999999999994),"
                                + "(1.0000000000000009,-1.7320508075688767),(3.6739403974420594E-16,-2.0),"
                                + "(-0.9999999999999987,-1.732050807568878),(-1.7320508075688767,-1.0000000000000009))\"],"
                                + "\"c_30\":[1.1,2.2,3.3],\"c_31\":[2,3,4,5,6],\"c_34\":[\"text1\",\"text2\"],"
                                + "\"c_35\":[\"11:10:33\"],\"c_36\":[\"13:05:06\"],\"c_37\":[\"2004-10-19T08:23:54Z\"],"
                                + "\"c_38\":[\"2004-10-19T08:23:54Z\"],\"c_39\":[\"'fat' & 'rat'\"],"
                                + "\"c_40\":[\"'test' 'tsvector'\"],\"c_42\":[\"a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11\"],"
                                + "\"c_43\":[\"<foo>bar</foo>\",\"<dev>null</dev>\"]},"
                                + "{\"c_1\":[789012,34567],\"c_3\":[false,true],\"c_4\":[\"001\",\"010\"],"
                                + "\"c_5\":[false,false,true],\"c_6\":[\"(30.0,50.0),(10.0,20.0)\",\"(100.0,60.0),(80.0,40.0)\"],"
                                + "\"c_8\":[\"def\",\"ghi\"],\"c_9\":[\"efgh\",\"spqr\"],"
                                + "\"c_10\":[\"190.168.0.0/16\",\"10.4.5.6/32\"],"
                                + "\"c_11\":[\"<(3.4,456.7),11.0>\",\"<(4.6,7.8),21.0>\"],"
                                + "\"c_12\":[\"2014-07-02\",\"2023-05-11\"],\"c_13\":[188.3,2.781],"
                                + "\"c_14\":[\"192.168.1.1/24\",\"232.104.0.1/24\"],\"c_15\":[67,89],"
                                + "\"c_16\":[\"0 years 0 mons 0 days 0 hours 20 mins 0.0 secs\","
                                + "\"0 years 0 mons 0 days 4 hours 0 mins 0.0 secs\"],"
                                + "\"c_17\":[\"{\\\"prop3\\\": \\\"value3\\\"}\",\"{\\\"prop4\\\": \\\"value4\\\"}\"],"
                                + "\"c_18\":[\"{\\\"single2\\\": \\\"value2\\\"}\"],\"c_19\":[\"{1.0,-1.0,0.0}\"],"
                                + "\"c_20\":[\"[(10.0,10.0),(20.0,20.0)]\"],\"c_21\":[\"09:00:2b:01:02:03\"],"
                                + "\"c_22\":[\"09:00:2b:01:02:03:04:05\"],\"c_24\":[98765.43,5432.1],"
                                + "\"c_25\":[\"((1.0,1.0),(2.0,2.0),(3.0,0.0))\"],\"c_26\":[\"16/B374D848\"],"
                                + "\"c_28\":[\"(3.0,3.0)\",\"(9.0,9.0)\"],\"c_29\":[\"((-3.0,0.0),"
                                + "(-2.598076211353316,1.4999999999999998),(-1.5000000000000004,2.598076211353316),"
                                + "(-1.8369701987210297E-16,3.0),(1.4999999999999993,2.598076211353316),"
                                + "(2.5980762113533156,1.5000000000000009),(3.0,3.6739403974420594E-16),"
                                + "(2.5980762113533165,-1.4999999999999991),(1.5000000000000013,-2.598076211353315),"
                                + "(5.51091059616309E-16,-3.0),(-1.499999999999998,-2.598076211353317),"
                                + "(-2.598076211353315,-1.5000000000000013))\"],\"c_30\":[2.2,7.7],"
                                + "\"c_31\":[4,5,6,7],\"c_34\":[\"text3\",\"text4\",\"text5\",\"text6\"],"
                                + "\"c_35\":[\"12:10:33\"],\"c_36\":[\"14:05:06\"],"
                                + "\"c_37\":[\"2005-10-19T08:23:54Z\",\"2006-11-20T10:34:05Z\"],"
                                + "\"c_38\":[\"2005-10-19T08:23:54Z\"],\"c_39\":[\"'fat' & 'cat'\"],"
                                + "\"c_40\":[\"'test2' 'tsvector'\"],\"c_42\":[\"a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11\","
                                + "\"b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11\"],\"c_43\":[\"<foo>bar2</foo>\","
                                + "\"<dev>null0</dev>\"]},"
                                + "{\"c_1\":[1,null],\"c_3\":[true,null,false],\"c_4\":[\"101\",null,\"100\"],"
                                + "\"c_5\":[true,false,null],\"c_6\":[\"(3.0,5.0),(1.0,2.0)\",\"(10.0,6.0),(8.0,4.0)\",null],"
                                + "\"c_8\":[\"chr\",\"abc\",null],\"c_9\":[null,\"test\",\"abcd\"],"
                                + "\"c_10\":[\"192.168.0.0/16\",\"10.1.2.3/32\",null],"
                                + "\"c_11\":[\"<(1.2,123.1),10.0>\",\"<(3.4,5.6),20.0>\",null],"
                                + "\"c_12\":[\"2013-06-01\",\"2022-09-10\",null],\"c_13\":[99.2,null,3.14159],"
                                + "\"c_14\":[null,\"192.168.2.1/24\",\"192.168.0.1/24\"],\"c_15\":[23,null,45],"
                                + "\"c_16\":[\"0 years 0 mons 0 days 0 hours 40 mins 0.0 secs\","
                                + "\"0 years 0 mons 0 days 2 hours 0 mins 0.0 secs\",null],"
                                + "\"c_17\":[\"{\\\"prop1\\\": \\\"value1\\\"}\",\"{\\\"prop2\\\": \\\"value2\\\"}\",null],"
                                + "\"c_18\":[\"{\\\"single\\\": null}\"],\"c_19\":[\"{1.0,-1.0,0.0}\",null],"
                                + "\"c_20\":[null,\"[(1.0,1.0),(2.0,2.0)]\"],\"c_21\":[\"08:00:2b:01:02:03\",null],"
                                + "\"c_22\":[null,\"08:00:2b:01:02:03:04:05\"],\"c_24\":[12345.32,6789.45,null],"
                                + "\"c_25\":[\"((0.0,0.0),(1.0,1.0),(2.0,0.0))\",null],\"c_26\":[\"16/B374D848\",null],"
                                + "\"c_28\":[\"(2.0,2.0)\",\"(3.0,3.0)\",null],\"c_29\":[\"((-2.0,0.0),"
                                + "(-1.7320508075688774,0.9999999999999999),(-1.0000000000000002,1.7320508075688772),"
                                + "(-1.2246467991473532E-16,2.0),(0.9999999999999996,1.7320508075688774),"
                                + "(1.732050807568877,1.0000000000000007),(2.0,2.4492935982947064E-16),"
                                + "(1.7320508075688776,-0.9999999999999994),(1.0000000000000009,-1.7320508075688767),"
                                + "(3.6739403974420594E-16,-2.0),(-0.9999999999999987,-1.732050807568878),"
                                + "(-1.7320508075688767,-1.0000000000000009))\",null],\"c_30\":[1.1,2.2,null,3.3],"
                                + "\"c_31\":[null,2,3,4,5,6],\"c_34\":[\"text1\",null,\"text2\"],"
                                + "\"c_35\":[\"11:10:33\",null],\"c_36\":[\"13:05:06\",null],"
                                + "\"c_37\":[null,\"2004-10-19T08:23:54Z\"],\"c_38\":[null,\"2004-10-19T08:23:54Z\"],"
                                + "\"c_39\":[null,\"'fat' & 'rat'\",null],\"c_40\":[null,\"'test' 'tsvector'\"],"
                                + "\"c_42\":[\"a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11\",null],"
                                + "\"c_43\":[\"<foo>bar</foo>\",\"<dev>null</dev>\",null]},"
                                + "{\"c_1\":[],\"c_3\":[],\"c_4\":[],\"c_5\":[],\"c_6\":[],\"c_8\":[],\"c_9\":[],"
                                + "\"c_10\":[],\"c_11\":[],\"c_12\":[],\"c_13\":[],\"c_14\":[],\"c_15\":[],\"c_16\":[],"
                                + "\"c_17\":[],\"c_18\":[],\"c_19\":[],\"c_20\":[],\"c_21\":[],\"c_22\":[],\"c_24\":[],"
                                + "\"c_25\":[],\"c_26\":[],\"c_28\":[],\"c_29\":[],\"c_30\":[],\"c_31\":[],\"c_34\":[],"
                                + "\"c_35\":[],\"c_36\":[],\"c_37\":[],\"c_38\":[],\"c_39\":[],\"c_40\":[],\"c_42\":[],"
                                + "\"c_43\":[]}]}", true));
    }

    @DisplayName("Count queries for partitioned tables")
    @ParameterizedTest(name = "{index} --> {0}.{1}")
    @MethodSource("partitionedTableEntitiesProvider")
    void whenPartitionedTableQueried_responseContainsCorrectCount(final String schemaNamespace, final String partitionedTableName)
            throws Exception {
        final int expectedCount = TestDataEntityMapping.getEntityRowCount(schemaNamespace, partitionedTableName);

        assertThat(TestDataEntityMapping.getNamespaces()).contains(schemaNamespace);
        assertThat(TestDataEntityMapping.getEntityTypes(schemaNamespace)).contains(partitionedTableName);

        final ResultActions result = mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                + schemaNamespace + "/" + partitionedTableName
                + "?$count=true&$top=0"));

        result.andExpectAll(
                MockMvcResultMatchers.status().isOk(),
                MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#"
                                + partitionedTableName
                                + "\",\"@odata.count\":"
                                + expectedCount
                                + ",\"value\":[]}"
                )
        );
    }

    private static List<Arguments> partitionedTableEntitiesProvider() {
        final Map<String, Set<String>> partitionedTableEntities = TestDataEntityMapping.getPartitionedTableEntities();

        return partitionedTableEntities.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream().map(value -> Arguments.of(entry.getKey(), value)))
                .collect(Collectors.toList());
    }

    @Test
    @DisplayName("Simple select from sensitive schema without query options")
    public void get_sensitive_all() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_MIXED + "/kpi_sensitive"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#kpi_sensitive\",\"value\":["
                                + "{\"date_time\":\"2023-05-01T05:21:23.444333Z\",\"First_kpi\":0,"
                                + "\"sECOND_kpi\":10,\"third_KPI\":92236854775807,\"fOURTH_KPi\":-1235.679,"
                                + "\"Fifth_kpI\":-1235.69,\"SIXTH_KPI\":-98.6521,\"seventh_kpi\":-12347890.123456713},"
                                + "{\"date_time\":\"2023-05-02T06:31:34.555678Z\",\"First_kpi\":1,\"sECOND_kpi\":11,"
                                + "\"third_KPI\":-9223372036854775808,\"fOURTH_KPi\":12345.6789,"
                                + "\"Fifth_kpI\":12345.6789,\"SIXTH_KPI\":987.6543,\"seventh_kpi\":1234567890.1234568},"
                                + "{\"date_time\":\"2023-05-03T07:41:45.667776Z\",\"First_kpi\":2,\"sECOND_kpi\":12,"
                                + "\"third_KPI\":9223372036854775807,\"fOURTH_KPi\":-12345.6789,"
                                + "\"Fifth_kpI\":-12345.6789,\"SIXTH_KPI\":-987.6543,"
                                + "\"seventh_kpi\":-1234567890.1234568},{\"date_time\":\"2023-05-04T08:51:56.788877Z\","
                                + "\"First_kpi\":3,\"sECOND_kpi\":13,\"third_KPI\":-9223372036854775808,"
                                + "\"fOURTH_KPi\":12345.6789,\"Fifth_kpI\":12345.6789,\"SIXTH_KPI\":987.6543,"
                                + "\"seventh_kpi\":1234567890.1234568},{\"date_time\":\"2023-05-05T09:00:00.820425Z\","
                                + "\"First_kpi\":4,\"sECOND_kpi\":14,\"third_KPI\":9223372036854775807,"
                                + "\"fOURTH_KPi\":-12345.6789,\"Fifth_kpI\":-12345.6789,\"SIXTH_KPI\":-987.6543,"
                                + "\"seventh_kpi\":-1234567890.1234568}]}"));
    }

    @Test
    @DisplayName("Select from sensitive schema with star")
    public void get_sensitive_all_star() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_MIXED + "/kpi_sensitive?$select=*"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#kpi_sensitive(*)\",\"value\":["
                                + "{\"date_time\":\"2023-05-01T05:21:23.444333Z\",\"First_kpi\":0,"
                                + "\"sECOND_kpi\":10,\"third_KPI\":92236854775807,\"fOURTH_KPi\":-1235.679,"
                                + "\"Fifth_kpI\":-1235.69,\"SIXTH_KPI\":-98.6521,\"seventh_kpi\":-12347890.123456713},"
                                + "{\"date_time\":\"2023-05-02T06:31:34.555678Z\",\"First_kpi\":1,\"sECOND_kpi\":11,"
                                + "\"third_KPI\":-9223372036854775808,\"fOURTH_KPi\":12345.6789,"
                                + "\"Fifth_kpI\":12345.6789,\"SIXTH_KPI\":987.6543,\"seventh_kpi\":1234567890.1234568},"
                                + "{\"date_time\":\"2023-05-03T07:41:45.667776Z\",\"First_kpi\":2,\"sECOND_kpi\":12,"
                                + "\"third_KPI\":9223372036854775807,\"fOURTH_KPi\":-12345.6789,"
                                + "\"Fifth_kpI\":-12345.6789,\"SIXTH_KPI\":-987.6543,"
                                + "\"seventh_kpi\":-1234567890.1234568},{\"date_time\":\"2023-05-04T08:51:56.788877Z\","
                                + "\"First_kpi\":3,\"sECOND_kpi\":13,\"third_KPI\":-9223372036854775808,"
                                + "\"fOURTH_KPi\":12345.6789,\"Fifth_kpI\":12345.6789,\"SIXTH_KPI\":987.6543,"
                                + "\"seventh_kpi\":1234567890.1234568},{\"date_time\":\"2023-05-05T09:00:00.820425Z\","
                                + "\"First_kpi\":4,\"sECOND_kpi\":14,\"third_KPI\":9223372036854775807,"
                                + "\"fOURTH_KPi\":-12345.6789,\"Fifth_kpI\":-12345.6789,\"SIXTH_KPI\":-987.6543,"
                                + "\"seventh_kpi\":-1234567890.1234568}]}"));
    }

    @Test
    @DisplayName("Select from sensitive schema with one column")
    public void get_sensitive_select() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL + SCHEMA_MIXED + "/kpi_sensitive?$select=First_kpi"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#kpi_sensitive(First_kpi)\",\"value\":["
                                + "{\"First_kpi\":0},{\"First_kpi\":1},{\"First_kpi\":2},{\"First_kpi\":3},"
                                + "{\"First_kpi\":4}]}", true));
    }

    @Test
    @DisplayName("Select from sensitive schema with multiple columns")
    public void get_sensitive_multi_column() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_MIXED + "/kpi_sensitive?$select=SIXTH_KPI,seventh_kpi"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#kpi_sensitive(SIXTH_KPI,seventh_kpi)\",\"value\":["
                                + "{\"SIXTH_KPI\":-98.6521,\"seventh_kpi\":-12347890.123456713},"
                                + "{\"SIXTH_KPI\":987.6543,\"seventh_kpi\":1234567890.1234568},"
                                + "{\"SIXTH_KPI\":-987.6543,\"seventh_kpi\":-1234567890.1234568},"
                                + "{\"SIXTH_KPI\":987.6543,\"seventh_kpi\":1234567890.1234568},"
                                + "{\"SIXTH_KPI\":-987.6543,\"seventh_kpi\":-1234567890.1234568}]}", true));
    }

    @Test
    @DisplayName("Select from sensitive schema with multiple columns and star")
    public void get_sensitive_multi_column_star() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_MIXED + "/kpi_sensitive?$select=SIXTH_KPI,*,seventh_kpi"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#kpi_sensitive(*)\",\"value\":["
                                + "{\"date_time\":\"2023-05-01T05:21:23.444333Z\",\"First_kpi\":0,"
                                + "\"sECOND_kpi\":10,\"third_KPI\":92236854775807,\"fOURTH_KPi\":-1235.679,"
                                + "\"Fifth_kpI\":-1235.69,\"SIXTH_KPI\":-98.6521,\"seventh_kpi\":-12347890.123456713},"
                                + "{\"date_time\":\"2023-05-02T06:31:34.555678Z\",\"First_kpi\":1,\"sECOND_kpi\":11,"
                                + "\"third_KPI\":-9223372036854775808,\"fOURTH_KPi\":12345.6789,"
                                + "\"Fifth_kpI\":12345.6789,\"SIXTH_KPI\":987.6543,\"seventh_kpi\":1234567890.1234568},"
                                + "{\"date_time\":\"2023-05-03T07:41:45.667776Z\",\"First_kpi\":2,\"sECOND_kpi\":12,"
                                + "\"third_KPI\":9223372036854775807,\"fOURTH_KPi\":-12345.6789,"
                                + "\"Fifth_kpI\":-12345.6789,\"SIXTH_KPI\":-987.6543,"
                                + "\"seventh_kpi\":-1234567890.1234568},{\"date_time\":\"2023-05-04T08:51:56.788877Z\","
                                + "\"First_kpi\":3,\"sECOND_kpi\":13,\"third_KPI\":-9223372036854775808,"
                                + "\"fOURTH_KPi\":12345.6789,\"Fifth_kpI\":12345.6789,\"SIXTH_KPI\":987.6543,"
                                + "\"seventh_kpi\":1234567890.1234568},{\"date_time\":\"2023-05-05T09:00:00.820425Z\","
                                + "\"First_kpi\":4,\"sECOND_kpi\":14,\"third_KPI\":9223372036854775807,"
                                + "\"fOURTH_KPi\":-12345.6789,\"Fifth_kpI\":-12345.6789,\"SIXTH_KPI\":-987.6543,"
                                + "\"seventh_kpi\":-1234567890.1234568}]}"));
    }

    @Test
    @DisplayName("Select from sensitive schema with count")
    public void get_sensitive_count() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL + SCHEMA_MIXED + "/kpi_sensitive?$count=true"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#kpi_sensitive\",\"@odata.count\": 5,\"value\":["
                                + "{\"date_time\":\"2023-05-01T05:21:23.444333Z\",\"First_kpi\":0,"
                                + "\"sECOND_kpi\":10,\"third_KPI\":92236854775807,\"fOURTH_KPi\":-1235.679,"
                                + "\"Fifth_kpI\":-1235.69,\"SIXTH_KPI\":-98.6521,\"seventh_kpi\":-12347890.123456713},"
                                + "{\"date_time\":\"2023-05-02T06:31:34.555678Z\",\"First_kpi\":1,\"sECOND_kpi\":11,"
                                + "\"third_KPI\":-9223372036854775808,\"fOURTH_KPi\":12345.6789,"
                                + "\"Fifth_kpI\":12345.6789,\"SIXTH_KPI\":987.6543,\"seventh_kpi\":1234567890.1234568},"
                                + "{\"date_time\":\"2023-05-03T07:41:45.667776Z\",\"First_kpi\":2,\"sECOND_kpi\":12,"
                                + "\"third_KPI\":9223372036854775807,\"fOURTH_KPi\":-12345.6789,"
                                + "\"Fifth_kpI\":-12345.6789,\"SIXTH_KPI\":-987.6543,"
                                + "\"seventh_kpi\":-1234567890.1234568},{\"date_time\":\"2023-05-04T08:51:56.788877Z\","
                                + "\"First_kpi\":3,\"sECOND_kpi\":13,\"third_KPI\":-9223372036854775808,"
                                + "\"fOURTH_KPi\":12345.6789,\"Fifth_kpI\":12345.6789,\"SIXTH_KPI\":987.6543,"
                                + "\"seventh_kpi\":1234567890.1234568},{\"date_time\":\"2023-05-05T09:00:00.820425Z\","
                                + "\"First_kpi\":4,\"sECOND_kpi\":14,\"third_KPI\":9223372036854775807,"
                                + "\"fOURTH_KPi\":-12345.6789,\"Fifth_kpI\":-12345.6789,\"SIXTH_KPI\":-987.6543,"
                                + "\"seventh_kpi\":-1234567890.1234568}]}"));
    }

    @Test
    @DisplayName("Select from sensitive schema with multiple ordering elements")
    public void get_sensitive_order_by() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_MIXED + "/kpi_sensitive?$orderby=fOURTH_KPi desc,sECOND_kpi asc"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#kpi_sensitive\",\"value\":["
                                + "{\"date_time\":\"2023-05-02T06:31:34.555678Z\",\"First_kpi\":1,\"sECOND_kpi\":11,"
                                + "\"third_KPI\":-9223372036854775808,\"fOURTH_KPi\":12345.6789,"
                                + "\"Fifth_kpI\":12345.6789,\"SIXTH_KPI\":987.6543,\"seventh_kpi\":1234567890.1234568},"
                                + "{\"date_time\":\"2023-05-04T08:51:56.788877Z\",\"First_kpi\":3,\"sECOND_kpi\":13,"
                                + "\"third_KPI\":-9223372036854775808,\"fOURTH_KPi\":12345.6789,"
                                + "\"Fifth_kpI\":12345.6789,\"SIXTH_KPI\":987.6543,\"seventh_kpi\":1234567890.1234568},"
                                + "{\"date_time\":\"2023-05-01T05:21:23.444333Z\",\"First_kpi\":0,\"sECOND_kpi\":10,"
                                + "\"third_KPI\":92236854775807,\"fOURTH_KPi\":-1235.679,\"Fifth_kpI\":-1235.69,"
                                + "\"SIXTH_KPI\":-98.6521,\"seventh_kpi\":-12347890.123456713},"
                                + "{\"date_time\":\"2023-05-03T07:41:45.667776Z\",\"First_kpi\":2,\"sECOND_kpi\":12,"
                                + "\"third_KPI\":9223372036854775807,\"fOURTH_KPi\":-12345.6789,"
                                + "\"Fifth_kpI\":-12345.6789,\"SIXTH_KPI\":-987.6543,"
                                + "\"seventh_kpi\":-1234567890.1234568},{\"date_time\":\"2023-05-05T09:00:00.820425Z\","
                                + "\"First_kpi\":4,\"sECOND_kpi\":14,\"third_KPI\":9223372036854775807,"
                                + "\"fOURTH_KPi\":-12345.6789,\"Fifth_kpI\":-12345.6789,\"SIXTH_KPI\":-987.6543,"
                                + "\"seventh_kpi\":-1234567890.1234568}]}", true));
    }

    @Test
    @DisplayName("Select from sensitive schema with filter")
    public void get_sensitive_filter() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_MIXED + "/kpi_sensitive?$filter=First_kpi lt 4 and round(SIXTH_KPI) eq "
                        + "988&$select=date_time,First_kpi,SIXTH_KPI,sECOND_kpi&$orderby=First_kpi desc"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#kpi_sensitive(date_time,First_kpi,sECOND_kpi,"
                                + "SIXTH_KPI)\",\"value\":[{\"date_time\":\"2023-05-04T08:51:56.788877Z\","
                                + "\"First_kpi\":3,\"sECOND_kpi\":13,\"SIXTH_KPI\":987.6543},"
                                + "{\"date_time\":\"2023-05-02T06:31:34.555678Z\",\"First_kpi\":1,"
                                + "\"sECOND_kpi\":11,\"SIXTH_KPI\":987.6543}]}", true));
    }

    @Test
    @DisplayName("Select from sensitive schema with filter and star")
    public void get_sensitive_filter_star() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_MIXED + "/kpi_sensitive?$filter=First_kpi lt 4 and round(SIXTH_KPI) eq "
                        + "988&$select=date_time,First_kpi,SIXTH_KPI,*,sECOND_kpi&$orderby=First_kpi desc"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#kpi_sensitive(*)\",\"value\":["
                                + "{\"date_time\":\"2023-05-04T08:51:56.788877Z\","
                                + "\"First_kpi\":3,\"sECOND_kpi\":13,\"third_KPI\":-9223372036854775808,"
                                + "\"fOURTH_KPi\":12345.6789,\"Fifth_kpI\":12345.67890,\"SIXTH_KPI\":987.6543,"
                                + "\"seventh_kpi\":1234567890.123456789012345},"
                                + "{\"date_time\":\"2023-05-02T06:31:34.555678Z\","
                                + "\"First_kpi\":1,\"sECOND_kpi\":11,\"third_KPI\":-9223372036854775808,"
                                + "\"fOURTH_KPi\":12345.6789,\"Fifth_kpI\":12345.67890,\"SIXTH_KPI\":987.6543,"
                                + "\"seventh_kpi\":1234567890.123456789012345}]}", true));
    }

    @Test
    @DisplayName("Select from sensitive schema with filter - nodeFDN")
    public void get_sensitive_nodeFdn_filter() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_MIXED + "/kpi_csac?$filter=nodeFDN eq 'ManagedElement=NodeFDNManagedElement0002,"
                        + "Equipment=3,SupportUnit=2'"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#kpi_csac\",\"value\":["
                                + "{\"moFdn\":\"/epg:epg/pgw/ns[name=131-000259]/apn[name=apn01.ericsson.se]\","
                                + "\"nodeFDN\":\"ManagedElement=NodeFDNManagedElement0002,Equipment=3,SupportUnit=2\","
                                + "\"workHours\":12},"
                                + "{\"moFdn\":\"/epg:epg/ftc/ns[name=123-001896]/apn[name=offline.ericsson.hu]\","
                                + "\"nodeFDN\":\"ManagedElement=NodeFDNManagedElement0002,Equipment=3,SupportUnit=2\","
                                + "\"workHours\":20}]}", true));
    }

    @Test
    @DisplayName("Select from sensitive schema with filter and star - nodeFDN")
    public void get_sensitive_nodeFdn_filter_star() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL
                        + SCHEMA_MIXED + "/kpi_csac?$select=*&$filter=nodeFDN eq 'ManagedElement=NodeFDNManagedElement0002,"
                        + "Equipment=3,SupportUnit=2'"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#kpi_csac(*)\",\"value\":["
                                + "{\"moFdn\":\"/epg:epg/pgw/ns[name=131-000259]/apn[name=apn01.ericsson.se]\","
                                + "\"nodeFDN\":\"ManagedElement=NodeFDNManagedElement0002,Equipment=3,SupportUnit=2\","
                                + "\"workHours\":12},"
                                + "{\"moFdn\":\"/epg:epg/ftc/ns[name=123-001896]/apn[name=offline.ericsson.hu]\","
                                + "\"nodeFDN\":\"ManagedElement=NodeFDNManagedElement0002,Equipment=3,SupportUnit=2\","
                                + "\"workHours\":20}]}", true));
    }

    @Test
    @DisplayName("Select from sensitive schema with filter - moFDN")
    public void get_sensitive_moFdn_filter() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL + SCHEMA_MIXED
                        + "/kpi_csac?$filter=moFdn eq '/epg:epg/pgw/ns[name=131-000259]/apn[name=apn01.ericsson.se]'"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#kpi_csac\",\"value\":["
                                + "{\"moFdn\":\"/epg:epg/pgw/ns[name=131-000259]/apn[name=apn01.ericsson.se]\","
                                + "\"nodeFDN\":\"ManagedElement=NodeFDNManagedElement0002,Equipment=3,SupportUnit=2\","
                                + "\"workHours\":12},"
                                + "{\"moFdn\":\"/epg:epg/pgw/ns[name=131-000259]/apn[name=apn01.ericsson.se]\","
                                + "\"nodeFDN\":\"ManagedElement=NodeFDNManagedElement0099,Equipment=2,SupportUnit=1\","
                                + "\"workHours\":16}]}", true));
    }

    @Test
    @DisplayName("Select from sensitive schema with filter and star - moFDN")
    public void get_sensitive_moFdn_filter_star() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL + SCHEMA_MIXED
                        + "/kpi_csac?$filter=moFdn eq '/epg:epg/pgw/ns[name=131-000259]/apn[name=apn01.ericsson.se]'"
                        + "&$select=*"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#kpi_csac(*)\",\"value\":["
                                + "{\"moFdn\":\"/epg:epg/pgw/ns[name=131-000259]/apn[name=apn01.ericsson.se]\","
                                + "\"nodeFDN\":\"ManagedElement=NodeFDNManagedElement0002,Equipment=3,SupportUnit=2\","
                                + "\"workHours\":12},"
                                + "{\"moFdn\":\"/epg:epg/pgw/ns[name=131-000259]/apn[name=apn01.ericsson.se]\","
                                + "\"nodeFDN\":\"ManagedElement=NodeFDNManagedElement0099,Equipment=2,SupportUnit=1\","
                                + "\"workHours\":16}]}", true));
    }

    @Test
    @DisplayName("Select from sensitive schema with multiple query options")
    public void get_sensitive_workHours_filter() throws Exception {
        mockMvs.perform(MockMvcRequestBuilders.get(BASE_URL + SCHEMA_MIXED
                        + "/kpi_csac?$filter=workHours gt 8 and workHours lt 24 and endswith(moFdn,'.hu]')"
                        + "&$select=moFdn,workHours&$orderby=workHours desc,tolower(moFdn) desc"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"@odata.context\":\"$metadata#kpi_csac(moFdn,workHours)\",\"value\":["
                                + "{\"moFdn\":\"/epg:epg/ftc/ns[name=123-001896]/apn[name=offline.ericsson.hu]\","
                                + "\"workHours\":20}]}", true));
    }
}
