package table;

public class Loggers {
    public String Name;
    public String configuredLevel;
    public String effectiveLevel;

    public Loggers(String name, String conf, String eff) {
        Name = name;
        configuredLevel = conf;
        effectiveLevel = eff;
    }

    public Object[] toObject() {
        return new Object[]{Name, configuredLevel, effectiveLevel};
    }
}
