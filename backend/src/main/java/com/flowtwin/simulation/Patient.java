package com.flowtwin.simulation;

public class Patient {

    private int id;
    private int acuity; // 1 = low, 2 = medium, 3 = high

    private double arrivalTime;
    private double triageStartTime;
    private double triageEndTime;
    private double treatmentStartTime;
    private double treatmentEndTime;
    private double dischargeTime;
    private Resource assignedNurse; 
    private Resource assignedDoctor;
    private Resource assignedBed;
    private double triageWaitTime;
    private double treatmentWaitTime;

    public Patient(int id, int acuity, double arrivalTime) {
        this.id = id;
        this.acuity = acuity;
        this.arrivalTime = arrivalTime;
    }

    public int getId() {
        return id;
    }

    public int getAcuity() {
        return acuity;
    }

    public double getArrivalTime() {
        return arrivalTime;
    }

    public double getTriageStartTime() {
        return triageStartTime;
    }

    public void setTriageStartTime(double triageStartTime) {
        this.triageStartTime = triageStartTime;
    }

    public double getTriageEndTime() {
        return triageEndTime;
    }

    public void setTriageEndTime(double triageEndTime) {
        this.triageEndTime = triageEndTime;
    }

    public double getTreatmentStartTime() {
        return treatmentStartTime;
    }

    public void setTreatmentStartTime(double treatmentStartTime) {
        this.treatmentStartTime = treatmentStartTime;
    }

    public double getTreatmentEndTime() {
        return treatmentEndTime;
    }

    public void setTreatmentEndTime(double treatmentEndTime) {
        this.treatmentEndTime = treatmentEndTime;
    }

    public double getDischargeTime() {
        return dischargeTime;
    }

    public void setDischargeTime(double dischargeTime) {
        this.dischargeTime = dischargeTime;
    }
    public Resource getAssignedNurse() {
    return assignedNurse;
    }

    public void setAssignedNurse(Resource assignedNurse) {
        this.assignedNurse = assignedNurse;
    }

    public Resource getAssignedDoctor() {
        return assignedDoctor;
    }

    public void setAssignedDoctor(Resource assignedDoctor) {
        this.assignedDoctor = assignedDoctor;
    }

    public Resource getAssignedBed() {
        return assignedBed;
    }

    public void setAssignedBed(Resource assignedBed) {
        this.assignedBed = assignedBed;
    }

    public double getTriageWaitTime() {
        return triageWaitTime;
    }

    public void setTriageWaitTime(double triageWaitTime) {
        this.triageWaitTime = triageWaitTime;
    }

    public double getTreatmentWaitTime() {
        return treatmentWaitTime;
    }

    public void setTreatmentWaitTime(double treatmentWaitTime) {
        this.treatmentWaitTime = treatmentWaitTime;
    }
}