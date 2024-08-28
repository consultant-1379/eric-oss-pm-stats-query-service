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

package com.ericsson.oss.air.queryservice.config.retry;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.ExceptionClassifierRetryPolicy;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * Utility class for the retry template creation.
 */
public final class RetryTemplateUtil {
    private RetryTemplateUtil() {
    }

    /**
     * Creates a new RetryTemplate.
     *
     * @param backOffPeriod the back off period in milliseconds
     * @param maxAttempts the number of attempts before retries are exhausted
     * @param exemptions list of Throwable-s to be included in the exception classifier's policy map using a never-retry
     *                   policy
     * @return RetryTemplate
     */
    public static RetryTemplate buildRetryTemplate(final Integer backOffPeriod,
                                                   final Integer maxAttempts,
                                                   final List<Class<? extends Throwable>> exemptions) {
        final RetryTemplate retryTemplate = new RetryTemplate();

        final FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(backOffPeriod);
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);

        final SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy();
        simpleRetryPolicy.setMaxAttempts(maxAttempts);

        if (exemptions == null || exemptions.isEmpty()) {
            retryTemplate.setRetryPolicy(simpleRetryPolicy);
        } else {
            final Map<Class<? extends Throwable>, RetryPolicy> policyMap = exemptions.stream().collect(Collectors.toMap(
                    Function.identity(),
                    arg -> new NeverRetryPolicy()));
            policyMap.put(Exception.class, simpleRetryPolicy);

            final ExceptionClassifierRetryPolicy exceptionClassifierRetryPolicy = new ExceptionClassifierRetryPolicy();
            exceptionClassifierRetryPolicy.setPolicyMap(policyMap);

            retryTemplate.setRetryPolicy(exceptionClassifierRetryPolicy);
        }
        return retryTemplate;
    }

    /**
     * Creates a new RetryTemplate.
     *
     * @param backOffPeriod the back off period in milliseconds
     * @param maxAttempts the number of attempts before retries are exhausted
     * @return RetryTemplate
     */
    public static RetryTemplate buildRetryTemplate(final Integer backOffPeriod, final Integer maxAttempts) {
        return buildRetryTemplate(backOffPeriod, maxAttempts, Collections.emptyList());
    }
}
