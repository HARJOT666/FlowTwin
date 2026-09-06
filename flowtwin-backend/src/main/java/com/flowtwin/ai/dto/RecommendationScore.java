package com.flowtwin.ai.dto;

import java.util.List;
import java.util.Map;

/** Signed effectiveness: -100 (worse) to +100 (better); zero means neutral. */
public record RecommendationScore(double score, Impact impact, String primaryReason,
                                  Map<String, Double> weightedContributions,
                                  List<String> limitations) {
    public enum Impact { NEGATIVE, NONE, LOW, MEDIUM, HIGH }
}
