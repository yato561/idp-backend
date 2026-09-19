package com.idp.backend.util.async;


import com.idp.backend.dto.MetricRequest;
import com.idp.backend.service.MetricService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaMetricConsumer {

    private final MetricService metricService;

    public KafkaMetricConsumer(MetricService metricService){
        this.metricService = metricService;
    }

    @KafkaListener(topics = "metrics-topic", groupId = "idp-backend-group")
    public void consume(MetricRequest request){
        metricService.ingestInternal(request);
    }
}
