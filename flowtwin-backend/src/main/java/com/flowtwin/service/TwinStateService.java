package com.flowtwin.service;

import com.flowtwin.ingestion.PatientEventMessage;
import com.flowtwin.twin.TwinState;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Authoritative in-memory model of the live department, updated from the event stream.
 * Headline metrics are mirrored to Redis for fast reads.
 */
@Service
public class TwinStateService {

    private volatile int nurses = 4;
    private volatile int beds = 20;
    private volatile int doctors = 3;

    private final AtomicInteger patientsInDept = new AtomicInteger();
    private final AtomicInteger triageQueue = new AtomicInteger();
    private final AtomicInteger bedsOccupied = new AtomicInteger();

    private final Deque<Long> arrivals = new ArrayDeque<>();
    private static final long ONE_HOUR_MS = 3_600_000L;

    private final StringRedisTemplate redis;

    public TwinStateService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public synchronized void apply(PatientEventMessage e) {
        switch (e.type()) {
            case PATIENT_ARRIVED -> {
                patientsInDept.incrementAndGet();
                triageQueue.incrementAndGet();
                recordArrival(e.occurredAtEpochMs());
            }
            case TRIAGED -> triageQueue.updateAndGet(v -> Math.max(0, v - 1));
            case BED_ASSIGNED -> bedsOccupied.updateAndGet(v -> Math.min(beds, v + 1));
            case DISCHARGED -> {
                patientsInDept.updateAndGet(v -> Math.max(0, v - 1));
                bedsOccupied.updateAndGet(v -> Math.max(0, v - 1));
            }
        }
        mirrorToRedis();
    }

    public synchronized TwinState snapshot() {
        return new TwinState(
                patientsInDept.get(),
                triageQueue.get(),
                bedsOccupied.get(),
                nurses, beds, doctors,
                observedArrivalRatePerHour()
        );
    }

    public synchronized double observedArrivalRatePerHour() {
        pruneArrivals();
        int count = arrivals.size();
        return count; // No arrivals means zero, not an invented demo arrival rate.
    }

    private void recordArrival(long epochMs) {
        arrivals.addLast(epochMs);
        pruneArrivals();
    }

    private void pruneArrivals() {
        long cutoff = System.currentTimeMillis() - ONE_HOUR_MS;
        while (!arrivals.isEmpty() && arrivals.peekFirst() < cutoff) {
            arrivals.pollFirst();
        }
    }

    private void mirrorToRedis() {
        redis.opsForValue().set("twin:triageQueue", String.valueOf(triageQueue.get()));
        redis.opsForValue().set("twin:bedsOccupied", String.valueOf(bedsOccupied.get()));
        redis.opsForValue().set("twin:patientsInDept", String.valueOf(patientsInDept.get()));
    }
}
