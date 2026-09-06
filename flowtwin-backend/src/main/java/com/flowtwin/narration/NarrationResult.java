package com.flowtwin.narration;

import java.util.List;

public record NarrationResult(String summary, List<String> recommendations, String source) {}
