package com.flowtwin.simulation;

import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

public class SimulationEngine {

    private final PriorityQueue<Event> eventQueue;
    private final SimulationState state;
    private final Random random;

    // Peak queue sizes during the simulation
    private int peakTriageQueue;
    private int peakTreatmentQueue;

    public SimulationEngine() {
        eventQueue = new PriorityQueue<>();
        state = new SimulationState();
        random = new Random();

        peakTriageQueue = 0;
        peakTreatmentQueue = 0;
    }

    // ---------------------------------------------------------
    // HOSPITAL SETUP
    // ---------------------------------------------------------

    public void setupHospital() {

        // 3 Nurses
        state.addNurse(new Resource("N1", "NURSE"));
        state.addNurse(new Resource("N2", "NURSE"));
        state.addNurse(new Resource("N3", "NURSE"));

        // 2 Doctors
        state.addDoctor(new Resource("D1", "DOCTOR"));
        state.addDoctor(new Resource("D2", "DOCTOR"));

        // 5 Beds
        state.addBed(new Resource("B1", "BED"));
        state.addBed(new Resource("B2", "BED"));
        state.addBed(new Resource("B3", "BED"));
        state.addBed(new Resource("B4", "BED"));
        state.addBed(new Resource("B5", "BED"));
    }

    // ---------------------------------------------------------
    // EVENT SCHEDULING
    // ---------------------------------------------------------

    public void schedulePatientArrival(Patient patient, double arrivalTime) {

        Event event = new Event(
                arrivalTime,
                EventType.PATIENT_ARRIVED,
                patient
        );

        eventQueue.add(event);
    }

    // ---------------------------------------------------------
    // MAIN SIMULATION LOOP
    // ---------------------------------------------------------

    public SimulationResult run() {

        while (!eventQueue.isEmpty()) {

            Event currentEvent = eventQueue.poll();

            // Move simulation clock
            state.setCurrentTime(currentEvent.getTime());

            // Process event
            processEvent(currentEvent);
        }

        return calculateResults();
    }

    // ---------------------------------------------------------
    // EVENT PROCESSING
    // ---------------------------------------------------------

    private void processEvent(Event event) {

        switch (event.getType()) {

            case PATIENT_ARRIVED:
                handlePatientArrival(event.getPatient());
                break;

            case TRIAGE_COMPLETED:
                handleTriageCompleted(event.getPatient());
                break;

            case TREATMENT_COMPLETED:
                handleTreatmentCompleted(event.getPatient());
                break;

            case PATIENT_DISCHARGED:
                handlePatientDischarged(event.getPatient());
                break;
        }
    }

    // ---------------------------------------------------------
    // PATIENT ARRIVAL
    // ---------------------------------------------------------

    private void handlePatientArrival(Patient patient) {

        state.addPatient(patient);

        System.out.println(
                "Time " + state.getCurrentTime()
                        + ": Patient " + patient.getId()
                        + " arrived."
        );

        Resource nurse = findAvailableResource(
                state.getNurses()
        );

        if (nurse != null) {

            // Assign nurse
            nurse.setAvailable(false);
            patient.setAssignedNurse(nurse);

            // Start triage
            patient.setTriageStartTime(
                    state.getCurrentTime()
            );

            // Calculate triage waiting time
            patient.setTriageWaitTime(
                    state.getCurrentTime()
                            - patient.getArrivalTime()
            );

            double triageDuration =
                    generateTriageTime(patient);

            // Schedule triage completion
            eventQueue.add(
                    new Event(
                            state.getCurrentTime()
                                    + triageDuration,
                            EventType.TRIAGE_COMPLETED,
                            patient
                    )
            );

            System.out.println(
                    "Patient " + patient.getId()
                            + " assigned to nurse "
                            + nurse.getId()
            );

        } else {

            // No nurse available
            state.getQueueManager()
                    .addToTriageQueue(patient);

            // Update peak queue size
            peakTriageQueue = Math.max(
                    peakTriageQueue,
                    state.getQueueManager()
                            .getTriageQueueSize()
            );

            System.out.println(
                    "Patient " + patient.getId()
                            + " added to triage queue."
            );
        }
    }

