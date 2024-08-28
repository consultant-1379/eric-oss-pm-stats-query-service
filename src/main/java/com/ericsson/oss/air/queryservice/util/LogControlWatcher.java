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

package com.ericsson.oss.air.queryservice.util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * Component responsible for log level management.
 */
@Component
@Slf4j
public class LogControlWatcher {

    @Value("${logging.logcontrolfile}")
    @SuppressWarnings("PMD.ImmutableField")
    private String logControlFileName = "";
    private String currentLogLevel = "INFO";

    /**
     * Reads log control file content with ObjectMapper.
     */
    @Scheduled(fixedRate = 2000)
    public void reloadLogControlFile() {
        final ObjectMapper mapper = new ObjectMapper();
        final Path logControlFilePath = Path.of(logControlFileName);
        if (logControlFilePath.toFile().exists()) {
            try {
                final LogControl[] logControls =
                        mapper.readValue(logControlFilePath.toFile(), LogControl[].class);
                for (final LogControl logControl : logControls) {
                    updateLogLevel(logControl.getSeverity());
                }
            } catch (final IOException e) {
                log.error("Unable to read logControl file: " + logControlFileName, e);
            }
        }
    }

    /**
     * Updates log level.
     * @param severity logging level
     */
    private void updateLogLevel(final String severity) {
        if (currentLogLevel.equals(severity)) {
            log.debug("Severity is the same as before, no change needed");
            return;
        }

        final Level logLevel = Level.toLevel(severity.toUpperCase(Locale.US));
        if (!"DEBUG".equals(severity) && logLevel.equals(Level.DEBUG)) {
            log.error("Not supported log level: {}", severity);
            return;
        }
        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        for (final Logger logger : loggerContext.getLoggerList()) {
            logger.setLevel(logLevel);
        }
        currentLogLevel = logLevel.toString();
    }
}
