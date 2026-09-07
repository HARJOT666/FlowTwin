package com.flowtwin.service;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real Redis via Testcontainers - infra sanity check that read/write and string
 * (de)serialization actually work through the Lettuce client, independent of the
 * business-logic key-writing behavior already covered by TwinStateServiceRedisTest's mocks.
 */
@Testcontainers
@SpringBootTest(classes = RedisConnectivityTest.RedisOnlyConfig.class)
class RedisConnectivityTest {

    @Container
    @ServiceConnection(name = "redis")
    static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7"));

    @Configuration
    @ImportAutoConfiguration(DataRedisAutoConfiguration.class)
    static class RedisOnlyConfig { }

    @Autowired
    private StringRedisTemplate template;

    @Test
    void writesAndReadsBackAStringValue() {
        template.opsForValue().set("twin:triageQueue", "7");

        assertThat(template.opsForValue().get("twin:triageQueue")).isEqualTo("7");
    }
}
