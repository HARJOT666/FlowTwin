package com.flowtwin.simulation;

public class SimulationConfig {

    private int numberOfNurses;
    private int numberOfDoctors;
    private int numberOfBeds;
    private long seed;

    public SimulationConfig(
            int numberOfNurses,
            int numberOfDoctors,
            int numberOfBeds,
            long seed) {

        this.numberOfNurses = numberOfNurses;
        this.numberOfDoctors = numberOfDoctors;
        this.numberOfBeds = numberOfBeds;
        this.seed = seed;
    }

    public int getNumberOfNurses() {
        return numberOfNurses;
    }

    public int getNumberOfDoctors() {
        return numberOfDoctors;
    }

    public int getNumberOfBeds() {
        return numberOfBeds;
    }

    public long getSeed() {
        return seed;
    }
}