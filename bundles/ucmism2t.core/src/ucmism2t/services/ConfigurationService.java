package ucmism2t.services;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Configuration Service for Acceleo Templates
 * 
 * This service provides access to configuration properties loaded from
 * multiple property files. Later files override values from earlier files,
 * allowing for cascading configuration (e.g., defaults + environment-specific).
 * 
 * Usage in Acceleo templates (AQL syntax):
 *   [model.getProperty('key.name')/]
 * 
 * Thread-Safety: This class is stateful and should be initialized once
 * before template execution in a single-threaded context.
 * 
 * @author ucmism2t generator
 * @version 1.0.0
 * @since Java 21
 */
public class ConfigurationService {
    
    /**
     * Merged configuration properties from all loaded files.
     * Later files override earlier ones.
     */
    private final Map<String, String> configuration;
    
    /**
     * Default constructor initializing empty configuration.
     * Call loadConfigurationFiles() to populate.
     */
    public ConfigurationService() {
        this.configuration = new HashMap<>();
    }
    
    /**
     * Loads multiple Java properties files in order.
     * 
     * Properties are loaded sequentially, with values from later files
     * overriding those from earlier files. This enables a layered
     * configuration approach:
     *   1. defaults.properties (base configuration)
     *   2. environment.properties (environment-specific overrides)
     *   3. local.properties (local developer overrides)
     * 
     * File Format: Standard Java properties file
     *   key1=value1
     *   key2=value2
     *   # Comments start with # or !
     * 
     * @param configFilePaths Array of file paths to load (order matters)
     * @throws IOException If any file cannot be read
     * @throws IllegalArgumentException If configFilePaths is null or empty
     */
    public void loadConfigurationFiles(String[] configFilePaths) throws IOException {
        if (configFilePaths == null || configFilePaths.length == 0) {
            throw new IllegalArgumentException("At least one configuration file path must be provided");
        }
        
        // Clear any existing configuration
        configuration.clear();
        
        // Load each file in order
        for (String filePath : configFilePaths) {
            loadSingleFile(filePath);
        }
    }
    
    /**
     * Loads a single properties file and merges into configuration.
     * 
     * @param filePath Path to the properties file
     * @throws IOException If file cannot be read or parsed
     */
    private void loadSingleFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        
        // Check file exists
        if (!Files.exists(path)) {
            throw new IOException("Configuration file not found: " + filePath);
        }
        
        // Check file is readable
        if (!Files.isReadable(path)) {
            throw new IOException("Configuration file not readable: " + filePath);
        }
        
        // Load properties from file
        Properties props = new Properties();
        try (InputStream input = new FileInputStream(path.toFile())) {
            props.load(input);
            
            // Merge into configuration map (later values override earlier)
            for (String key : props.stringPropertyNames()) {
                configuration.put(key, props.getProperty(key));
            }
        }
    }
    
    /**
     * Retrieves a configuration value by key.
     * 
     * This method is callable from Acceleo templates using AQL syntax.
     * 
     * Example in .mtl template:
     *   [comment Get a configuration value /]
     *   [model.getProperty('output.encoding')/]
     * 
     * @param key The property key to look up
     * @return The property value, or empty string if key not found
     */
    public String getProperty(String key) {
        return configuration.getOrDefault(key, "");
    }
    
    /**
     * Retrieves a configuration value with a default fallback.
     * 
     * Example in .mtl template:
     *   [model.getProperty('max.retries', '3')/]
     * 
     * @param key The property key to look up
     * @param defaultValue Value to return if key not found
     * @return The property value, or defaultValue if key not found
     */
    public String getProperty(String key, String defaultValue) {
        return configuration.getOrDefault(key, defaultValue);
    }
    
    /**
     * Checks if a configuration key exists.
     * 
     * Example in .mtl template:
     *   [if (model.hasProperty('debug.enabled'))]
     *     Debug mode active
     *   [/if]
     * 
     * @param key The property key to check
     * @return true if key exists, false otherwise
     */
    public boolean hasProperty(String key) {
        return configuration.containsKey(key);
    }
    
    /**
     * Returns all configuration keys.
     * 
     * Useful for debugging or generating configuration reports.
     * 
     * @return Array of all property keys
     */
    public String[] getAllKeys() {
        return configuration.keySet().toArray(new String[0]);
    }
    
    /**
     * Returns the number of loaded configuration properties.
     * 
     * @return Count of properties
     */
    public int getPropertyCount() {
        return configuration.size();
    }
    
    /**
     * Clears all loaded configuration.
     * 
     * Useful for testing or reloading configuration.
     */
    public void clear() {
        configuration.clear();
    }
}