    // ---------------------------------------------------------
    // TRIAGE COMPLETED
    // ---------------------------------------------------------

    private void handleTriageCompleted(
            Patient patient) {

        patient.setTriageEndTime(
                state.getCurrentTime()
        );

        System.out.println(
                "Time " + state.getCurrentTime()
                        + ": Patient " + patient.getId()
                        + " completed triage."
        );

        // Release the exact nurse assigned to this patient
        Resource nurse = patient.getAssignedNurse();

        if (nurse != null) {

            nurse.setAvailable(true);
            patient.setAssignedNurse(null);

            System.out.println(
                    "Nurse " + nurse.getId()
                            + " is now available."
            );
        }

        // Start next patient waiting for triage
        startNextTriagePatient();

        // Patient now enters treatment queue
        state.getQueueManager()
                .addToTreatmentQueue(patient);

        // Update peak treatment queue
        peakTreatmentQueue = Math.max(
                peakTreatmentQueue,
                state.getQueueManager()
                        .getTreatmentQueueSize()
        );

        // Try to start treatment
        startNextTreatmentPatient();
    }

    // ---------------------------------------------------------
    // START NEXT TRIAGE PATIENT
    // ---------------------------------------------------------

    private void startNextTriagePatient() {

        if (state.getQueueManager()
                .isTriageQueueEmpty()) {

            return;
        }

        Resource nurse = findAvailableResource(
                state.getNurses()
        );

        if (nurse == null) {
            return;
        }

        Patient patient =
                state.getQueueManager()
                        .getNextTriagePatient();

        // Assign nurse
        nurse.setAvailable(false);
        patient.setAssignedNurse(nurse);

        // Start triage
        patient.setTriageStartTime(
                state.getCurrentTime()
        );

        // Calculate waiting time
        patient.setTriageWaitTime(
                state.getCurrentTime()
                        - patient.getArrivalTime()
        );

        double triageDuration =
                generateTriageTime(patient);

        // Schedule triage completion
        eventQueue.add(
                new Event(
                        state.getCurrentTime()
                                + triageDuration,
                        EventType.TRIAGE_COMPLETED,
                        patient
                )
        );

        System.out.println(
                "Patient " + patient.getId()
                        + " taken from triage queue by nurse "
                        + nurse.getId()
        );
    }

    // ---------------------------------------------------------
    // START NEXT TREATMENT PATIENT
    // ---------------------------------------------------------

    private void startNextTreatmentPatient() {

        while (!state.getQueueManager()
                .isTreatmentQueueEmpty()) {

            Resource bed = findAvailableResource(
                    state.getBeds()
            );

            Resource doctor = findAvailableResource(
                    state.getDoctors()
            );

            // Need BOTH doctor and bed
            if (bed == null || doctor == null) {
                return;
            }

            Patient patient =
                    state.getQueueManager()
                            .getNextTreatmentPatient();

            // Assign resources
            bed.setAvailable(false);
            doctor.setAvailable(false);

            patient.setAssignedBed(bed);
            patient.setAssignedDoctor(doctor);

            // Start treatment
            patient.setTreatmentStartTime(
                    state.getCurrentTime()
            );

            // Calculate treatment waiting time
            patient.setTreatmentWaitTime(
                    state.getCurrentTime()
                            - patient.getTriageEndTime()
            );

            double treatmentDuration =
                    generateTreatmentTime(patient);

            // Schedule treatment completion
            eventQueue.add(
                    new Event(
                            state.getCurrentTime()
                                    + treatmentDuration,
                            EventType.TREATMENT_COMPLETED,
                            patient
                    )
            );

            System.out.println(
                    "Patient " + patient.getId()
                            + " started treatment with doctor "
                            + doctor.getId()
                            + " and bed "
                            + bed.getId()
            );
        }
    }

    // ---------------------------------------------------------
    // TREATMENT COMPLETED
    // ---------------------------------------------------------

