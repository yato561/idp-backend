package com.idp.backend.util.async;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.idp.backend.dto.LogRequest;
import com.idp.backend.service.LogsService;

@Component
public class KafkaLogConsumer {

    private final LogsService logsService;

    public KafkaLogConsumer(LogsService logsService){
        this.logsService = logsService;
    }

    @KafkaListener(topics = "logs-topic", groupId = "idp-backend-group")
    public void consume(LogRequest request){
        logsService.ingestInternal(request);
    }
}
