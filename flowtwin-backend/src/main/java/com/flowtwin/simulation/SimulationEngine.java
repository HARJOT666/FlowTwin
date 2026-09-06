package com.flowtwin.simulation;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * Deterministic ED discrete-event simulation adapted from Ujjwal's acuity/resource engine.
 * Flow: arrival -> acuity-priority triage -> doctor+bed treatment -> discharge.
 * Each run owns all mutable state, so the singleton Spring component is safe for concurrent scenarios.
 */
@Component
public class SimulationEngine {
    private static final int TICK_MINUTES = 5;

    public SimResult run(SimConfig config) {
        validate(config);
        return new Run(config).execute();
    }

    private static void validate(SimConfig c) {
        if (c.horizonMinutes() <= 0 || c.arrivalRatePerHour() < 0
                || c.meanTriageMinutes() <= 0 || c.meanTreatmentMinutes() <= 0
                || c.nurses() < 0 || c.beds() < 0 || c.doctors() < 0
                || c.initialTriageQueue() < 0 || c.initialBedsOccupied() < 0
                || c.initialPatientsInDept() < 0) {
            throw new IllegalArgumentException("Simulation values must be nonnegative and durations positive");
        }
    }

    private enum Type { ARRIVAL, TRIAGE_DONE, TREATMENT_DONE, INITIAL_TREATMENT_DONE, TICK }

    private static final class Patient {
        final long id;
        final int acuity;
        final double arrivalTime;
        double triageWait;
        double treatmentReadyTime;
        double treatmentWait;

        Patient(long id, int acuity, double arrivalTime) {
            this.id = id;
            this.acuity = acuity;
            this.arrivalTime = arrivalTime;
        }
    }

    private record Event(double time, long sequence, Type type, Patient patient, boolean releasesDoctor) {}

    private static final Comparator<Patient> PATIENT_PRIORITY = Comparator
            .comparingInt((Patient p) -> p.acuity).reversed()
            .thenComparingDouble(p -> p.arrivalTime)
            .thenComparingLong(p -> p.id);

    private static final class Run {
        private final SimConfig c;
        private final Random random;
        private final PriorityQueue<Event> events = new PriorityQueue<>(Comparator
                .comparingDouble(Event::time).thenComparingLong(Event::sequence));
        private final PriorityQueue<Patient> triageQueue = new PriorityQueue<>(PATIENT_PRIORITY);
        private final PriorityQueue<Patient> treatmentQueue = new PriorityQueue<>(PATIENT_PRIORITY);
        private final List<Double> queueWaits = new ArrayList<>();
        private final List<TimelinePoint> timeline = new ArrayList<>();
        private long sequence;
        private long patientId;
        private int freeNurses;
        private int freeDoctors;
        private int occupiedBeds;
        private int totalPatients;
        private int completedPatients;
        private int triageStarted;
        private int treatmentStarted;
        private int peakTriageQueue;
        private int peakTreatmentQueue;
        private double triageWaitTotal;
        private double treatmentWaitTotal;
        private double lengthOfStayTotal;
        private double busyBedMinutes;
        private double lastEventTime;

        Run(SimConfig config) {
            c = config;
            random = new Random(c.seed());
            freeNurses = c.nurses();
            occupiedBeds = Math.min(c.beds(), Math.min(c.initialBedsOccupied(), c.initialPatientsInDept()));
            int initiallyBusyDoctors = Math.min(c.doctors(), occupiedBeds);
            freeDoctors = c.doctors() - initiallyBusyDoctors;
            totalPatients = c.initialPatientsInDept();

            for (int i = 0; i < occupiedBeds; i++) {
                schedule(exponential(c.meanTreatmentMinutes()), Type.INITIAL_TREATMENT_DONE,
                        null, i < initiallyBusyDoctors);
            }
            int initialTriage = Math.min(c.initialTriageQueue(), Math.max(0, totalPatients - occupiedBeds));
            for (int i = 0; i < initialTriage; i++) triageQueue.add(new Patient(++patientId, 2, 0));
            int initialTreatment = Math.max(0, totalPatients - initialTriage - occupiedBeds);
            for (int i = 0; i < initialTreatment; i++) {
                Patient patient = new Patient(++patientId, 2, 0);
                patient.treatmentReadyTime = 0;
                treatmentQueue.add(patient);
            }
        }

        SimResult execute() {
            startTriage(0);
            startTreatment(0);
            updatePeaks();
            capture(0);
            if (c.arrivalRatePerHour() > 0) {
                schedule(exponential(60.0 / c.arrivalRatePerHour()), Type.ARRIVAL, null, false);
            }
            schedule(TICK_MINUTES, Type.TICK, null, false);

            while (!events.isEmpty()) {
                Event event = events.poll();
                if (event.time() > c.horizonMinutes()) break;
                accumulateUtilization(event.time());
                switch (event.type()) {
                    case ARRIVAL -> arrive(event.time());
                    case TRIAGE_DONE -> finishTriage(event.patient(), event.time());
                    case TREATMENT_DONE -> finishTreatment(event.patient(), event.time());
                    case INITIAL_TREATMENT_DONE -> finishInitialTreatment(event.time(), event.releasesDoctor());
                    case TICK -> {
                        capture(event.time());
                        if (event.time() + TICK_MINUTES <= c.horizonMinutes())
                            schedule(event.time() + TICK_MINUTES, Type.TICK, null, false);
                    }
                }
                updatePeaks();
            }
            accumulateUtilization(c.horizonMinutes());
            return result();
        }

