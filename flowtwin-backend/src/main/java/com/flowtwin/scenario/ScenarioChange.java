package com.flowtwin.scenario;

import com.flowtwin.model.ResourceRole;
import com.flowtwin.model.Zone;

/**
 * One change to overlay on the twin for a what-if.
 * type: "STAFF" | "CAPACITY" | "POLICY". from/to are for time-windowed changes
 * (parsed but not yet applied in the skeleton - see ScenarioOrchestrator TODO).
 */
public record ScenarioChange(
        String type,
        ResourceRole role,
        Zone zone,
        int delta,
        String from,
        String to
) {}
