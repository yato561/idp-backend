package com.idp.backend.util.async;


import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.idp.backend.dto.MetricRequest;

@Component
@Profile("kafka")
public class KafkaMetricProducer implements AsyncIngestor<MetricRequest> {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaMetricProducer(KafkaTemplate<String, Object> kafkaTemplate){
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void submit(MetricRequest payload){
        kafkaTemplate.send("metrics-topic", payload.getServiceId().toString(), payload);
    }
}
