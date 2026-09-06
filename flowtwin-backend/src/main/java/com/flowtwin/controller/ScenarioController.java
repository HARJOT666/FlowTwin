package com.flowtwin.controller;

import com.flowtwin.model.ScenarioEntity;
import com.flowtwin.repository.ScenarioRepository;
import com.flowtwin.scenario.ScenarioRequest;
import com.flowtwin.scenario.ScenarioResponse;
import com.flowtwin.service.ScenarioOrchestrator;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scenarios")
public class ScenarioController {

    private final ScenarioOrchestrator orchestrator;
    private final ScenarioRepository repository;

    public ScenarioController(ScenarioOrchestrator orchestrator, ScenarioRepository repository) {
        this.orchestrator = orchestrator;
        this.repository = repository;
    }

    @PostMapping
    public ScenarioResponse run(@Valid @RequestBody ScenarioRequest request) {
        return orchestrator.run(request);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScenarioEntity> get(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
