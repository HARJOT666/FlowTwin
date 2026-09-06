package com.flowtwin.controller;

import com.flowtwin.service.TwinStateService;
import com.flowtwin.twin.TwinState;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/twin")
public class TwinController {

    private final TwinStateService twin;

    public TwinController(TwinStateService twin) {
        this.twin = twin;
    }

    @GetMapping("/state")
    public TwinState state() {
        return twin.snapshot();
    }

    @GetMapping("/metrics")
    public TwinState metrics() {
        return twin.snapshot();
    }
}
