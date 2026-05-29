package com.pediuber.pediuber.pool;


import com.pediuber.pediuber.entity.Ride;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.Queue;

@Component
public class PendingRidePool {

    private final Queue<Ride> pendingRides = new LinkedList<>();

    public void addRide(Ride ride) {
        pendingRides.add(ride);
    }

    public Ride getNextRide() {
        return pendingRides.poll();
    }

    public boolean hasPendingRides() {
        return !pendingRides.isEmpty();
    }

    public int size() {
        return pendingRides.size();
    }

}
