package com.flowtwin.simulation;

/** Complete, immutable input for one deterministic simulation run. */
public record SimConfig(
        int horizonMinutes,
        double arrivalRatePerHour,
        double meanTriageMinutes,
        double meanTreatmentMinutes,
        int nurses,
        int beds,
        int doctors,
        long seed,
        int initialTriageQueue,
        int initialBedsOccupied,
        int initialPatientsInDept
) {
    /** Backwards-compatible constructor for callers that simulate from an empty department. */
    public SimConfig(int horizonMinutes, double arrivalRatePerHour, double meanTriageMinutes,
                     double meanTreatmentMinutes, int nurses, int beds, int doctors, long seed) {
        this(horizonMinutes, arrivalRatePerHour, meanTriageMinutes, meanTreatmentMinutes,
                nurses, beds, doctors, seed, 0, 0, 0);
    }
}
