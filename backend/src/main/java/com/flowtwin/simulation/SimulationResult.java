package com.flowtwin.simulation;

public class SimulationResult {

    private int totalPatients;

    private double averageTriageWait;
    private double averageTreatmentWait;
    private double averageLengthOfStay;

    private int peakTriageQueue;
    private int peakTreatmentQueue;

    public SimulationResult(
            int totalPatients,
            double averageTriageWait,
            double averageTreatmentWait,
            double averageLengthOfStay,
            int peakTriageQueue,
            int peakTreatmentQueue) {

        this.totalPatients = totalPatients;
        this.averageTriageWait = averageTriageWait;
        this.averageTreatmentWait = averageTreatmentWait;
        this.averageLengthOfStay = averageLengthOfStay;
        this.peakTriageQueue = peakTriageQueue;
        this.peakTreatmentQueue = peakTreatmentQueue;
    }

    public int getTotalPatients() {
        return totalPatients;
    }

    public double getAverageTriageWait() {
        return averageTriageWait;
    }

    public double getAverageTreatmentWait() {
        return averageTreatmentWait;
    }

    public double getAverageLengthOfStay() {
        return averageLengthOfStay;
    }

    public int getPeakTriageQueue() {
        return peakTriageQueue;
    }

    public int getPeakTreatmentQueue() {
        return peakTreatmentQueue;
    }

    @Override
    public String toString() {

        return "\n===== SIMULATION RESULT =====\n" +
                "Total Patients       : " + totalPatients + "\n" +
                "Average Triage Wait  : " + averageTriageWait + " min\n" +
                "Average Treatment Wait: " + averageTreatmentWait + " min\n" +
                "Average Length of Stay: " + averageLengthOfStay + " min\n" +
                "Peak Triage Queue    : " + peakTriageQueue + "\n" +
                "Peak Treatment Queue : " + peakTreatmentQueue + "\n";
    }
}