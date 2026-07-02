package com.pediuber.pediuber.metrics;

import com.pediuber.pediuber.config.RabbitMQConfig;
import com.pediuber.pediuber.repository.DriverRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PediUberMetricsService {

    private final DriverRepository driverRepository;
    private final RabbitTemplate rabbitTemplate;

    private final Counter localRidesCounter;
    private final Counter delegatedOutRidesCounter;
    private final Counter delegatedInRidesCounter;
    private final Counter instanceRequestsCounter;

    private final AtomicInteger serviceAvailable = new AtomicInteger(1);
    private final AtomicInteger serviceCongested = new AtomicInteger(0);

    private final AtomicInteger inputQueueSize = new AtomicInteger(0);
    private final AtomicInteger outputQueueSize = new AtomicInteger(0);
    private final AtomicInteger inputDeadLetterQueueSize = new AtomicInteger(0);
    private final AtomicInteger outputDeadLetterQueueSize = new AtomicInteger(0);

    public PediUberMetricsService(
            MeterRegistry meterRegistry,
            DriverRepository driverRepository,
            RabbitTemplate rabbitTemplate
    ) {
        this.driverRepository = driverRepository;
        this.rabbitTemplate = rabbitTemplate;

        this.localRidesCounter = Counter.builder("pediuber_rides_local_total")
                .description("Total de corridas atendidas localmente pelo PediUber")
                .register(meterRegistry);

        this.delegatedOutRidesCounter = Counter.builder("pediuber_rides_delegated_out_total")
                .description("Total de corridas delegadas pelo PediUber para o Core ou outros grupos")
                .register(meterRegistry);

        this.delegatedInRidesCounter = Counter.builder("pediuber_rides_delegated_in_total")
                .description("Total de corridas recebidas pelo PediUber por delegação do Core ou outros grupos")
                .register(meterRegistry);

        this.instanceRequestsCounter = Counter.builder("pediuber_instance_requests_total")
                .description("Total de requisições recebidas por esta instância do PediUber")
                .register(meterRegistry);

        Gauge.builder("pediuber_service_available", serviceAvailable, AtomicInteger::get)
                .description("Estado atual do serviço: 1 disponível, 0 indisponível")
                .register(meterRegistry);

        Gauge.builder("pediuber_service_congested", serviceCongested, AtomicInteger::get)
                .description("Estado atual do serviço: 1 congestionado, 0 normal")
                .register(meterRegistry);

        Gauge.builder("pediuber_queue_in_size", inputQueueSize, AtomicInteger::get)
                .description("Tamanho atual da fila real de entrada no RabbitMQ")
                .register(meterRegistry);

        Gauge.builder("pediuber_queue_out_size", outputQueueSize, AtomicInteger::get)
                .description("Tamanho atual da fila real de saída no RabbitMQ")
                .register(meterRegistry);

        Gauge.builder("pediuber_queue_in_dlq_size", inputDeadLetterQueueSize, AtomicInteger::get)
                .description("Tamanho atual da fila de erro da entrada no RabbitMQ")
                .register(meterRegistry);

        Gauge.builder("pediuber_queue_out_dlq_size", outputDeadLetterQueueSize, AtomicInteger::get)
                .description("Tamanho atual da fila de erro da saída no RabbitMQ")
                .register(meterRegistry);
    }

    public void incrementLocalRide() {
        localRidesCounter.increment();
    }

    public void incrementDelegatedOutRide() {
        delegatedOutRidesCounter.increment();
    }

    public void incrementDelegatedInRide() {
        delegatedInRidesCounter.increment();
    }

    public void incrementInstanceRequest() {
        instanceRequestsCounter.increment();
    }

    @Scheduled(fixedDelay = 5000)
    public void refreshServiceState() {
        long availableDrivers = driverRepository.countByAvailableTrue();

        boolean available = availableDrivers > 0;
        boolean congested = availableDrivers == 0;

        serviceAvailable.set(available ? 1 : 0);
        serviceCongested.set(congested ? 1 : 0);
    }

    @Scheduled(fixedDelay = 5000)
    public void refreshQueueSizes() {
        inputQueueSize.set(getQueueMessageCount(RabbitMQConfig.INPUT_QUEUE));
        outputQueueSize.set(getQueueMessageCount(RabbitMQConfig.OUTPUT_QUEUE));
        inputDeadLetterQueueSize.set(getQueueMessageCount(RabbitMQConfig.INPUT_DLQ));
        outputDeadLetterQueueSize.set(getQueueMessageCount(RabbitMQConfig.OUTPUT_DLQ));
    }

    private int getQueueMessageCount(String queueName) {
        try {
            Integer messageCount = rabbitTemplate.execute(channel ->
                    channel.queueDeclarePassive(queueName).getMessageCount()
            );

            return messageCount == null ? 0 : messageCount;
        } catch (Exception exception) {
            return 0;
        }
    }
}