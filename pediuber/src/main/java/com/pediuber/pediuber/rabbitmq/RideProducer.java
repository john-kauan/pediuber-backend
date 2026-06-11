package com.pediuber.pediuber.rabbitmq;

import com.pediuber.pediuber.config.RabbitMQConfig;
import com.pediuber.pediuber.dto.RideQueueMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RideProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendRideToInputQueue(RideQueueMessage message) {

        rabbitTemplate.convertAndSend(RabbitMQConfig.INPUT_QUEUE, message);

        System.out.println("Corrida enviada para a fila de entrada: " + message);
    }

    public void sendRideToOutputQueue(RideQueueMessage message) {

        rabbitTemplate.convertAndSend(RabbitMQConfig.OUTPUT_QUEUE, message);

        System.out.println("Corrida enviada para a fila de saída: " + message);
    }
}