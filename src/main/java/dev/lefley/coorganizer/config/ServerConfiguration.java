package dev.lefley.coorganizer.config;

/**
 * Centralized server configuration for Co-Organizer extension.
 * 
 * To host the sharing server at a different address, fork the repository
 * and modify these constants in one place.
 */
public final class ServerConfiguration {
    /**
     * The hostname of the sharing server.
     * Default: "localhost" for local development and testing.
     * 
     * Change this to point to your custom server:
     * - "example.com" for a remote server
     * - "192.168.1.100" for a local network server
     * - "myserver.local" for a local hostname
     */
    public static final String HOST = "localhost";
    
    /**
     * The port number of the sharing server.
     * Default: 3000
     * 
     * Change this to match your server's port:
     * - 8080 for common web servers
     * - 443 for HTTPS servers
     * - Any custom port your server uses
     */
    public static final int PORT = 3000;
    
    /**
     * The full base URL of the sharing server.
     * Automatically constructed from HOST and PORT.
     */
    public static final String BASE_URL = "http://" + HOST + ":" + PORT;
    
    /**
     * The endpoint path for uploading shared items.
     * Default: "/share"
     */
    public static final String SHARE_ENDPOINT = "/share";
    
    /**
     * The endpoint suffix for downloading/importing shared items.
     * Default: "/import"
     */
    public static final String IMPORT_ENDPOINT_SUFFIX = "/import";
    
    // Private constructor to prevent instantiation
    private ServerConfiguration() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}