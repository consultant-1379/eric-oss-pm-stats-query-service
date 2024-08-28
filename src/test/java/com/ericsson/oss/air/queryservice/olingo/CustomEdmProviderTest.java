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

package com.ericsson.oss.air.queryservice.olingo;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;

import com.ericsson.oss.air.queryservice.service.DatabaseInfoService;
import com.ericsson.oss.air.queryservice.service.SchemaTranslatorService;

import lombok.SneakyThrows;

@SpringBootTest(classes = CustomEdmProvider.class)
@MockBeans({@MockBean(DatabaseInfoService.class), @MockBean(SchemaTranslatorService.class)})
@DisplayName("Olingo custom EDM provider")
public class CustomEdmProviderTest {

    @Autowired
    private CustomEdmProvider customEdmProvider;

    @Test
    @DisplayName("Throw ODataApplicationException if getEntityType is called with null argument")
    void whenGetEntityTypeCalledWithNull_shouldThrowODataApplicationException() {
        assertThatThrownBy(() -> customEdmProvider.getEntityType(null))
                .isInstanceOf(ODataApplicationException.class)
                .hasMessage("Invalid parameter!")
                .hasFieldOrPropertyWithValue("statusCode", HttpStatusCode.BAD_REQUEST.getStatusCode());
    }

    @Test
    @DisplayName("Throw ODataApplicationException if getEntitySet is called with invalid container argument")
    void whenGetEntitySetCalledWithInvalidContainer_shouldThrowODataApplicationException() {
        final FullQualifiedName invalidContainerName = new FullQualifiedName("!", "!");

        assertThatThrownBy(() -> customEdmProvider.getEntitySet(invalidContainerName, null))
                .isInstanceOf(ODataApplicationException.class)
                .hasMessageContaining("Invalid entity container ")
                .hasFieldOrPropertyWithValue("statusCode", HttpStatusCode.BAD_REQUEST.getStatusCode());
    }

    @Test
    @DisplayName("Throw ODataApplicationException if getEntityContainerInfo is called with invalid container argument")
    void whenGetEntityContainerInfoCalledWithInvalidContainer_shouldThrowODataApplicationException() {
        final FullQualifiedName invalidContainerName = new FullQualifiedName("!", "!");

        assertThatThrownBy(() -> customEdmProvider.getEntityContainerInfo(invalidContainerName))
                .isInstanceOf(ODataApplicationException.class)
                .hasMessageContaining("Invalid entity container ")
                .hasFieldOrPropertyWithValue("statusCode", HttpStatusCode.BAD_REQUEST.getStatusCode());
    }

    @Test
    @SneakyThrows
    @DisplayName("Return correct info if getEntityContainerInfo is called with null argument")
    void whenGetEntityContainerInfoCalledWithNull_shouldReturnCorrectInfo() {
        final CsdlEntityContainerInfo entityContainerInfo = new CsdlEntityContainerInfo();
        entityContainerInfo.setContainerName(CustomEdmProvider.ENTITY_CONTAINER);

        assertThat(customEdmProvider.getEntityContainerInfo(null))
                .usingRecursiveComparison().withStrictTypeChecking().isEqualTo(entityContainerInfo);
    }
}
