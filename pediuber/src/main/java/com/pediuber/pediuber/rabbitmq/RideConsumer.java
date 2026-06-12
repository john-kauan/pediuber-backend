package com.pediuber.pediuber.rabbitmq;

import com.pediuber.pediuber.config.RabbitMQConfig;
import com.pediuber.pediuber.dto.RideQueueMessage;
import com.pediuber.pediuber.entity.Ride;
import com.pediuber.pediuber.enums.RideStatus;
import com.pediuber.pediuber.logging.LogEvent;
import com.pediuber.pediuber.logging.LoggingService;
import com.pediuber.pediuber.pool.PendingRidePool;
import com.pediuber.pediuber.repository.RideRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RideConsumer {

    private final RideRepository rideRepository;
    private final PendingRidePool pendingRidePool;
    private final LoggingService loggingService;

    public RideConsumer(
            RideRepository rideRepository,
            PendingRidePool pendingRidePool,
            LoggingService loggingService
    ) {
        this.rideRepository = rideRepository;
        this.pendingRidePool = pendingRidePool;
        this.loggingService = loggingService;
    }

    @RabbitListener(queues = RabbitMQConfig.INPUT_QUEUE)
    public void consumeRide(RideQueueMessage message) {

        Ride ride = new Ride();

        ride.setOrigin(message.getOrigin());
        ride.setDestination(message.getDestination());
        ride.setStatus(RideStatus.REQUESTED);
        ride.setCreatedAt(LocalDateTime.now());

        Ride savedRide = rideRepository.save(ride);

        pendingRidePool.addRide(savedRide);

        loggingService.info(
                new LogEvent(
                        LocalDateTime.now().toString(),
                        "RIDE_RECEIVED_FROM_INPUT_QUEUE",
                        savedRide.getId(),
                        "PediUber",
                        null,
                        RideStatus.REQUESTED.name(),
                        null
                )
        );

        System.out.println("Corrida recebida da fila de entrada e adicionada ao pool:");
        System.out.println("Mensagem original: " + message);
        System.out.println("Corrida local criada: " + savedRide.getId());
    }
}