package com.bytegen.common.http.util;

import com.bytegen.common.metrics.MetricsCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: xiang
 * Date: 2018/10/19
 * Desc:
 */
public class ServiceMonitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceMonitor.class);

    private ServiceMonitor() {
    }

    public static void count(String serviceName, String uri, String method, int httpStatus, long latency) {
        try {
            MetricsCounter.setCounterCount(String.format("[%s][%s][%s][%s]", serviceName, method, uri, httpStatus), 1);

            MetricsCounter.setMeterCount(String.format("[%s][%s][%s]", serviceName, method, uri), 1);
            MetricsCounter.setTimerValue(String.format("[%s][%s][%s]", serviceName, method, uri), latency);
        } catch (Exception e) {
            LOGGER.error("service metrics counter error", e);
        }
    }
}
