package com.foodfusion.kafkaconsumer.consumer;

import com.foodfusion.avro.generated.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderConsumer {
    @KafkaListener(topics = "order-topic"
            ,autoStartup = "${orderConsumer.startup:true}"
            ,groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(ConsumerRecord<String, OrderEvent> consumerRecord){
        log.info("ConsumerRecord key: {} , value: {} ", consumerRecord.key(), consumerRecord.value());
    }
}
