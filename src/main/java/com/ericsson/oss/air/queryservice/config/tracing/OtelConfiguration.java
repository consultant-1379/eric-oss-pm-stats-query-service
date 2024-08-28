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

package com.ericsson.oss.air.queryservice.config.tracing;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import io.micrometer.observation.ObservationRegistry;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.extension.trace.jaeger.sampler.JaegerRemoteSampler;
import io.opentelemetry.sdk.trace.samplers.Sampler;

/**
 * Configuration for DST Tracing.
 * <p>
 * Contains methods to configure export protocol and remote sampling strategy
 */
@Configuration
public class OtelConfiguration {
    private final Environment environment;

    /**
     * OtelConfiguration.
     */
    @Autowired
    public OtelConfiguration(final Environment environment) {
        this.environment = environment;
    }

    /**
     * This function is for use grpc (port 4317) channel for span export.
     * @return returns OtlpGrpcSpanExporter
     */
    @Bean
    @ConditionalOnExpression("${ericsson.tracing.enabled} && 'grpc'.equals('${ericsson.tracing.exporter.protocol}')")
    public OtlpGrpcSpanExporter otlpExporterGrpc() {
        return OtlpGrpcSpanExporter.builder()
            .setEndpoint(environment.getProperty("ericsson.tracing.exporter.endpoint", "http://eric-dst-collector:4317"))
            .build();
    }

    /**
     * This function is for use http (port 4318) channel for span export.
     * @return returns OtlpHttpSpanExporter
     */
    @Bean
    @ConditionalOnExpression("${ericsson.tracing.enabled} && 'http'.equals('${ericsson.tracing.exporter.protocol}')")
    public OtlpHttpSpanExporter otlpExporterHttp() {
        return OtlpHttpSpanExporter.builder()
            .setEndpoint(environment.getProperty("ericsson.tracing.exporter.endpoint", "http://eric-dst-collector:4318"))
            .build();
    }

    /**
     * This function is to pass spans across jaeger UI.
     * @return returns JaegerRemoteSampler
     */
    @Bean
    @ConditionalOnProperty(prefix = "ericsson.tracing", name = "enabled", havingValue = "true")
    public JaegerRemoteSampler jaegerRemoteSampler() {
        return JaegerRemoteSampler.builder()
            .setEndpoint(environment.getProperty("ericsson.tracing.sampler.jaeger-remote.endpoint", "http://eric-dst-collector:14250"))
            .setPollingInterval(Duration.ofSeconds(30))
            .setInitialSampler(Sampler.alwaysOff())
            .setServiceName(environment.getProperty("SERVICE_ID", "unknown_service"))
            .build();
    }

    /**
     * Skip the actuator health check logs.
     * @return returns ObservationRegistryCustomizer ObservationRegistry
     */
    @Bean
    @ConditionalOnProperty(prefix = "ericsson.tracing", name = "enabled", havingValue = "true")
    ObservationRegistryCustomizer<ObservationRegistry> skipActuatorEndpointsFromObservation() {
        final PathMatcher pathMatcher = new AntPathMatcher("/");
        return registry -> registry.observationConfig().observationPredicate((name, context) -> {
            if (context instanceof ServerRequestObservationContext observationContext) {
                return !pathMatcher.match("/actuator/**", observationContext.getCarrier().getRequestURI());
            } else {
                return true;
            }
        });
    }
}
