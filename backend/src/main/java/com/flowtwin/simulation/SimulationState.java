package com.flowtwin.simulation;

import java.util.ArrayList;
import java.util.List;

public class SimulationState {

    private double currentTime;

    private final List<Patient> patients;
    private final List<Resource> nurses;
    private final List<Resource> doctors;
    private final List<Resource> beds;

    private final QueueManager queueManager;

    public SimulationState() {
        currentTime = 0.0;

        patients = new ArrayList<>();
        nurses = new ArrayList<>();
        doctors = new ArrayList<>();
        beds = new ArrayList<>();

        queueManager = new QueueManager();
    }

    public double getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(double currentTime) {
        this.currentTime = currentTime;
    }

    public List<Patient> getPatients() {
        return patients;
    }

    public List<Resource> getNurses() {
        return nurses;
    }

    public List<Resource> getDoctors() {
        return doctors;
    }

    public List<Resource> getBeds() {
        return beds;
    }

    public QueueManager getQueueManager() {
        return queueManager;
    }

    public void addPatient(Patient patient) {
        patients.add(patient);
    }

    public void addNurse(Resource nurse) {
        nurses.add(nurse);
    }

    public void addDoctor(Resource doctor) {
        doctors.add(doctor);
    }

    public void addBed(Resource bed) {
        beds.add(bed);
    }
}