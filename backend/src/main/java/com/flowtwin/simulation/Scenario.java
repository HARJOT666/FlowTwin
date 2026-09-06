package com.flowtwin.simulation;

public class Scenario {

    private String name;

    private int numberOfNurses;
    private int numberOfDoctors;
    private int numberOfBeds;

    public Scenario(
            String name,
            int numberOfNurses,
            int numberOfDoctors,
            int numberOfBeds) {

        this.name = name;
        this.numberOfNurses = numberOfNurses;
        this.numberOfDoctors = numberOfDoctors;
        this.numberOfBeds = numberOfBeds;
    }

    public String getName() {
        return name;
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

    public SimulationConfig createConfig(long seed) {

        return new SimulationConfig(
                numberOfNurses,
                numberOfDoctors,
                numberOfBeds,
                seed
        );
    }
}