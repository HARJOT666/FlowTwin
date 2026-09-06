package com.flowtwin.simulation;

import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

public class SimulationEngine {

    private final PriorityQueue<Event> eventQueue;
    private final SimulationState state;
    private final Random random;
    private final SimulationConfig config;

    // Peak queue sizes
    private int peakTriageQueue;
    private int peakTreatmentQueue;

    // Resource busy-time tracking
    private double totalNurseBusyTime;
    private double totalDoctorBusyTime;
    private double totalBedBusyTime;

    // Time of previous processed event
    private double lastEventTime;


    // ---------------------------------------------------------
    // CONSTRUCTOR
    // ---------------------------------------------------------

    public SimulationEngine(SimulationConfig config) {

        this.config = config;

        eventQueue = new PriorityQueue<>();
        state = new SimulationState();

        // Same seed gives reproducible simulation results
        random = new Random(config.getSeed());

        peakTriageQueue = 0;
        peakTreatmentQueue = 0;

        totalNurseBusyTime = 0;
        totalDoctorBusyTime = 0;
        totalBedBusyTime = 0;

        lastEventTime = 0;
    }


    // ---------------------------------------------------------
    // HOSPITAL SETUP
    // ---------------------------------------------------------

    public void setupHospital() {

        // Create nurses
        for (int i = 1;
             i <= config.getNumberOfNurses();
             i++) {

            state.addNurse(
                    new Resource(
                            "N" + i,
                            "NURSE"
                    )
            );
        }


        // Create doctors
        for (int i = 1;
             i <= config.getNumberOfDoctors();
             i++) {

            state.addDoctor(
                    new Resource(
                            "D" + i,
                            "DOCTOR"
                    )
            );
        }


        // Create beds
        for (int i = 1;
             i <= config.getNumberOfBeds();
             i++) {

            state.addBed(
                    new Resource(
                            "B" + i,
                            "BED"
                    )
            );
        }
    }


    // ---------------------------------------------------------
    // EVENT SCHEDULING
    // ---------------------------------------------------------

    public void schedulePatientArrival(
            Patient patient,
            double arrivalTime) {

        Event event =
                new Event(
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

            Event currentEvent =
                    eventQueue.poll();

            double currentTime =
                    currentEvent.getTime();

            // Track how long resources remained busy
            updateResourceBusyTime(currentTime);

            // Move simulation clock
            state.setCurrentTime(currentTime);

            // Process event
            processEvent(currentEvent);

            // Remember current event time
            lastEventTime = currentTime;
        }

        return calculateResults();
    }


    // ---------------------------------------------------------
    // RESOURCE BUSY-TIME TRACKING
    // ---------------------------------------------------------

    private void updateResourceBusyTime(
            double currentTime) {

        double elapsedTime =
                currentTime - lastEventTime;

        if (elapsedTime <= 0) {
            return;
        }


        // Nurses
        for (Resource nurse :
                state.getNurses()) {

            if (!nurse.isAvailable()) {
                totalNurseBusyTime += elapsedTime;
            }
        }


        // Doctors
        for (Resource doctor :
                state.getDoctors()) {

            if (!doctor.isAvailable()) {
                totalDoctorBusyTime += elapsedTime;
            }
        }


        // Beds
        for (Resource bed :
                state.getBeds()) {

            if (!bed.isAvailable()) {
                totalBedBusyTime += elapsedTime;
            }
        }
    }


    // ---------------------------------------------------------
    // EVENT PROCESSING
    // ---------------------------------------------------------

    private void processEvent(Event event) {

        switch (event.getType()) {

            case PATIENT_ARRIVED:
                handlePatientArrival(
                        event.getPatient()
                );
                break;

            case TRIAGE_COMPLETED:
                handleTriageCompleted(
                        event.getPatient()
                );
                break;

            case TREATMENT_COMPLETED:
                handleTreatmentCompleted(
                        event.getPatient()
                );
                break;

            case PATIENT_DISCHARGED:
                handlePatientDischarged(
                        event.getPatient()
                );
                break;
        }
    }


    // ---------------------------------------------------------
    // PATIENT ARRIVAL
    // ---------------------------------------------------------

