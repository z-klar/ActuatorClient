package table;

public class Metrics {
    public String Name;

    public Metrics(String name) {
        Name = name;
    }

    public Object[] toObject() {
        return new Object[]{Name};
    }
}
