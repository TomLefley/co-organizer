package dev.lefley.coorganizer.service;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.persistence.Preferences;
import dev.lefley.coorganizer.util.Logger;

import java.util.UUID;

public class DebugIdManager {
    private static final String DEBUG_ID_PREFERENCE_KEY = "co-organizer.debug-id";
    
    private final MontoyaApi api;
    private final Logger logger;
    private String debugId;
    
    public DebugIdManager(MontoyaApi api) {
        this.api = api;
        this.logger = new Logger(api, DebugIdManager.class);
        
        initializeDebugId();
    }
    
    /**
     * Gets the debug ID for this installation.
     * @return The debug ID (may be empty string if user has cleared it for privacy)
     */
    public String getDebugId() {
        return debugId;
    }
    
    /**
     * Checks if debug ID is enabled (not null and not empty).
     * @return true if debug ID should be sent in requests
     */
    public boolean isDebugIdEnabled() {
        return debugId != null && !debugId.isEmpty();
    }
    
    private void initializeDebugId() {
        Preferences preferences = api.persistence().preferences();
        
        // Check if debug ID preference exists (even if empty)
        if (preferences.getString(DEBUG_ID_PREFERENCE_KEY) != null) {
            // Preference exists - use whatever value is stored (including empty string)
            debugId = preferences.getString(DEBUG_ID_PREFERENCE_KEY);
            
            if (debugId.isEmpty()) {
                logger.info("Debug ID found but empty - respecting user privacy preference");
            } else {
                logger.debug("Debug ID loaded from preferences: " + debugId.substring(0, 8) + "...");
            }
        } else {
            // Preference doesn't exist - generate new UUID and store it
            debugId = UUID.randomUUID().toString();
            preferences.setString(DEBUG_ID_PREFERENCE_KEY, debugId);
            
            logger.info("Generated new debug ID for this installation: " + debugId.substring(0, 8) + "...");
        }
    }
    
    /**
     * Clears the debug ID (sets it to empty string) for privacy.
     * This will persist the empty string so it won't be regenerated.
     */
    public void clearDebugId() {
        debugId = "";
        Preferences preferences = api.persistence().preferences();
        preferences.setString(DEBUG_ID_PREFERENCE_KEY, debugId);
        
        logger.info("Debug ID cleared for privacy - will not be sent in future requests");
    }
    
    /**
     * Regenerates a new debug ID and stores it.
     * @return The new debug ID
     */
    public String regenerateDebugId() {
        debugId = UUID.randomUUID().toString();
        Preferences preferences = api.persistence().preferences();
        preferences.setString(DEBUG_ID_PREFERENCE_KEY, debugId);
        
        logger.info("Regenerated debug ID: " + debugId.substring(0, 8) + "...");
        return debugId;
    }
}