package model;

import lombok.Data;

@Data
public class LoggerRecord {
    private String Name;
    private String configuredLevel;
    private String effectiveLevel;
}
