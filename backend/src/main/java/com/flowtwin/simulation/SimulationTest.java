package com.flowtwin.simulation;

public class SimulationTest {

    public static void main(String[] args) {

        SimulationEngine engine = new SimulationEngine();

        // Set up hospital resources
        engine.setupHospital();

        // Create 20 patients arriving every 2 minutes
        for (int i = 1; i <= 20; i++) {

            int acuity = (i % 3) + 1;

            double arrivalTime = i * 2;

            Patient patient =
                    new Patient(i, acuity, arrivalTime);

            engine.schedulePatientArrival(
                    patient,
                    arrivalTime
            );
        }

        // Run simulation and get results
        SimulationResult result = engine.run();

        // Print final results
        System.out.println(result);
    }
}