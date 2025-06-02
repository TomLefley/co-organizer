package dev.lefley.coorganizer.util;

public enum LogLevel {
    TRACE(0),
    DEBUG(1),
    INFO(2),
    ERROR(3);
    
    private final int level;
    
    LogLevel(int level) {
        this.level = level;
    }
    
    public int getLevel() {
        return level;
    }
    
    public boolean shouldLog(LogLevel targetLevel) {
        return this.level <= targetLevel.level;
    }
}