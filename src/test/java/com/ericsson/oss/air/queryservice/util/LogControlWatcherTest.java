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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import lombok.SneakyThrows;

@ActiveProfiles("test")
class LogControlWatcherTest {

    public static final String CURRENT_LOG_LEVEL = "currentLogLevel";
    private static final String LEVEL_DEBUG = "DEBUG";
    private static final String LEVEL_INFO = "INFO";

    private LogControlWatcher watcher;

    @BeforeEach
    void init() {
        watcher = new LogControlWatcher();
    }

    private void initLogControlFile(final String severity) throws IOException {
        final File tempFile = File.createTempFile("logcontrol", ".tmp");

        final String logControlFileContent =
                "[{\"container\": \"eric-oss-pm-stats-query-service\",\"severity\": \"" + severity + "\"}]";
        Files.write(tempFile.toPath(), logControlFileContent.getBytes(StandardCharsets.UTF_8));
        ReflectionTestUtils.setField(watcher, "logControlFileName", tempFile.getAbsolutePath());
    }

    @Test
    void whenValidLogControlFileChange_shouldLoggingLevelChange() throws IOException {
        initLogControlFile(LEVEL_DEBUG);

        assertEquals(LEVEL_INFO, ReflectionTestUtils.getField(watcher, CURRENT_LOG_LEVEL));

        watcher.reloadLogControlFile();

        assertEquals(LEVEL_DEBUG, ReflectionTestUtils.getField(watcher, CURRENT_LOG_LEVEL));
    }

    @Test
    void whenValidLogControlFileIsUnchanged_shouldRespondNoChangeNeeded() throws  IOException {
        initLogControlFile(LEVEL_INFO);

        assertEquals(LEVEL_INFO, ReflectionTestUtils.getField(watcher, CURRENT_LOG_LEVEL));

        watcher.reloadLogControlFile();

        assertEquals(LEVEL_INFO, ReflectionTestUtils.getField(watcher, CURRENT_LOG_LEVEL));
    }

    @Test
    void whenInvalidLogControlFileChange_shouldLoggingLevelNotChange() throws IOException {
        initLogControlFile("BAD_LOGGING_LEVEL");

        assertEquals(LEVEL_INFO, ReflectionTestUtils.getField(watcher, CURRENT_LOG_LEVEL));

        watcher.reloadLogControlFile();

        assertEquals(LEVEL_INFO, ReflectionTestUtils.getField(watcher, CURRENT_LOG_LEVEL));
    }

    @SneakyThrows
    @Test
    @DisplayName("Log errors when the control file content is invalid")
    void whenInvalidControlFile_shouldLogException() {
        final File tempFile = File.createTempFile("logcontrol", ".tmp");
        final String invalidFileContent = "[{\"container\": ";
        final LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        final TestLogAppender appender = new TestLogAppender();
        appender.setContext(ctx);
        ctx.getLogger(LogControlWatcher.class).addAppender(appender);
        appender.start();

        Files.write(tempFile.toPath(), invalidFileContent.getBytes(StandardCharsets.UTF_8));
        ReflectionTestUtils.setField(watcher, "logControlFileName", tempFile.getAbsolutePath());

        assertEquals(LEVEL_INFO, ReflectionTestUtils.getField(watcher, CURRENT_LOG_LEVEL));

        watcher.reloadLogControlFile();

        assertThat(appender.getSize()).isGreaterThan(0);
        assertThat(appender.getEvents().get(0).getMessage()).startsWith("Unable to read logControl file");
    }

    private static class TestLogAppender extends ListAppender<ILoggingEvent> {
        public int getSize() {
            return this.list.size();
        }

        public List<ILoggingEvent> getEvents() {
            return Collections.unmodifiableList(this.list);
        }
    }
}
