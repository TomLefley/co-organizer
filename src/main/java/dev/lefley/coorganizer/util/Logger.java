package dev.lefley.coorganizer.util;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.logging.Logging;

public class Logger {
    // Easy to change log level - set to INFO for production, DEBUG/TRACE for development
    private static final LogLevel CURRENT_LOG_LEVEL = LogLevel.INFO;
    
    private final Logging logging;
    private final String className;
    
    public Logger(MontoyaApi api, Class<?> clazz) {
        this.logging = api != null ? api.logging() : null;
        this.className = clazz.getSimpleName();
    }
    
    public void trace(String message) {
        log(LogLevel.TRACE, message);
    }
    
    public void debug(String message) {
        log(LogLevel.DEBUG, message);
    }
    
    public void info(String message) {
        log(LogLevel.INFO, message);
    }
    
    public void error(String message) {
        log(LogLevel.ERROR, message);
    }
    
    public void error(String message, Throwable throwable) {
        log(LogLevel.ERROR, message + " - " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        if (CURRENT_LOG_LEVEL.shouldLog(LogLevel.DEBUG)) {
            throwable.printStackTrace();
        }
    }
    
    private void log(LogLevel level, String message) {
        if (CURRENT_LOG_LEVEL.shouldLog(level)) {
            String formattedMessage = String.format("[%s] %s: %s", level.name(), className, message);
            
            // If logging is null (e.g., for static utility classes), fall back to System.out/err
            if (logging != null) {
                if (level == LogLevel.ERROR) {
                    logging.logToError(formattedMessage);
                } else {
                    logging.logToOutput(formattedMessage);
                }
            } else {
                if (level == LogLevel.ERROR) {
                    System.err.println(formattedMessage);
                } else {
                    System.out.println(formattedMessage);
                }
            }
        }
    }
}