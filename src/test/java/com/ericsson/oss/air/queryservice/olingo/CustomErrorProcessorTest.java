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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ODataServerError;
import org.apache.olingo.server.core.ODataImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Olingo custom error processor")
public class CustomErrorProcessorTest {

    @Test
    @DisplayName("Handle invalid content type")
    void testHandleInvalidContentType() {
        final ODataRequest request = new ODataRequest();
        final ODataResponse response = new ODataResponse();
        final ODataServerError error = new ODataServerError();
        final CustomErrorProcessor processor = new CustomErrorProcessor();

        processor.init(new ODataImpl(), null);
        processor.processError(request, response, error, ContentType.APPLICATION_SVG_XML);

        assertThat(response.getContent()).hasContent("{\"error\":{\"code\":null,\"message\":\"An unexpected exception occurred\"}}");
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatusCode());
    }
}
