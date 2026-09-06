package com.flowtwin.simulation;

public class ScenarioResult {

    private String scenarioName;

    private SimulationResult baselineResult;
    private SimulationResult scenarioResult;

    private double triageWaitImprovement;
    private double treatmentWaitImprovement;
    private double lengthOfStayImprovement;

    private double nurseUtilizationChange;
    private double doctorUtilizationChange;
    private double bedUtilizationChange;

    private int triageQueueChange;
    private int treatmentQueueChange;

    private String bottleneckBefore;
    private String bottleneckAfter;

    public ScenarioResult(
            String scenarioName,
            SimulationResult baselineResult,
            SimulationResult scenarioResult) {

        this.scenarioName = scenarioName;
        this.baselineResult = baselineResult;
        this.scenarioResult = scenarioResult;

        this.triageWaitImprovement =
                calculateImprovement(
                        baselineResult.getAverageTriageWait(),
                        scenarioResult.getAverageTriageWait());

        this.treatmentWaitImprovement =
                calculateImprovement(
                        baselineResult.getAverageTreatmentWait(),
                        scenarioResult.getAverageTreatmentWait());

        this.lengthOfStayImprovement =
                calculateImprovement(
                        baselineResult.getAverageLengthOfStay(),
                        scenarioResult.getAverageLengthOfStay());

        // Utilization change is expressed in percentage points.
        this.nurseUtilizationChange =
                scenarioResult.getNurseUtilization()
                        - baselineResult.getNurseUtilization();

        this.doctorUtilizationChange =
                scenarioResult.getDoctorUtilization()
                        - baselineResult.getDoctorUtilization();

        this.bedUtilizationChange =
                scenarioResult.getBedUtilization()
                        - baselineResult.getBedUtilization();

        this.triageQueueChange =
                scenarioResult.getPeakTriageQueue()
                        - baselineResult.getPeakTriageQueue();

        this.treatmentQueueChange =
                scenarioResult.getPeakTreatmentQueue()
                        - baselineResult.getPeakTreatmentQueue();

        this.bottleneckBefore =
                baselineResult.getPrimaryBottleneck();

        this.bottleneckAfter =
                scenarioResult.getPrimaryBottleneck();
    }

    private double calculateImprovement(
            double baseline,
            double scenario) {

        if (baseline == 0) {
            return 0;
        }

        return ((baseline - scenario) / baseline) * 100;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public SimulationResult getBaselineResult() {
        return baselineResult;
    }

    public SimulationResult getScenarioResult() {
        return scenarioResult;
    }

    public double getTriageWaitImprovement() {
        return triageWaitImprovement;
    }

    public double getTreatmentWaitImprovement() {
        return treatmentWaitImprovement;
    }

    public double getLengthOfStayImprovement() {
        return lengthOfStayImprovement;
    }

    public double getNurseUtilizationChange() {
        return nurseUtilizationChange;
    }

    public double getDoctorUtilizationChange() {
        return doctorUtilizationChange;
    }

    public double getBedUtilizationChange() {
        return bedUtilizationChange;
    }

    public int getTriageQueueChange() {
        return triageQueueChange;
    }

    public int getTreatmentQueueChange() {
        return treatmentQueueChange;
    }

    public String getBottleneckBefore() {
        return bottleneckBefore;
    }

    public String getBottleneckAfter() {
        return bottleneckAfter;
    }

    @Override
    public String toString() {

        return "\n===== SCENARIO RESULT =====" +
                "\nScenario: " + scenarioName +

                "\n\n--- Baseline ---" +
                "\n" + baselineResult +

                "\n\n--- Scenario ---" +
                "\n" + scenarioResult +

                "\n\n--- Improvements ---" +
                "\nTriage Wait Improvement: "
                + round(triageWaitImprovement) + "%" +

                "\nTreatment Wait Improvement: "
                + round(treatmentWaitImprovement) + "%" +

                "\nLength of Stay Improvement: "
                + round(lengthOfStayImprovement) + "%" +

                "\n\n--- Resource Utilization Change ---" +
                "\nNurse Utilization Change: "
                + round(nurseUtilizationChange) + " percentage points" +

                "\nDoctor Utilization Change: "
                + round(doctorUtilizationChange) + " percentage points" +

                "\nBed Utilization Change: "
                + round(bedUtilizationChange) + " percentage points" +

                "\n\n--- Queue Change ---" +
                "\nPeak Triage Queue Change: "
                + triageQueueChange +

                "\nPeak Treatment Queue Change: "
                + treatmentQueueChange +

                "\n\n--- Bottleneck ---" +
                "\nBefore: " + bottleneckBefore +
                "\nAfter: " + bottleneckAfter;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}