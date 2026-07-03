package com.pediuber.pediuber.core.contract;

import com.pediuber.pediuber.core.dto.RideAssignment;
import com.pediuber.pediuber.core.dto.RideAuctionNotification;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreContractTests {

    @Test
    void rideAuctionNotificationShouldKeepExpectedCoreFields() {
        assertFields(
                RideAuctionNotification.class,
                "rideUuid",
                "origin",
                "destination",
                "originServiceId",
                "passengerId",
                "logicalTimestamp",
                "auctionDeadline"
        );
    }

    @Test
    void rideAssignmentShouldKeepExpectedCoreFields() {
        assertFields(
                RideAssignment.class,
                "rideUuid",
                "origin",
                "destination",
                "passengerId",
                "originServiceId",
                "logicalTimestamp",
                "lockExpiresAt"
        );
    }

    private void assertFields(Class<?> clazz, String... expectedFields) {
        Set<String> actualFields = Arrays.stream(clazz.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

        for (String expectedField : expectedFields) {
            assertTrue(
                    actualFields.contains(expectedField),
                    "Campo obrigatório ausente no contrato do Core: " + expectedField
            );
        }
    }
}