    private void handlePatientArrival(
            Patient patient) {

        state.addPatient(patient);

        System.out.println(
                "Time "
                        + state.getCurrentTime()
                        + ": Patient "
                        + patient.getId()
                        + " arrived."
        );


        Resource nurse =
                findAvailableResource(
                        state.getNurses()
                );


        if (nurse != null) {

            nurse.setAvailable(false);

            patient.setAssignedNurse(nurse);

            patient.setTriageStartTime(
                    state.getCurrentTime()
            );

            patient.setTriageWaitTime(
                    state.getCurrentTime()
                            - patient.getArrivalTime()
            );


            double triageDuration =
                    generateTriageTime(patient);


            eventQueue.add(
                    new Event(
                            state.getCurrentTime()
                                    + triageDuration,
                            EventType.TRIAGE_COMPLETED,
                            patient
                    )
            );


            System.out.println(
                    "Patient "
                            + patient.getId()
                            + " assigned to nurse "
                            + nurse.getId()
            );

        } else {

            state.getQueueManager()
                    .addToTriageQueue(patient);


            peakTriageQueue =
                    Math.max(
                            peakTriageQueue,
                            state.getQueueManager()
                                    .getTriageQueueSize()
                    );


            System.out.println(
                    "Patient "
                            + patient.getId()
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
                "Time "
                        + state.getCurrentTime()
                        + ": Patient "
                        + patient.getId()
                        + " completed triage."
        );


        // Release exact nurse
        Resource nurse =
                patient.getAssignedNurse();


        if (nurse != null) {

            nurse.setAvailable(true);

            patient.setAssignedNurse(null);


            System.out.println(
                    "Nurse "
                            + nurse.getId()
                            + " is now available."
            );
        }


        // Start next triage patient
        startNextTriagePatient();


        // Move patient to treatment queue
        state.getQueueManager()
                .addToTreatmentQueue(patient);


        peakTreatmentQueue =
                Math.max(
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


        Resource nurse =
                findAvailableResource(
                        state.getNurses()
                );


        if (nurse == null) {
            return;
        }


        Patient patient =
                state.getQueueManager()
                        .getNextTriagePatient();


        nurse.setAvailable(false);

        patient.setAssignedNurse(nurse);


        patient.setTriageStartTime(
                state.getCurrentTime()
        );


        patient.setTriageWaitTime(
                state.getCurrentTime()
                        - patient.getArrivalTime()
        );


        double triageDuration =
                generateTriageTime(patient);


        eventQueue.add(
                new Event(
                        state.getCurrentTime()
                                + triageDuration,
                        EventType.TRIAGE_COMPLETED,
                        patient
                )
        );


        System.out.println(
                "Patient "
                        + patient.getId()
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


            Resource bed =
                    findAvailableResource(
                            state.getBeds()
                    );


            Resource doctor =
                    findAvailableResource(
                            state.getDoctors()
                    );


            // Patient requires both a doctor and a bed
            if (bed == null || doctor == null) {
                return;
            }


            Patient patient =
                    state.getQueueManager()
                            .getNextTreatmentPatient();


            // Assign bed
            bed.setAvailable(false);

            patient.setAssignedBed(bed);


            // Assign doctor
            doctor.setAvailable(false);

            patient.setAssignedDoctor(doctor);


            // Start treatment
            patient.setTreatmentStartTime(
                    state.getCurrentTime()
            );


            patient.setTreatmentWaitTime(
                    state.getCurrentTime()
                            - patient.getTriageEndTime()
            );


            double treatmentDuration =
                    generateTreatmentTime(patient);


            eventQueue.add(
                    new Event(
                            state.getCurrentTime()
                                    + treatmentDuration,
                            EventType.TREATMENT_COMPLETED,
                            patient
                    )
            );


            System.out.println(
                    "Patient "
                            + patient.getId()
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
                "Time "
                        + state.getCurrentTime()
                        + ": Patient "
                        + patient.getId()
                        + " completed treatment."
        );


        // Release exact bed
        Resource bed =
                patient.getAssignedBed();


        if (bed != null) {

            bed.setAvailable(true);

            patient.setAssignedBed(null);


            System.out.println(
                    "Bed "
                            + bed.getId()
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
                    "Doctor "
                            + doctor.getId()
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


        // Start next waiting treatment patient
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
                "Time "
                        + state.getCurrentTime()
                        + ": Patient "
                        + patient.getId()
                        + " discharged."
        );


        double totalTime =
                patient.getDischargeTime()
                        - patient.getArrivalTime();


        System.out.println(
                "Patient "
                        + patient.getId()
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
                    0,
                    0,
                    0,
                    0,
                    "NONE"
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
                        : totalLengthOfStay
                                / dischargedPatients;


        double simulationTime =
                lastEventTime;


        double nurseUtilization =
                calculateUtilization(
                        totalNurseBusyTime,
                        state.getNurses().size(),
                        simulationTime
                );


        double doctorUtilization =
                calculateUtilization(
                        totalDoctorBusyTime,
                        state.getDoctors().size(),
                        simulationTime
                );


        double bedUtilization =
                calculateUtilization(
                        totalBedBusyTime,
                        state.getBeds().size(),
                        simulationTime
                );


        String bottleneck =
                detectBottleneck(
                        nurseUtilization,
                        doctorUtilization,
                        bedUtilization,
                        peakTriageQueue,
                        peakTreatmentQueue
                );


        return new SimulationResult(
                totalPatients,
                round(averageTriageWait),
                round(averageTreatmentWait),
                round(averageLengthOfStay),
                peakTriageQueue,
                peakTreatmentQueue,
                round(nurseUtilization),
                round(doctorUtilization),
                round(bedUtilization),
                bottleneck
        );
    }


    // ---------------------------------------------------------
    // RESOURCE UTILIZATION
    // ---------------------------------------------------------

    private double calculateUtilization(
            double busyTime,
            int resourceCount,
            double simulationTime) {

        if (resourceCount == 0 ||
                simulationTime == 0) {

            return 0;
        }


        double totalAvailableTime =
                resourceCount * simulationTime;


        return (busyTime /
                totalAvailableTime) * 100.0;
    }


    // ---------------------------------------------------------
    // BOTTLENECK DETECTION
    // ---------------------------------------------------------

    private String detectBottleneck(
            double nurseUtilization,
            double doctorUtilization,
            double bedUtilization,
            int peakTriageQueue,
            int peakTreatmentQueue) {


        // Queue + utilization based scores
        double nurseScore =
                nurseUtilization
                        + (peakTriageQueue * 5);


        double doctorScore =
                doctorUtilization
                        + (peakTreatmentQueue * 5);


        double bedScore =
                bedUtilization;


        double highestScore =
                Math.max(
                        nurseScore,
                        Math.max(
                                doctorScore,
                                bedScore
                        )
                );


        if (highestScore == nurseScore) {
            return "NURSE / TRIAGE";
        }


        if (highestScore == doctorScore) {
            return "DOCTOR / TREATMENT";
        }


        return "BED";
    }


    // ---------------------------------------------------------
    // RESOURCE SEARCH
    // ---------------------------------------------------------

    private Resource findAvailableResource(
            List<Resource> resources) {

        for (Resource resource :
                resources) {

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