package com.pediuber.pediuber.core.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class LamportClockService {

    private final AtomicLong clock = new AtomicLong(0);

    public long tick() {
        return clock.incrementAndGet();
    }

    public long update(long receivedTimestamp) {

        return clock.updateAndGet(
                current -> Math.max(current, receivedTimestamp) + 1
        );
    }

    public long nextAfter(Long receivedTimestamp) {

        long safeTimestamp = receivedTimestamp == null ? 0L : receivedTimestamp;

        return update(safeTimestamp);
    }

    public long current() {

        return clock.get();
    }

}
