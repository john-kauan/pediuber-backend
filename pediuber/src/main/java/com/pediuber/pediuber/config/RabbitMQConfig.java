package com.pediuber.pediuber.config;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.MessageDeliveryMode;

@Configuration
public class RabbitMQConfig {

    public static final String INPUT_QUEUE = "ride.input.queue";
    public static final String OUTPUT_QUEUE = "ride.output.queue";

    public static final String INPUT_DLQ = "ride.input.dlq";
    public static final String OUTPUT_DLQ = "ride.output.dlq";

    @Bean
    public Queue inputQueue() {
        return QueueBuilder
                .durable(INPUT_QUEUE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(INPUT_DLQ)
                .build();
    }

    @Bean
    public Queue outputQueue() {
        return QueueBuilder
                .durable(OUTPUT_QUEUE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(OUTPUT_DLQ)
                .build();
    }

    @Bean
    public Queue inputDeadLetterQueue() {
        return QueueBuilder
                .durable(INPUT_DLQ)
                .build();
    }

    @Bean
    public Queue outputDeadLetterQueue() {
        return QueueBuilder
                .durable(OUTPUT_DLQ)
                .build();
    }

    @Bean
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter("com.pediuber.pediuber");
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {

        RabbitTemplate template = new RabbitTemplate(connectionFactory);

        template.setMessageConverter(messageConverter);

        template.setBeforePublishPostProcessors(message -> {
            message.getMessageProperties()
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT);

            return message;
        });

        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();

        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);

        factory.setDefaultRequeueRejected(false);

        return factory;
    }

    @Bean
    public ApplicationRunner runner(AmqpAdmin amqpAdmin) {
        return args -> {
            amqpAdmin.declareQueue(inputDeadLetterQueue());
            amqpAdmin.declareQueue(outputDeadLetterQueue());
            amqpAdmin.declareQueue(inputQueue());
            amqpAdmin.declareQueue(outputQueue());
        };
    }
}
