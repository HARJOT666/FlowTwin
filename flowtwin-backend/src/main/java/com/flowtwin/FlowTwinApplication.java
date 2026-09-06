package com.flowtwin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class FlowTwinApplication {
    public static void main(String[] args) {
        // Java-side DNS hardening for outbound HTTPS (e.g. the Gemini call). Prefer IPv4 so we
        // avoid intermittent IPv6 AAAA-lookup failures on hosts without working IPv6, and never
        // cache a failed DNS lookup so a transient UnknownHostException does not stick for the
        // whole JVM lifetime. Set before any networking is initialised.
        System.setProperty("java.net.preferIPv4Stack", "true");
        java.security.Security.setProperty("networkaddress.cache.negative.ttl", "0");

        SpringApplication.run(FlowTwinApplication.class, args);
    }
}
