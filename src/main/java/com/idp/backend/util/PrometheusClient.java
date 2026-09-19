package com.idp.backend.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client to fetch metrics from Prometheus
 * Pulls CPU, Memory, and other metrics for services
 */
@Slf4j
@Component
public class PrometheusClient {

    @Value("${prometheus.url:http://localhost:9090}")
    private String prometheusUrl;

    private final RestClient restClient;

    public PrometheusClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    /**
     * Query Prometheus for a specific metric
     * @param query PromQL query string
     * @return Map containing result data
     */
    public Map<String, Object> query(String query) {
        try {
            log.info("Querying Prometheus: {}", query);
            
            String url = prometheusUrl + "/api/v1/query?query=" + java.net.URLEncoder.encode(query, "UTF-8");
            
            Map<String, Object> response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(Map.class);
            
            return response != null ? response : new HashMap<>();
        } catch (Exception e) {
            log.error("Error querying Prometheus", e);
            return new HashMap<>();
        }
    }

    /**
     * Query Prometheus for a metric over a time range
     * @param query PromQL query string
     * @param startTime Start time in RFC3339 format
     * @param endTime End time in RFC3339 format
     * @param step Query resolution step
     * @return Map containing result data
     */
    public Map<String, Object> queryRange(String query, String startTime, String endTime, String step) {
        try {
            log.info("Querying Prometheus range: query={}, start={}, end={}, step={}", 
                    query, startTime, endTime, step);
            
            String url = prometheusUrl + "/api/v1/query_range?" +
                    "query=" + java.net.URLEncoder.encode(query, "UTF-8") +
                    "&start=" + startTime +
                    "&end=" + endTime +
                    "&step=" + step;
            
            Map<String, Object> response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(Map.class);
            
            return response != null ? response : new HashMap<>();
        } catch (Exception e) {
            log.error("Error querying Prometheus range", e);
            return new HashMap<>();
        }
    }

    /**
     * Get CPU usage for a service from Prometheus
     * @param serviceName Service name label
     * @return CPU percentage
     */
    public Double getCpuUsage(String serviceName) {
        try {
            Map<String, Object> result = query(
                    "avg(rate(container_cpu_usage_seconds_total{pod=~\"" + serviceName + ".*\"}[5m])) * 100"
            );
            return extractMetricValue(result);
        } catch (Exception e) {
            log.error("Error fetching CPU usage for service: {}", serviceName, e);
            return null;
        }
    }

    /**
     * Get Memory usage for a service from Prometheus
     * @param serviceName Service name label
     * @return Memory in MB
     */
    public Long getMemoryUsage(String serviceName) {
        try {
            Map<String, Object> result = query(
                    "avg(container_memory_usage_bytes{pod=~\"" + serviceName + ".*\"}) / 1024 / 1024"
            );
            Double value = extractMetricValue(result);
            return value != null ? value.longValue() : null;
        } catch (Exception e) {
            log.error("Error fetching memory usage for service: {}", serviceName, e);
            return null;
        }
    }

    /**
     * Check service health from Prometheus
     * @param serviceName Service name label
     * @return true if service is up
     */
    public Boolean isServiceUp(String serviceName) {
        try {
            Map<String, Object> result = query(
                    "up{job=\"" + serviceName + "\"}"
            );
            Double value = extractMetricValue(result);
            return value != null && value > 0;
        } catch (Exception e) {
            log.error("Error checking service health: {}", serviceName, e);
            return null;
        }
    }

    /**
     * Extract metric value from Prometheus response
     */
    @SuppressWarnings("unchecked")
    private Double extractMetricValue(Map<String, Object> response) {
        try {
            if (response == null || response.isEmpty()) {
                return null;
            }

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            if (data == null) {
                return null;
            }

            List<Map<String, Object>> result = (List<Map<String, Object>>) data.get("result");
            if (result == null || result.isEmpty()) {
                return null;
            }

            Map<String, Object> firstResult = result.get(0);
            List<Object> value = (List<Object>) firstResult.get("value");
            if (value != null && value.size() > 1) {
                return Double.parseDouble(value.get(1).toString());
            }
        } catch (Exception e) {
            log.error("Error extracting metric value from response", e);
        }
        return null;
    }
}
