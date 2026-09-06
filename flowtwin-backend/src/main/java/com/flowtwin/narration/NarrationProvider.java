package com.flowtwin.narration;

/** A pluggable LLM backend. Implementations throw on failure so the service can fall back. */
public interface NarrationProvider {
    String generate(String system, String user);
}
