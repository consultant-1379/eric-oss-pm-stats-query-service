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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ODataServerError;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.processor.ErrorProcessor;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Processor class for handling errors/exceptions from the Olingo library or another processor implementation.
 */
@Component
public class CustomErrorProcessor implements ErrorProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomErrorProcessor.class);

    private OData odata;

    /**
     * Initializes private members. This method is called by the handler implementation in the Olingo library.
     * @param oData OData implementation
     * @param serviceMetadata service metadata implementation with entity data model reference
     */
    @Override
    public void init(final OData oData, final ServiceMetadata serviceMetadata) {
        this.odata = oData;
    }

    /**
     * Processes an error/exception. This method must not throw exceptions.
     * @param oDataRequest OData request object containing raw HTTP information
     * @param oDataResponse OData response object
     * @param oDataServerError the server error
     * @param contentType requested content type as response format
     */
    @Override
    public void processError(final ODataRequest oDataRequest, final ODataResponse oDataResponse,
                             final ODataServerError oDataServerError, final ContentType contentType) {
        try {
            final ODataSerializer serializer = odata.createSerializer(contentType);
            if (oDataServerError.getException() != null && oDataServerError.getException().getCause() != null
                    && oDataServerError.getException().getCause() instanceof ODataApplicationException) {
                // if the underlying exception is instance of ODataApplicationException, then use the underlying exception's codes
                final ODataApplicationException ex = (ODataApplicationException) oDataServerError.getException().getCause();
                oDataServerError.setCode(ex.getODataErrorCode());
                oDataServerError.setStatusCode(ex.getStatusCode());
                oDataResponse.setStatusCode(ex.getStatusCode());
            } else {
                oDataResponse.setStatusCode(oDataServerError.getStatusCode());
            }
            oDataResponse.setContent(serializer.error(oDataServerError).getContent());
            oDataResponse.setHeader(HttpHeader.CONTENT_TYPE, contentType.toContentTypeString());
            LOGGER.error(oDataServerError.getMessage(), oDataServerError);
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
            // This should never happen but to be sure we have this catch here to prevent sending a stacktrace to a client.
            final String responseContent = "{\"error\":{\"code\":null,\"message\":\"An unexpected exception occurred\"}}";
            oDataResponse.setContent(new ByteArrayInputStream(responseContent.getBytes(StandardCharsets.UTF_8)));
            oDataResponse.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode());
            oDataResponse.setHeader(HttpHeader.CONTENT_TYPE, ContentType.APPLICATION_JSON.toContentTypeString());
        }
    }
}