    private void handleTreatmentCompleted(
            Patient patient) {

        patient.setTreatmentEndTime(
                state.getCurrentTime()
        );

        System.out.println(
                "Time " + state.getCurrentTime()
                        + ": Patient " + patient.getId()
                        + " completed treatment."
        );

        // Release exact bed
        Resource bed = patient.getAssignedBed();

        if (bed != null) {

            bed.setAvailable(true);
            patient.setAssignedBed(null);

            System.out.println(
                    "Bed " + bed.getId()
                            + " is now available."
            );
        }

        // Release exact doctor
        Resource doctor =
                patient.getAssignedDoctor();

        if (doctor != null) {

            doctor.setAvailable(true);
            patient.setAssignedDoctor(null);

            System.out.println(
                    "Doctor " + doctor.getId()
                            + " is now available."
            );
        }

        // Schedule discharge
        eventQueue.add(
                new Event(
                        state.getCurrentTime(),
                        EventType.PATIENT_DISCHARGED,
                        patient
                )
        );

        // Resources are now available
        // Try the next treatment patient
        startNextTreatmentPatient();
    }

    // ---------------------------------------------------------
    // PATIENT DISCHARGED
    // ---------------------------------------------------------

    private void handlePatientDischarged(
            Patient patient) {

        patient.setDischargeTime(
                state.getCurrentTime()
        );

        System.out.println(
                "Time " + state.getCurrentTime()
                        + ": Patient " + patient.getId()
                        + " discharged."
        );

        double totalTime =
                patient.getDischargeTime()
                        - patient.getArrivalTime();

        System.out.println(
                "Patient " + patient.getId()
                        + " total time in hospital: "
                        + totalTime
                        + " minutes."
        );
    }

    // ---------------------------------------------------------
    // CALCULATE RESULTS
    // ---------------------------------------------------------

    private SimulationResult calculateResults() {

        int totalPatients =
                state.getPatients().size();

        if (totalPatients == 0) {

            return new SimulationResult(
                    0,
                    0,
                    0,
                    0,
                    0,
                    0
            );
        }

        double totalTriageWait = 0;
        double totalTreatmentWait = 0;
        double totalLengthOfStay = 0;

        int dischargedPatients = 0;

        for (Patient patient :
                state.getPatients()) {

            totalTriageWait +=
                    patient.getTriageWaitTime();

            totalTreatmentWait +=
                    patient.getTreatmentWaitTime();

            // Only count patients who actually discharged
            if (patient.getDischargeTime() > 0) {

                totalLengthOfStay +=
                        patient.getDischargeTime()
                                - patient.getArrivalTime();

                dischargedPatients++;
            }
        }

        double averageTriageWait =
                totalTriageWait / totalPatients;

        double averageTreatmentWait =
                totalTreatmentWait / totalPatients;

        double averageLengthOfStay =
                dischargedPatients == 0
                        ? 0
                        : totalLengthOfStay / dischargedPatients;

        return new SimulationResult(
                totalPatients,
                round(averageTriageWait),
                round(averageTreatmentWait),
                round(averageLengthOfStay),
                peakTriageQueue,
                peakTreatmentQueue
        );
    }

    // ---------------------------------------------------------
    // RESOURCE SEARCH
    // ---------------------------------------------------------

    private Resource findAvailableResource(
            List<Resource> resources) {

        for (Resource resource : resources) {

            if (resource.isAvailable()) {
                return resource;
            }
        }

        return null;
    }

    // ---------------------------------------------------------
    // TRIAGE TIME
    // ---------------------------------------------------------

    private double generateTriageTime(
            Patient patient) {

        /*
         * Acuity:
         * 3 = High
         * 2 = Medium
         * 1 = Low
         */

        switch (patient.getAcuity()) {

            case 3:
                return 4 + random.nextInt(4);

            case 2:
                return 5 + random.nextInt(5);

            default:
                return 6 + random.nextInt(5);
        }
    }

    // ---------------------------------------------------------
    // TREATMENT TIME
    // ---------------------------------------------------------

    private double generateTreatmentTime(
            Patient patient) {

        switch (patient.getAcuity()) {

            case 3:
                return 30 + random.nextInt(21);

            case 2:
                return 20 + random.nextInt(21);

            default:
                return 15 + random.nextInt(16);
        }
    }

    // ---------------------------------------------------------
    // ROUNDING
    // ---------------------------------------------------------

    private double round(double value) {

        return Math.round(value * 10.0) / 10.0;
    }
}