package model;

import lombok.Data;

@Data
public class ActuatorLink {
    private String Name;
    private String href;
    private boolean templated;
}
