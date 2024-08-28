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

package com.ericsson.oss.air.queryservice.controller;

import static com.ericsson.oss.air.queryservice.service.QueryService.CUSTOM_METRIC_PREFIX;

import java.util.ArrayList;

import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataHttpHandler;
import org.apache.olingo.server.api.ServiceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ericsson.oss.air.queryservice.olingo.CustomEdmProvider;
import com.ericsson.oss.air.queryservice.olingo.CustomEntityCollectionProcessor;
import com.ericsson.oss.air.queryservice.olingo.CustomErrorProcessor;
import com.ericsson.oss.orchestration.eo.logging.LoggerHandler;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Class responsible for receiving and handling OData requests.
 */
@RestController
public class ODataController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ODataController.class);
    private static final String BASE_URL = "/kpi-handling/exposure/v1/";
    private Boolean isInitialized = false;

    @Autowired
    private CustomEdmProvider customEdmProvider;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private CustomEntityCollectionProcessor customEntityCollectionProcessor;

    @Autowired
    private CustomErrorProcessor errorProcessor;

    @Autowired
    private LoggerHandler loggerHandler;

    /**
     * REST endpoint method for OData requests.
     * @param request the servlet request with the uri containing the targeted schema and the OData query options
     * @param response the servlet response built by the OData entity processor
     */
    @GetMapping("**")
    public void serveOData(final HttpServletRequest request, final HttpServletResponse response) {
        loggerHandler.logAudit(LOGGER, String.format("GET request received at: %s", request.getRequestURI()), request);

        final OData odata = OData.newInstance();

        final String schema = obtainSchemaFromUri(request.getRequestURI());
        customEdmProvider.setSchemaName(schema);
        final ServiceMetadata edm = odata.createServiceMetadata(customEdmProvider, new ArrayList<>());
        final ODataHttpHandler handler = odata.createHandler(edm);

        customEntityCollectionProcessor.setSchema(schema);
        handler.register(customEntityCollectionProcessor);
        handler.register(errorProcessor);
        handler.process(new HttpServletRequestWrapper(request) {
            @Override
            public String getServletPath() {
                return BASE_URL + schema;
            }
        }, response);
        if (!isInitialized) {
            meterRegistry.remove(meterRegistry.get(CUSTOM_METRIC_PREFIX + "served_queries_counter").counter());
            isInitialized = true;
        }
        Metrics.counter(CUSTOM_METRIC_PREFIX + "served_queries_counter", "response_status", String.valueOf(response.getStatus())).increment();
        loggerHandler.logAudit(LOGGER, String.format("Sent response to GET request at: %s", request.getRequestURI()), request);
    }

    /**
     * Obtains the schema part of the request URI.
     * @param uri the full request uri
     * @return schema id
     */
    private String obtainSchemaFromUri(final String uri) {
        try {
            return uri.split("/")[4];
        } catch (final ArrayIndexOutOfBoundsException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not obtain schema from request", e);
        }
    }
}
