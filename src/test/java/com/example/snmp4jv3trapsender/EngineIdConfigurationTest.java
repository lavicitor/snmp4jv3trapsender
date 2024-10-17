package com.example.snmp4jv3trapsender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class EngineIdConfigurationTest {

    @Autowired
    private EngineIdConfiguration engineIdConfigurationTest;

    @Test
    public void testPropertySetAndGet() throws IOException {
        engineIdConfigurationTest.setProperty("testKey", "testValue");
        assertEquals("testValue", engineIdConfigurationTest.getProperty("testKey"));
    }

    @Test
    public void testPropertyNotFound() {
        assertNull(engineIdConfigurationTest.getProperty("nonExistentKey"));
    }

}
