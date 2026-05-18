package com.najim.webhookservice.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor

public class PushEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(Map<String, Object> payload) {
        kafkaTemplate.send("push.event", payload);
        System.out.println("Published push event to Kafka topic: push.event");
    }

    public KafkaTemplate<String, Object> getKafkaTemplate() {
        return kafkaTemplate;
    }
}