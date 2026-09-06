package com.flowtwin.twin;

public record TwinState(
        int patientsInDept,
        int triageQueue,
        int bedsOccupied,
        int nurses,
        int beds,
        int doctors,
        double observedArrivalRatePerHour
) {}
