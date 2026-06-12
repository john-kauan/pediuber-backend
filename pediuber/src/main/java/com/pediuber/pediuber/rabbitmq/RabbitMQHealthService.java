package com.pediuber.pediuber.rabbitmq;

import com.pediuber.pediuber.config.RabbitMQConfig;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
public class RabbitMQHealthService {

    private final AmqpAdmin amqpAdmin;

    public RabbitMQHealthService(AmqpAdmin amqpAdmin) {
        this.amqpAdmin = amqpAdmin;
    }

    public int getInputQueueMessages() {
        return getMessageCount(RabbitMQConfig.INPUT_QUEUE);
    }

    public int getOutputQueueMessages() {
        return getMessageCount(RabbitMQConfig.OUTPUT_QUEUE);
    }

    public int getInputDlqMessages() {
        return getMessageCount(RabbitMQConfig.INPUT_DLQ);
    }

    public int getOutputDlqMessages() {
        return getMessageCount(RabbitMQConfig.OUTPUT_DLQ);
    }

    private int getMessageCount(String queueName) {

        if (!(amqpAdmin instanceof RabbitAdmin rabbitAdmin)) {
            return -1;
        }

        Properties properties = rabbitAdmin.getQueueProperties(queueName);

        if (properties == null) {
            return -1;
        }

        Object messageCount =
                properties.get(RabbitAdmin.QUEUE_MESSAGE_COUNT);

        if (messageCount instanceof Integer count) {
            return count;
        }

        return 0;
    }
}