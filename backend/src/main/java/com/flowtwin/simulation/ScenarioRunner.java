package com.flowtwin.simulation;

public class ScenarioRunner {

    public ScenarioResult runScenario(
            Scenario baseline,
            Scenario scenario) {

        long seed = 12345L;
        // -----------------------------
        // Run baseline simulation
        // -----------------------------

        SimulationEngine baselineEngine =
                new SimulationEngine(
                        baseline.createConfig(seed)
                );

        baselineEngine.setupHospital();

        addTestPatients(baselineEngine);

        SimulationResult baselineResult =
                baselineEngine.run();


        // -----------------------------
        // Run scenario simulation
        // -----------------------------

        SimulationEngine scenarioEngine =
                new SimulationEngine(
                        scenario.createConfig(seed)
                );

        scenarioEngine.setupHospital();

        addTestPatients(scenarioEngine);

        SimulationResult scenarioResult =
                scenarioEngine.run();


        // -----------------------------
        // Compare results
        // -----------------------------

        return new ScenarioResult(
                scenario.getName(),
                baselineResult,
                scenarioResult
        );
    }


    // Add the same patient load to both simulations
    private void addTestPatients(
            SimulationEngine engine) {

        for (int i = 1; i <= 20; i++) {

            // 1 = Low
            // 2 = Medium
            // 3 = High
            int acuity = (i % 3) + 1;

            // Patient arrives every 2 minutes
            double arrivalTime = i * 2;

            Patient patient =
                    new Patient(
                            i,
                            acuity,
                            arrivalTime
                    );

            engine.schedulePatientArrival(
                    patient,
                    arrivalTime
            );
        }
    }
}