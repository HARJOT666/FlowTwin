package com.flowtwin.simulation;

public class SimulationTest {

    public static void main(String[] args) {

        Scenario baseline =
                new Scenario(
                        "Baseline",
                        3,
                        2,
                        5
                );

        Scenario addDoctor =
                new Scenario(
                        "Add One Doctor",
                        3,
                        3,
                        5
                );

        ScenarioRunner runner = new ScenarioRunner();

        System.out.println("\n\n######## ADD ONE DOCTOR ########");

        ScenarioResult doctorResult =
                runner.runScenario(baseline, addDoctor);

        System.out.println(doctorResult);
    }
}