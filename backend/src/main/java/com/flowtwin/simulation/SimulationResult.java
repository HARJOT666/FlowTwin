package com.flowtwin.simulation;

public class SimulationResult {

    private int totalPatients;

    private double averageTriageWait;
    private double averageTreatmentWait;
    private double averageLengthOfStay;

    private int peakTriageQueue;
    private int peakTreatmentQueue;

    private double nurseUtilization;
    private double doctorUtilization;
    private double bedUtilization;

    private String primaryBottleneck;

    public SimulationResult(
            int totalPatients,
            double averageTriageWait,
            double averageTreatmentWait,
            double averageLengthOfStay,
            int peakTriageQueue,
            int peakTreatmentQueue,
            double nurseUtilization,
            double doctorUtilization,
            double bedUtilization,
            String primaryBottleneck) {

        this.totalPatients = totalPatients;
        this.averageTriageWait = averageTriageWait;
        this.averageTreatmentWait = averageTreatmentWait;
        this.averageLengthOfStay = averageLengthOfStay;
        this.peakTriageQueue = peakTriageQueue;
        this.peakTreatmentQueue = peakTreatmentQueue;
        this.nurseUtilization = nurseUtilization;
        this.doctorUtilization = doctorUtilization;
        this.bedUtilization = bedUtilization;
        this.primaryBottleneck = primaryBottleneck;
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

    public double getNurseUtilization() {
        return nurseUtilization;
    }

    public double getDoctorUtilization() {
        return doctorUtilization;
    }

    public double getBedUtilization() {
        return bedUtilization;
    }

    public String getPrimaryBottleneck() {
        return primaryBottleneck;
    }

    @Override
    public String toString() {

        return "\n===== SIMULATION RESULT =====\n" +
                "Total Patients        : " + totalPatients + "\n" +
                "Average Triage Wait   : " + averageTriageWait + " min\n" +
                "Average Treatment Wait: " + averageTreatmentWait + " min\n" +
                "Average Length of Stay: " + averageLengthOfStay + " min\n" +
                "Peak Triage Queue     : " + peakTriageQueue + "\n" +
                "Peak Treatment Queue  : " + peakTreatmentQueue + "\n" +
                "Nurse Utilization     : " + nurseUtilization + "%\n" +
                "Doctor Utilization    : " + doctorUtilization + "%\n" +
                "Bed Utilization       : " + bedUtilization + "%\n" +
                "Primary Bottleneck    : " + primaryBottleneck + "\n";
    }
}