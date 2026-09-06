package com.flowtwin.simulation;

public class Event implements Comparable<Event> {

    private double time;
    private EventType type;
    private Patient patient;

    public Event(double time, EventType type, Patient patient) {
        this.time = time;
        this.type = type;
        this.patient = patient;
    }

    public double getTime() {
        return time;
    }

    public EventType getType() {
        return type;
    }

    public Patient getPatient() {
        return patient;
    }

    @Override
    public int compareTo(Event other) {
        return Double.compare(this.time, other.time);
    }
}