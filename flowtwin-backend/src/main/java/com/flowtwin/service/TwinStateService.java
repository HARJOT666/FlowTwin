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

    // --- Arrival-rate estimation settings --------------------------------------------------
    // Demand assumed during startup, before we have enough live data to trust.
    private static final double BASELINE_ARRIVALS_PER_HOUR = 12.0;
    // We only trust live data after we have been observing for at least this long...
    private static final long WARMUP_MILLIS = 2 * 60 * 1000L;   // 2 minutes
    // ...and after we have seen at least this many arrivals in the window.
    private static final int MIN_ARRIVALS_FOR_LIVE = 10;

    // When this service started observing events - used to measure the real window length.
    private final long startedAtMs = System.currentTimeMillis();

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

    public TwinState snapshot() {
        return new TwinState(
                patientsInDept.get(),
                triageQueue.get(),
                bedsOccupied.get(),
                nurses, beds, doctors,
                observedArrivalRatePerHour()
        );
    }

    /**
     * Estimates how many patients arrive per hour.
     *
     * <p>During startup we have too few live events for a meaningful estimate, so we return a
     * stable BASELINE demand. Once we have observed for long enough AND seen enough arrivals, we
     * switch to the REAL observed rate: the number of arrivals divided by how long we have
     * actually been watching (never more than one hour, because older arrivals are pruned).
     */
    public double observedArrivalRatePerHour() {
        pruneArrivals();

        long observedForMs = System.currentTimeMillis() - startedAtMs;
        int arrivalsInWindow = arrivals.size();

        // Still warming up: not enough time OR not enough events yet -> use a stable baseline.
        boolean notEnoughTime = observedForMs < WARMUP_MILLIS;
        boolean notEnoughEvents = arrivalsInWindow < MIN_ARRIVALS_FOR_LIVE;
        if (notEnoughTime || notEnoughEvents) {
            return BASELINE_ARRIVALS_PER_HOUR;
        }

        // Warmed up: use the real rate = arrivals / hours observed (window capped at one hour).
        long windowMs = Math.min(observedForMs, ONE_HOUR_MS);
        double windowHours = windowMs / (double) ONE_HOUR_MS;
        return arrivalsInWindow / windowHours;
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
