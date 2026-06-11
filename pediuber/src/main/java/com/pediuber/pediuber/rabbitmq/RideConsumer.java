package com.pediuber.pediuber.rabbitmq;

import com.pediuber.pediuber.config.RabbitMQConfig;
import com.pediuber.pediuber.dto.RideQueueMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class RideConsumer {

    @RabbitListener(queues = RabbitMQConfig.INPUT_QUEUE)
    public void consumeRide(RideQueueMessage message) {

        System.out.println("Corrida recebida da fila de entrada:");
        System.out.println(message);
    }
}