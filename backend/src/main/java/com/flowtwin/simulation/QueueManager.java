package com.flowtwin.simulation;

import java.util.PriorityQueue;

public class QueueManager {

    private PriorityQueue<Patient> triageQueue;
    private PriorityQueue<Patient> treatmentQueue;

    public QueueManager() {
        triageQueue = new PriorityQueue<>(
                (p1, p2) -> Integer.compare(p2.getAcuity(), p1.getAcuity())
        );

        treatmentQueue = new PriorityQueue<>(
                (p1, p2) -> Integer.compare(p2.getAcuity(), p1.getAcuity())
        );
    }

    public void addToTriageQueue(Patient patient) {
        triageQueue.add(patient);
    }

    public Patient getNextTriagePatient() {
        return triageQueue.poll();
    }

    public boolean isTriageQueueEmpty() {
        return triageQueue.isEmpty();
    }

    public int getTriageQueueSize() {
        return triageQueue.size();
    }

    public void addToTreatmentQueue(Patient patient) {
        treatmentQueue.add(patient);
    }

    public Patient getNextTreatmentPatient() {
        return treatmentQueue.poll();
    }

    public boolean isTreatmentQueueEmpty() {
        return treatmentQueue.isEmpty();
    }

    public int getTreatmentQueueSize() {
        return treatmentQueue.size();
    }
}