        private void arrive(double time) {
            Patient patient = new Patient(++patientId, randomAcuity(), time);
            totalPatients++;
            triageQueue.add(patient);
            startTriage(time);
            double next = time + exponential(60.0 / c.arrivalRatePerHour());
            if (next <= c.horizonMinutes()) schedule(next, Type.ARRIVAL, null, false);
        }

        private void startTriage(double time) {
            while (freeNurses > 0 && !triageQueue.isEmpty()) {
                Patient patient = triageQueue.poll();
                freeNurses--;
                patient.triageWait = Math.max(0, time - patient.arrivalTime);
                triageWaitTotal += patient.triageWait;
                triageStarted++;
                schedule(time + exponential(c.meanTriageMinutes() * triageFactor(patient.acuity)),
                        Type.TRIAGE_DONE, patient, false);
            }
        }

        private void finishTriage(Patient patient, double time) {
            freeNurses++;
            patient.treatmentReadyTime = time;
            treatmentQueue.add(patient);
            startTriage(time);
            startTreatment(time);
        }

        private void startTreatment(double time) {
            while (occupiedBeds < c.beds() && freeDoctors > 0 && !treatmentQueue.isEmpty()) {
                Patient patient = treatmentQueue.poll();
                occupiedBeds++;
                freeDoctors--;
                patient.treatmentWait = Math.max(0, time - patient.treatmentReadyTime);
                treatmentWaitTotal += patient.treatmentWait;
                treatmentStarted++;
                queueWaits.add(patient.triageWait + patient.treatmentWait);
                schedule(time + exponential(c.meanTreatmentMinutes() * treatmentFactor(patient.acuity)),
                        Type.TREATMENT_DONE, patient, true);
            }
        }

        private void finishTreatment(Patient patient, double time) {
            occupiedBeds--;
            freeDoctors++;
            completedPatients++;
            lengthOfStayTotal += Math.max(0, time - patient.arrivalTime);
            startTreatment(time);
        }

        private void finishInitialTreatment(double time, boolean releasesDoctor) {
            occupiedBeds--;
            if (releasesDoctor) freeDoctors++;
            startTreatment(time);
        }

        private void schedule(double time, Type type, Patient patient, boolean releasesDoctor) {
            if (Double.isFinite(time)) events.add(new Event(time, sequence++, type, patient, releasesDoctor));
        }

        private void capture(double time) {
            timeline.add(new TimelinePoint((int) Math.round(time), triageQueue.size(),
                    treatmentQueue.size(), occupiedBeds));
        }

        private void updatePeaks() {
            peakTriageQueue = Math.max(peakTriageQueue, triageQueue.size());
            peakTreatmentQueue = Math.max(peakTreatmentQueue, treatmentQueue.size());
        }

        private void accumulateUtilization(double time) {
            busyBedMinutes += occupiedBeds * Math.max(0, time - lastEventTime);
            lastEventTime = time;
        }

        private int randomAcuity() {
            double value = random.nextDouble();
            return value < 0.10 ? 3 : value < 0.40 ? 2 : 1;
        }

        private double exponential(double mean) {
            return -mean * Math.log1p(-random.nextDouble());
        }

        private SimResult result() {
            double utilization = c.beds() == 0 ? 0
                    : busyBedMinutes / (c.beds() * c.horizonMinutes()) * 100.0;
            return new SimResult(
                    round(triageStarted == 0 ? 0 : triageWaitTotal / triageStarted),
                    round(treatmentStarted == 0 ? 0 : treatmentWaitTotal / treatmentStarted),
                    round(percentile(queueWaits)), peakTriageQueue, round(utilization), totalPatients,
                    completedPatients, round(completedPatients == 0 ? 0 : lengthOfStayTotal / completedPatients),
                    peakTreatmentQueue, List.copyOf(timeline));
        }
    }

    private static double triageFactor(int acuity) {
        return acuity == 3 ? 0.75 : acuity == 2 ? 0.9 : 1.1;
    }

    private static double treatmentFactor(int acuity) {
        return acuity == 3 ? 1.35 : acuity == 2 ? 1.0 : 0.75;
    }

    private static double percentile(List<Double> values) {
        if (values.isEmpty()) return 0;
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int index = Math.max(0, Math.min(sorted.size() - 1,
                (int) Math.ceil(0.90 * sorted.size()) - 1));
        return sorted.get(index);
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
