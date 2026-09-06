package com.flowtwin.simulation;

public record TimelinePoint(int minute, int triageQueue, int treatmentQueue, int bedsOccupied) {
    /** Preserves source compatibility with the original triage/bed timeline. */
    public TimelinePoint(int minute, int triageQueue, int bedsOccupied) {
        this(minute, triageQueue, 0, bedsOccupied);
    }
}
