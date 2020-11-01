package tools;

import lombok.Data;
import model.ActuatorLink;
import model.LoggerRecord;

import java.util.ArrayList;

@Data
public class GlobalData {

    private ArrayList<ActuatorLink> actuatorLinks = new ArrayList<>();
    private ArrayList<LoggerRecord> loggerRecords = new ArrayList<>();


}
