package com.flowtwin.simulation;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Minimal but real discrete-event simulation of an ED as a two-stage queueing network:
 * arrival -> triage (nurses) -> bed/treatment (beds) -> discharge.
 * Deterministic given the same seed, so scenarios are comparable.
 *
 * This is the piece worth borrowing conceptually from ED-flow references; everything
 * around it is FlowTwin's own engineering.
 */
@Component
public class SimulationEngine {

    private enum EType { ARRIVAL, TRIAGE_DONE, DISCHARGE, TICK }

    private static final class Patient {
        double arrivalTime;
        double triageWait;
        double bedReadyTime;
        double bedWait;
    }

    private record SimEvent(double time, EType type, Patient patient) {}

    private static final int TICK_MINUTES = 5;

    public SimResult run(SimConfig c) {
        Random rng = new Random(c.seed());
        PriorityQueue<SimEvent> pq = new PriorityQueue<>(Comparator.comparingDouble(SimEvent::time));

        int freeNurses = c.nurses();
        int freeBeds = c.beds();
        Deque<Patient> triageQueue = new ArrayDeque<>();
        Deque<Patient> bedQueue = new ArrayDeque<>();

        List<Double> totalWaits = new ArrayList<>();
        double triageWaitSum = 0; int triageCount = 0;
        double bedWaitSum = 0; int bedCount = 0;
        int peakTriageQueue = 0;

        int occupiedBeds = 0;
        double busyBedTime = 0.0;
        double lastTime = 0.0;

        List<TimelinePoint> timeline = new ArrayList<>();

        double horizon = c.horizonMinutes();
        double meanInterArrival = 60.0 / Math.max(0.0001, c.arrivalRatePerHour());

        pq.add(new SimEvent(expo(rng, meanInterArrival), EType.ARRIVAL, null));
        pq.add(new SimEvent(TICK_MINUTES, EType.TICK, null));

        while (!pq.isEmpty()) {
            SimEvent e = pq.poll();
            if (e.time() > horizon) break;

            busyBedTime += occupiedBeds * (e.time() - lastTime);
            lastTime = e.time();

            switch (e.type()) {
                case ARRIVAL -> {
                    Patient p = new Patient();
                    p.arrivalTime = e.time();

                    double next = e.time() + expo(rng, meanInterArrival);
                    if (next <= horizon) pq.add(new SimEvent(next, EType.ARRIVAL, null));

                    if (freeNurses > 0) {
                        freeNurses--;
                        p.triageWait = 0;
                        triageCount++;
                        pq.add(new SimEvent(e.time() + expo(rng, c.meanTriageMinutes()), EType.TRIAGE_DONE, p));
                    } else {
                        triageQueue.addLast(p);
                    }
                }
                case TRIAGE_DONE -> {
                    Patient p = e.patient();
                    freeNurses++;

                    if (!triageQueue.isEmpty()) {
                        Patient nextP = triageQueue.pollFirst();
                        freeNurses--;
                        nextP.triageWait = e.time() - nextP.arrivalTime;
                        triageWaitSum += nextP.triageWait; triageCount++;
                        pq.add(new SimEvent(e.time() + expo(rng, c.meanTriageMinutes()), EType.TRIAGE_DONE, nextP));
                    }

                    p.bedReadyTime = e.time();
                    if (freeBeds > 0) {
                        freeBeds--; occupiedBeds++;
                        p.bedWait = 0; bedCount++;
                        totalWaits.add(p.triageWait + p.bedWait);
                        pq.add(new SimEvent(e.time() + expo(rng, c.meanTreatmentMinutes()), EType.DISCHARGE, p));
                    } else {
                        bedQueue.addLast(p);
                    }
                }
                case DISCHARGE -> {
                    freeBeds++; occupiedBeds--;
                    if (!bedQueue.isEmpty()) {
                        Patient nextP = bedQueue.pollFirst();
                        freeBeds--; occupiedBeds++;
                        nextP.bedWait = e.time() - nextP.bedReadyTime;
                        bedWaitSum += nextP.bedWait; bedCount++;
                        totalWaits.add(nextP.triageWait + nextP.bedWait);
                        pq.add(new SimEvent(e.time() + expo(rng, c.meanTreatmentMinutes()), EType.DISCHARGE, nextP));
                    }
                }
                case TICK -> {
                    timeline.add(new TimelinePoint((int) Math.round(e.time()), triageQueue.size(), occupiedBeds));
                    double nextTick = e.time() + TICK_MINUTES;
                    if (nextTick <= horizon) pq.add(new SimEvent(nextTick, EType.TICK, null));
                }
            }
            peakTriageQueue = Math.max(peakTriageQueue, triageQueue.size());
        }

        double avgTriage = triageCount == 0 ? 0 : triageWaitSum / triageCount;
        double avgBed = bedCount == 0 ? 0 : bedWaitSum / bedCount;
        double p90 = percentile(totalWaits);
        double util = c.beds() <= 0 ? 0 : (busyBedTime / (c.beds() * horizon)) * 100.0;

        return new SimResult(round(avgTriage), round(avgBed), round(p90), peakTriageQueue, round(util), timeline);
    }

    private static double expo(Random rng, double mean) {
        return -mean * Math.log(1 - rng.nextDouble());
    }

    private static double percentile(List<Double> xs) {
        if (xs.isEmpty()) return 0;
        List<Double> s = new ArrayList<>(xs);
        Collections.sort(s);
        int idx = (int) Math.ceil(0.90 * s.size()) - 1;
        idx = Math.max(0, Math.min(s.size() - 1, idx));
        return s.get(idx);
    }

    private static double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
