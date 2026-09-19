package com.idp.backend.util.async;

import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.idp.backend.dto.LogRequest;

@Component
@Profile("kafka")
public class KafkaLogProducer implements AsyncIngestor<LogRequest> {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaLogProducer(KafkaTemplate<String, Object> kafkaTemplate){
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void submit(LogRequest request){
        kafkaTemplate.send("logs-topic", request.getServiceId().toString(), request);
    }
}
