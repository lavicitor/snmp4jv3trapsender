package com.example.snmp4jv3trapsender;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for managing SNMP engine ID and boot count.
 * Reads and writes configuration from engine.properties file.
 * In a real world usecase these can be stored in a database or anywhere else, a
 * file is used for simplicity in this example.
 */
@Configuration
public class EngineIdConfiguration {

    private static final String CONFIG_FILE = "engine.properties";

    /**
     * Creates a Properties object, loading configuration from engine.properties if
     * it exists
     * 
     * @return A Properties object containing the configuration
     */
    @Bean
    public Properties customProperties() {
        Properties props = new Properties();
        try {
            File configFile = new File(CONFIG_FILE);
            if (configFile.exists()) {
                try (FileInputStream fis = new FileInputStream(configFile)) {
                    props.load(fis);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return props;
    }

    /**
     * Sets a property in the engine.properties file.
     * 
     * @param key   The property key
     * @param value The property value
     */
    public void setProperty(String key, String value) {
        Properties props = customProperties();
        props.setProperty(key, value);
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            props.store(fos, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves a property from the engine.properties file.
     * 
     * @param key The property key
     * @return The property value, or null if not found
     */
    public String getProperty(String key) {
        Properties props = customProperties();
        return props.getProperty(key);
    }
}
